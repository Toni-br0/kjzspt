package com.pearadmin.modules.report.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.pearadmin.common.constant.CommonConstant;
import com.pearadmin.common.context.UserContext;
import com.pearadmin.common.tools.check.CheckUtil;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.im.domain.ImPushObjectManage;
import com.pearadmin.modules.im.mapper.ImPushObjectManageMapper;
import com.pearadmin.modules.report.domain.ReportAutoCreateInfo;
import com.pearadmin.modules.report.domain.ReportIndexWarning;
import com.pearadmin.modules.report.domain.ReportPustLeaderApproval;
import com.pearadmin.modules.report.mapper.ReportIndexWarningMapper;
import com.pearadmin.modules.report.service.RepIndexWarnApprovalService;
import com.pearadmin.modules.sys.domain.SysUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 创建日期：2025-10-24
 * 指标预警审批
 **/

@Slf4j
@Service
public class RepIndexWarnApprovalServiceImpl implements RepIndexWarnApprovalService {

    @Resource
    private ImPushObjectManageMapper imPushObjectManageMapper;

    @Resource
    private ReportIndexWarningMapper reportIndexWarningMapper;

    /**
     * 获取指标预警审批列表数据
     * @param reportIndexWarning
     * @return
     */
    @Override
    public List<ReportIndexWarning> getRepIndexWarnApprovalList(ReportIndexWarning reportIndexWarning) {
        //当前登录人信息
        SysUser currentUser = UserContext.currentUser();
        String loginUserId = currentUser.getUserId();
        boolean isAdmin = CheckUtil.hasSuperAdmin(currentUser);
        if(isAdmin){
            loginUserId = null;
        }

        List<ReportIndexWarning> list = reportIndexWarningMapper.getReportIndexWarningApprovList(reportIndexWarning,loginUserId);
        if(list != null && list.size() >0){
            for(ReportIndexWarning queryReportIndexWarning : list){
                //拼接推送对象名称
                String pushOtherObjectId = queryReportIndexWarning.getPushOtherObjectId();
                if(StringUtil.isNotEmpty(pushOtherObjectId)){
                    String[] pushObjectIdArr = pushOtherObjectId.split(";");
                    String pushOtherObjectName = "";
                    for(int i=0;i<pushObjectIdArr.length;i++){
                        String strPushObjectId = pushObjectIdArr[i];
                        //int iPushObjectId = Integer.parseInt(strPushObjectId);
                        ImPushObjectManage imPushObjectManage = imPushObjectManageMapper.selectById(strPushObjectId);
                        if(imPushObjectManage != null){
                            if(!pushOtherObjectName.equals("")){
                                pushOtherObjectName = pushOtherObjectName+";"+imPushObjectManage.getPushObjectName();
                            }else{
                                pushOtherObjectName = imPushObjectManage.getPushObjectName();
                            }
                        }
                    }

                    queryReportIndexWarning.setPushOtherObjectName(pushOtherObjectName);
                }

            }
        }
        return list;
    }


    /**
     * 根据ID获取指标预警审批状态
     * @param param
     * @return
     */
    @Override
    public JSONObject getApplyState(String param) {
        JSONObject retJson = new JSONObject();
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            String warningId = paramJson.getString("warningId");
            ReportIndexWarning reportIndexWarning = reportIndexWarningMapper.selectById(warningId);
            if(reportIndexWarning != null){
                if(StringUtil.isNotEmpty(reportIndexWarning.getApprState()) && !reportIndexWarning.getApprState().equals("0")){
                    retJson.put("retCode","-1");
                    retJson.put("retMsg","此记录已审批过,请勿重复审批");
                }else{
                    retJson.put("retCode","0");
                    retJson.put("retMsg","成功");
                }

            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","未查询到审批记录");
            }
        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","-1");
            retJson.put("retMsg","获取审批状态失败，请联系管理员");
        }
        return retJson;
    }


    /**
     * 根据ID获取指标预警审批状态
     * @param param
     * @return
     */
    @Override
    public JSONObject getBatchApplyState(String param) {
        JSONObject retJson = new JSONObject();
        try {

            JSONObject paramJson = JSONObject.parseObject(param);
            String warningIds = paramJson.getString("warningIds");

            List<String> warningIdList = Arrays.asList(warningIds.split(CommonConstant.COMMA));

            List<ReportIndexWarning> reportIndexWarningList = reportIndexWarningMapper.selApprStateByList(warningIdList);
            if(reportIndexWarningList != null && reportIndexWarningList.size() >0){
                retJson.put("retCode","-1");
                retJson.put("retMsg","所选的审批记录中已有审批过的记录");
            }else{
                retJson.put("retCode","0");
                retJson.put("retMsg","成功");
            }
        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","-1");
            retJson.put("retMsg","批量获取审批状态失败，系统异常，请联系管理员");
        }
        return retJson;
    }


    /**
     * 保存指标预警审批审批结果信息
     * @param param
     * @return
     */
    @Override
    public JSONObject saveApplyResult(String param) {
        JSONObject retJson = new JSONObject();
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            String warningId = paramJson.getString("warningId");
            String applyResult = paramJson.getString("applyResult");
            String applyOpinion = paramJson.getString("applyOpinion");

            ReportIndexWarning reportIndexWarning = reportIndexWarningMapper.selectById(warningId);
            if(reportIndexWarning != null){
                reportIndexWarning.setApprState(applyResult);
                reportIndexWarning.setApprTime(LocalDateTime.now());
                reportIndexWarning.setApplyOpinion(applyOpinion);
                //当前登录人信息
                SysUser currentUser = UserContext.currentUser();
                reportIndexWarning.setApprPersonId(currentUser.getUserId());

                int upResult = reportIndexWarningMapper.updateById(reportIndexWarning);
                if(upResult > 0){
                    retJson.put("retCode","0");
                    retJson.put("retMsg","审批成功");
                }else{
                    retJson.put("retCode","-1");
                    retJson.put("retMsg","审批失败");
                }
            }else {
                retJson.put("retCode","-1");
                retJson.put("retMsg","审批失败,未查询到审批记录信息");
            }

        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","-1");
                retJson.put("retMsg","审批失败，系统异常，请联系管理员");
        }
        return retJson;
    }


    /**
     * 批量保存指标预警审批审批结果信息
     * @param param
     * @return
     */
    @Override
    public JSONObject saveBatchApplyResult(String param) {
        JSONObject retJson = new JSONObject();
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            String warningIds = paramJson.getString("warningIds");
            String applyResult = paramJson.getString("applyResult");
            String applyOpinion = paramJson.getString("applyOpinion");

            //当前登录人信息
            SysUser currentUser = UserContext.currentUser();

            List<String> warningIdList = Arrays.asList(warningIds.split(CommonConstant.COMMA));

            int  updateResult = reportIndexWarningMapper.updateApplyResult(warningIdList,applyResult,applyOpinion,currentUser.getUserId());
            if(updateResult >0){
                retJson.put("retCode","0");
                retJson.put("retMsg","审批成功");

            }else{
            retJson.put("retCode","-1");
            retJson.put("retMsg","审批失败");
        }

        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","-1");
            retJson.put("retMsg","审批失败，系统异常，请联系管理员");
        }
        return retJson;
    }
}
