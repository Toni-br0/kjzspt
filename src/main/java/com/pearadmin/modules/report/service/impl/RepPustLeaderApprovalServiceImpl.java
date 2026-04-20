package com.pearadmin.modules.report.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.pearadmin.common.context.UserContext;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.im.domain.ImPushObjectManage;
import com.pearadmin.modules.im.mapper.ImPushObjectManageMapper;
import com.pearadmin.modules.report.domain.ReportAutoCreateInfo;
import com.pearadmin.modules.report.domain.ReportPustLeaderApproval;
import com.pearadmin.modules.report.mapper.ReportAutoCreateInfoMapper;
import com.pearadmin.modules.report.mapper.ReportPustLeaderApprovalMapper;
import com.pearadmin.modules.report.service.RepPustLeaderApprovalService;
import com.pearadmin.modules.sys.domain.SysUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 创建日期：2025-08-22
 * 自助取数报表推送至领导IM申请表
 **/

@Slf4j
@Service
public class RepPustLeaderApprovalServiceImpl implements RepPustLeaderApprovalService {

    @Value("${report-auto-apply-person}")
    private String reportAutoApplyPerson;

    @Resource
   private ReportPustLeaderApprovalMapper reportPustLeaderApprovalMapper;

    @Resource
    private ImPushObjectManageMapper imPushObjectManageMapper;
    @Autowired
    private ReportAutoCreateInfoMapper reportAutoCreateInfoMapper;


    /**
     * 获取自助取数报表推送至领导IM申请管理列表数据
     * @param reportPustLeaderApproval
     * @return
     */
    @Override
    public List<ReportPustLeaderApproval> getPustLeaderApprovalList(ReportPustLeaderApproval reportPustLeaderApproval) {
        List<ReportPustLeaderApproval> pustLeaderApprovalList = reportPustLeaderApprovalMapper.selPustLeaderApprovalList(reportPustLeaderApproval);
        if(pustLeaderApprovalList != null && pustLeaderApprovalList.size() >0){
            for(ReportPustLeaderApproval forReportPustLeaderApproval : pustLeaderApprovalList){
                String pushObjectType = forReportPustLeaderApproval.getPushObjectType() ==null?"":forReportPustLeaderApproval.getPushObjectType();
                //不为组织维度时
                if(!pushObjectType.equals("zzwd")){
                    //获取推送领导名称
                    String pushLeaderId = forReportPustLeaderApproval.getPushLeaderId();
                    if(StringUtil.isNotEmpty(pushLeaderId)){
                        String[] pushLeaderIdArr = pushLeaderId.split(";");
                        List<Integer> pushLeaderIdList = new ArrayList<>();
                        for(int i=0;i<pushLeaderIdArr.length;i++){
                            String strLeaderId = pushLeaderIdArr[i];
                            int iLeaderId = Integer.parseInt(strLeaderId);
                            pushLeaderIdList.add(iLeaderId);
                        }

                        List<ImPushObjectManage> pushLeaderList = imPushObjectManageMapper.selectListByList(pushLeaderIdList);
                        if(pushLeaderList != null && pushLeaderList.size() >0){
                            String pushLeaderName = "";
                            for(ImPushObjectManage imPushObjectManage : pushLeaderList){
                                if(pushLeaderName.equals("")){
                                    pushLeaderName = imPushObjectManage.getPushObjectName();
                                }else{
                                    pushLeaderName = pushLeaderName+";"+imPushObjectManage.getPushObjectName();
                                }
                            }

                            forReportPustLeaderApproval.setPushLeaderName(pushLeaderName);
                        }
                    }
                }

            }
        }

        return pustLeaderApprovalList;
    }


    /**
     * 保存审批结果信息
     * @param param
     * @return
     */
    @Override
    public JSONObject saveApplyResult(String param) {
        JSONObject retJson = new JSONObject();
        try {
                JSONObject paramJson = JSONObject.parseObject(param);
                Integer approvalId = paramJson.getInteger("approvalId");
                String applyResult = paramJson.getString("applyResult");
                String applyOpinion = paramJson.getString("applyOpinion");

                ReportPustLeaderApproval reportPustLeaderApproval = reportPustLeaderApprovalMapper.selectById(approvalId);
                if(reportPustLeaderApproval != null){
                    reportPustLeaderApproval.setApplyState(applyResult);
                    reportPustLeaderApproval.setApplyTime(LocalDateTime.now());
                    reportPustLeaderApproval.setApplyOpinion(applyOpinion);
                    //当前登录人信息
                    SysUser currentUser = UserContext.currentUser();
                    reportPustLeaderApproval.setApproPersonId(currentUser.getUserId());

                    int upResult = reportPustLeaderApprovalMapper.updateById(reportPustLeaderApproval);
                    if(upResult > 0){

                        //更新我的任务表中的信息
                        int autoCreateId = reportPustLeaderApproval.getAutoCreateId();
                        ReportAutoCreateInfo reportAutoCreateInfo = reportAutoCreateInfoMapper.selectById(autoCreateId);
                        if(reportAutoCreateInfo != null){
                            reportAutoCreateInfo.setApplyResult(applyResult);
                            reportAutoCreateInfo.setApplyOpinion(applyOpinion);
                            reportAutoCreateInfo.setApplyTime(LocalDateTime.now());
                            reportAutoCreateInfo.setApproPersonId(currentUser.getUserId());
                            reportAutoCreateInfoMapper.updateById(reportAutoCreateInfo);

                        }

                        retJson.put("retCode","0");
                        retJson.put("retMsg","审批成功");
                    }else{
                        retJson.put("retCode","-1");
                        retJson.put("retMsg","审批失败");
                    }
                }else {
                    retJson.put("retCode","-1");
                    retJson.put("retMsg","审批失败");
                }

        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","-1");
            retJson.put("retMsg","审批失败");
        }
        return retJson;
    }


    /**
     * 根据ID获取审批状态
     * @param param
     * @return
     */
    @Override
    public JSONObject getApplyState(String param) {
        JSONObject retJson = new JSONObject();
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            Integer approvalId = paramJson.getInteger("approvalId");
            ReportPustLeaderApproval reportPustLeaderApproval = reportPustLeaderApprovalMapper.selectById(approvalId);
            if(reportPustLeaderApproval != null){
                retJson.put("retCode","0");
                retJson.put("applyState",reportPustLeaderApproval.getApplyState());
            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","获取审批状态失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","-1");
            retJson.put("retMsg","获取审批状态失败");
        }
        return retJson;
    }


    /**
     * 获取审批人
     * @return
     */
    @Override
    public JSONObject getApplyPerson() {
        JSONObject retJson = new JSONObject();
        retJson.put("retCode","0");
        retJson.put("applyPerson",reportAutoApplyPerson);
        return retJson;
    }
}
