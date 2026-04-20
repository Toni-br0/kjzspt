package com.pearadmin.modules.report.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.constant.CommonConstant;
import com.pearadmin.common.context.UserContext;
import com.pearadmin.common.tools.check.CheckUtil;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.im.domain.ImPushObjectManage;
import com.pearadmin.modules.im.mapper.ImPushObjectManageMapper;
import com.pearadmin.modules.report.domain.ReportIndex;
import com.pearadmin.modules.report.domain.ReportIndexWarning;
import com.pearadmin.modules.report.mapper.ReportIndexMapper;
import com.pearadmin.modules.report.mapper.ReportIndexWarningMapper;
import com.pearadmin.modules.report.service.ReportIndexWarningService;
import com.pearadmin.modules.sys.domain.SysDept;
import com.pearadmin.modules.sys.domain.SysDictData;
import com.pearadmin.modules.sys.domain.SysUser;
import com.pearadmin.modules.sys.mapper.SysDeptMapper;
import com.pearadmin.modules.sys.mapper.SysDictDataMapper;
import com.pearadmin.modules.sys.service.impl.SysUserServiceImpl;
import com.pearadmin.modules.xfppt.domain.XfpptFilePushManage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 创建日期：2025-10-16
 **/
@Slf4j
@Service
public class ReportIndexWarningServiceImpl implements ReportIndexWarningService {

    @Resource
    private ReportIndexWarningMapper reportIndexWarningMapper;

    @Resource
    private ImPushObjectManageMapper imPushObjectManageMapper;

    @Resource
    private ReportIndexMapper reportIndexMapper;

    @Resource
    private SysDictDataMapper sysDictDataMapper;

    @Resource
    private SysDeptMapper sysDeptMapper;


    /**
     * 获取指标预警列表数据
     * @param reportIndexWarning
     * @return
     */
    @Override
    public List<ReportIndexWarning> getIndexWarningList(ReportIndexWarning reportIndexWarning) {
        //当前登录人信息
        SysUser currentUser = UserContext.currentUser();
        String loginUserId = currentUser.getUserId();
        boolean isAdmin = CheckUtil.hasSuperAdmin(currentUser);
        if(isAdmin){
            loginUserId = null;
        }

        List<ReportIndexWarning> list = reportIndexWarningMapper.getReportIndexWarningList(reportIndexWarning,loginUserId);
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
     * 单个删除指标预警数据
     * @param warningId
     * @return
     */
    @Override
    public JSONObject remove(String warningId) {
        JSONObject retJsonObject = new JSONObject();
        try {
            int deleteResult = reportIndexWarningMapper.deleteById(warningId);
            if(deleteResult >0){
                retJsonObject.put("retCode","0");
                retJsonObject.put("retMsg","删除成功");
            }else{
                retJsonObject.put("retCode","-1");
                retJsonObject.put("retMsg","删除失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            retJsonObject.put("retCode","-1");
            retJsonObject.put("retMsg","删除失败");
        }
        return retJsonObject;
    }


    /**
     * 批量删除指标预警数据
     * @param warningIds
     * @return
     */
    @Override
    public JSONObject batchRemove(String warningIds) {
        JSONObject retJsonObject = new JSONObject();
        try {
            List<String> delList = new ArrayList<>();
            for (String warningId : warningIds.split(CommonConstant.COMMA)) {
                delList.add(warningId);
            }

            int deleteResult = reportIndexWarningMapper.deleteBatchIds(delList);
            if(deleteResult >0){
                retJsonObject.put("retCode","0");
                retJsonObject.put("retMsg","删除成功");
            }else{
                retJsonObject.put("retCode","-1");
                retJsonObject.put("retMsg","删除失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            retJsonObject.put("retCode","-1");
            retJsonObject.put("retMsg","删除失败");
        }
        return retJsonObject;
    }

    /**
     * 根据分类获取指标下拉框
     * @param classifyId
     * @return
     */
    @Override
    public Object getIndexSelect(int classifyId,int warningIndexId) {
        List<JSONObject> retList = new ArrayList<>();
        try {
            QueryWrapper<ReportIndex> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("classify_id", classifyId);
            queryWrapper.eq("index_type", "count");
            queryWrapper.and(w -> w.eq("index_class", "lv").or().eq("index_class", "zb"));

            List<ReportIndex> reportIndexList = reportIndexMapper.selectList(queryWrapper);
            ReportIndex parentReportIndex = null;
            String parentIndexName = "";
            if(reportIndexList !=null && reportIndexList.size() >0){
                for(ReportIndex reportIndex : reportIndexList){
                    parentIndexName ="";
                    int queryIndexId = reportIndex.getParentId();
                    parentReportIndex = reportIndexMapper.selectById(queryIndexId);
                    if(parentReportIndex != null){
                        parentIndexName = parentReportIndex.getIndexName();
                    }

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("name","["+parentIndexName+"]_"+reportIndex.getIndexName());
                    //jsonObject.put("name",reportIndex.getIndexName());
                    jsonObject.put("value",reportIndex.getIndexId());
                    if(warningIndexId == reportIndex.getIndexId()){
                        jsonObject.put("selected",true);
                    }

                    retList.add(jsonObject);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return retList;
    }


    /**
     * 获取推送对象管理下拉框
     * @param localCity
     * @return
     */
    @Override
    public Object getPushObjectSelectSel(String localCity,String pushObjectId) {
        List<JSONObject> retList = new ArrayList<>();
        try {
            //当前登录人信息
            SysUser currentUser = UserContext.currentUser();
            String deptId = currentUser.getDeptId()==null?"":currentUser.getDeptId();

            QueryWrapper<ImPushObjectManage> queryWrapper = new QueryWrapper<>();

            if(deptId.equals("1")){ //区公司
                queryWrapper.or(qw -> {
                    qw.eq("object_area", localCity)  // 条件1：object_area = '乌鲁木齐'
                            .or()                            // 内部OR连接
                            .eq("object_area", "区公司");   // 条件2：object_area = '区公司'
                });
            }else{
                queryWrapper.eq("object_area", localCity);
            }

            queryWrapper.isNotNull("bind_time");
            queryWrapper.orderByDesc("create_time");

            List<ImPushObjectManage> list = imPushObjectManageMapper.selectList(queryWrapper);
            if(list != null && list.size() >0){

                String[] pushObjectIdArr = null;
                if(StringUtil.isNotEmpty(pushObjectId)){
                    pushObjectIdArr = pushObjectId.split(";");
                }


                for(ImPushObjectManage imPushObjectManage : list){
                    JSONObject jsonObject = new JSONObject();

                    int queryManageId = imPushObjectManage.getManageId();

                    //jsonObject.put("name","["+pushObjectType+"]"+imPushObjectManage.getPushObjectName());
                    jsonObject.put("name","["+imPushObjectManage.getObjectArea()+"]"+imPushObjectManage.getPushObjectName());
                    jsonObject.put("value",imPushObjectManage.getPushObjectId());
                    jsonObject.put("type",imPushObjectManage.getPushObjectType());
                    jsonObject.put("id",imPushObjectManage.getManageId());

                    if(pushObjectIdArr != null && pushObjectIdArr.length > 0){
                        for(String strPushObjectId :pushObjectIdArr){
                            if(StringUtil.isNotEmpty(strPushObjectId)){
                                int iPushObjectId = Integer.parseInt(strPushObjectId);

                                if(iPushObjectId == queryManageId){
                                    jsonObject.put("selected",true);
                                }
                            }
                        }
                    }

                    retList.add(jsonObject);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return retList;
    }


    /**
     * 保存指标预警数据
     * @param reportIndexWarning
     * @return
     */
    @Override
    public JSONObject saveIndexWarning(ReportIndexWarning reportIndexWarning) {
        JSONObject retJsonObject = new JSONObject();
        try {

            QueryWrapper<ReportIndexWarning> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("warning_index_id", reportIndexWarning.getWarningIndexId());
            queryWrapper.eq("local_city", reportIndexWarning.getLocalCity());
            queryWrapper.last("limit 1");
            ReportIndexWarning queryReportIndexWarning = reportIndexWarningMapper.selectOne(queryWrapper);

            if(queryReportIndexWarning != null){
                retJsonObject.put("retCode","-1");
                retJsonObject.put("retMsg","预警指标已存在");
                return retJsonObject;
            }

            //当前登录人信息
            SysUser currentUser = UserContext.currentUser();
            reportIndexWarning.setCreateUserId(currentUser.getUserId());
            reportIndexWarning.setCreateTime(LocalDateTime.now());
            reportIndexWarning.setExecCount(0);

            if(StringUtil.isNotEmpty(reportIndexWarning.getPushOtherObjectId())){
                reportIndexWarning.setApprState("0");
            }

            int addResult = reportIndexWarningMapper.insert(reportIndexWarning);
            if(addResult >0){
                retJsonObject.put("retCode","0");
                retJsonObject.put("retMsg","保存成功");
            }else{
                retJsonObject.put("retCode","-1");
                retJsonObject.put("retMsg","保存失败");
            }

        }catch (Exception e){
            e.printStackTrace();
            retJsonObject.put("retCode", "-1");
            retJsonObject.put("retMsg", "保存失败");
        }
        return retJsonObject;
    }

    /**
     * 修改指标预警数据
     * @param reportIndexWarning
     * @return
     */
    @Override
    public JSONObject updateIndexWarning(ReportIndexWarning reportIndexWarning) {
        JSONObject retJsonObject = new JSONObject();
        try {

            QueryWrapper<ReportIndexWarning> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("warning_index_id", reportIndexWarning.getWarningIndexId());
            queryWrapper.ne("warning_id",reportIndexWarning.getWarningId());
            queryWrapper.eq("local_city", reportIndexWarning.getLocalCity());
            queryWrapper.last("limit 1");
            ReportIndexWarning queryReportIndexWarning = reportIndexWarningMapper.selectOne(queryWrapper);

            if(queryReportIndexWarning != null){
                retJsonObject.put("retCode","-1");
                retJsonObject.put("retMsg","预警指标已存在");
                return retJsonObject;
            }

            //根据ID查询预警信息
            ReportIndexWarning originalReportIndexWarning = reportIndexWarningMapper.selectById(reportIndexWarning.getWarningId());
            if(originalReportIndexWarning !=null){
                String pushOtherObjectId = originalReportIndexWarning.getPushOtherObjectId();
                String upPushOtherObjectId = reportIndexWarning.getPushOtherObjectId();
                //其他推送对象为空
                if(StringUtil.isEmpty(upPushOtherObjectId)){
                    reportIndexWarning.setApprState("");
                    reportIndexWarning.setApprPersonId("");
                    reportIndexWarning.setApprTime(null);
                    reportIndexWarning.setApplyOpinion("");
                }else if(StringUtil.isNotEmpty(upPushOtherObjectId) && !upPushOtherObjectId.equals(pushOtherObjectId)){ //原有的指标触发推送其他对象ID和现在的不一致，需要更新推送对象管理表中的审批状态
                    reportIndexWarning.setApprState("0");
                    reportIndexWarning.setApprPersonId("");
                    reportIndexWarning.setApprTime(null);
                    reportIndexWarning.setApplyOpinion("");
                }
            }

            //当前登录人信息
            SysUser currentUser = UserContext.currentUser();
            reportIndexWarning.setCreateUserId(currentUser.getUserId());
            reportIndexWarning.setCreateTime(LocalDateTime.now());
            int addResult = reportIndexWarningMapper.updateById(reportIndexWarning);
            if(addResult >0){
                retJsonObject.put("retCode","0");
                retJsonObject.put("retMsg","修改成功");
            }else{
                retJsonObject.put("retCode","-1");
                retJsonObject.put("retMsg","修改失败");
            }

        }catch (Exception e){
            e.printStackTrace();
            retJsonObject.put("retCode", "-1");
            retJsonObject.put("retMsg", "保存失败");
        }
        return retJsonObject;
    }


    /**
     * 根据 ID查询对象
     * @param warningId
     * @return
     */
    @Override
    public ReportIndexWarning getById(String warningId) {

        return reportIndexWarningMapper.selectById(warningId);
    }

    /**
     * 获取当前登录人数据字典中地市的值
     * @return
     */
    @Override
    public List<SysDictData> getSysDictDsData() {

        String deptName = "";
        //当前登录人信息
        SysUser currentUser = UserContext.currentUser();
        String deptId = currentUser.getDeptId();
        if(StringUtil.isNotEmpty(deptId) && !deptId.equals("1")){ //分公司
            SysDept sysDept = sysDeptMapper.selectById(deptId);
            if(sysDept != null){
                deptName = sysDept.getDeptName();
            }
        }

        QueryWrapper<SysDictData> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type_code","sys_dishi_value");
        if(StringUtil.isNotEmpty(deptName)){
            queryWrapper.eq("data_value",deptName);
        }

        queryWrapper.orderByAsc("sort");

        List<SysDictData> list = sysDictDataMapper.selectList(queryWrapper);

        return list;
    }
}
