package com.pearadmin.modules.report.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.constant.CommonConstant;
import com.pearadmin.common.context.UserContext;
import com.pearadmin.common.tools.check.CheckUtil;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.im.domain.ImHdjkPushManage;
import com.pearadmin.modules.im.domain.ImPushObjectManage;
import com.pearadmin.modules.im.mapper.ImPushObjectManageMapper;
import com.pearadmin.modules.report.domain.ReportAutoCreateInfo;
import com.pearadmin.modules.report.domain.ReportPustLeaderApproval;
import com.pearadmin.modules.report.mapper.ReportAutoCreateInfoMapper;
import com.pearadmin.modules.report.mapper.ReportPustLeaderApprovalMapper;
import com.pearadmin.modules.report.service.RepMyTaskService;
import com.pearadmin.modules.sys.domain.SysUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 创建日期：2025-08-05
 **/

@Slf4j
@Service
public class RepMyTaskServiceImpl implements RepMyTaskService {

  @Resource
  private ReportAutoCreateInfoMapper reportAutoCreateInfoMapper;

  @Resource
  private ImPushObjectManageMapper imPushObjectManageMapper;

  @Resource
  private ReportPustLeaderApprovalMapper reportPustLeaderApprovalMapper;

  /**
   * 获取自助取数我的任务列表数据
   * @param reportAutoCreateInfo
   * @return
   */
  @Override
  public List<ReportAutoCreateInfo> getMyTaskList(ReportAutoCreateInfo reportAutoCreateInfo) {
    //当前登录人信息
    SysUser currentUser = UserContext.currentUser();
    String loginUserId = currentUser.getUserId();
    /*if(loginUserId.equals("1309861917694623744")){ //管理员
      loginUserId = null;
    }*/
    boolean isAdmin = CheckUtil.hasSuperAdmin(currentUser);
    if(isAdmin){
      loginUserId = null;
    }

    List<ReportAutoCreateInfo> list = reportAutoCreateInfoMapper.getListByWhere(reportAutoCreateInfo,loginUserId);
    if(list !=null && list.size() >0){
      for(ReportAutoCreateInfo queryReportAutoCreateInfo : list){

        String pushObjectId = queryReportAutoCreateInfo.getPushObjectId();
        String[] pushObjectIdArr = pushObjectId.split(";");

        List<Integer> pushObjectIdList = new ArrayList<>();
        for(int i=0;i<pushObjectIdArr.length;i++){
          String strPushObjectId = pushObjectIdArr[i];
          int iPushObjectId = Integer.parseInt(strPushObjectId);
          pushObjectIdList.add(iPushObjectId);
          /*ImPushObjectManage imPushObjectManage = imPushObjectManageMapper.selectById(strPushObjectId);
          if(imPushObjectManage != null){
            if(!pushObjectName.equals("")){
              pushObjectName = pushObjectName+";"+imPushObjectManage.getPushObjectName();
            }else{
              pushObjectName = imPushObjectManage.getPushObjectName();
            }
          }*/
        }

        List<ImPushObjectManage> pushObjectList = imPushObjectManageMapper.selectListByList(pushObjectIdList);
        if(pushObjectList != null && pushObjectList.size() >0){
          String pushObjectName = "";
          for(ImPushObjectManage imPushObjectManage : pushObjectList){
            if(pushObjectName.equals("")){
              pushObjectName = imPushObjectManage.getPushObjectName();
            }else{
              pushObjectName = pushObjectName+";"+imPushObjectManage.getPushObjectName();
            }
          }

          queryReportAutoCreateInfo.setPushObjectName(pushObjectName);
        }

        String pushObjectType = queryReportAutoCreateInfo.getPushObjectType()==null?"":queryReportAutoCreateInfo.getPushObjectType();
        if(!pushObjectType.equals("zzwd")){
          //获取推送领导名称
          String pushLeaderId = queryReportAutoCreateInfo.getPushLeaderId();
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

              queryReportAutoCreateInfo.setPushLeaderName(pushLeaderName);
            }
          }
        }

      }
    }

    return list;
  }

  /**
   * 单个删除自助取数我的任务
   * @param infoId
   * @return
   */
  @Override
  public JSONObject remove(int infoId) {
    JSONObject retJson = new JSONObject();
    int result = reportAutoCreateInfoMapper.deleteById(infoId);
    if(result > 0){
      //删除推送至领导审批记录
      /*QueryWrapper<ReportPustLeaderApproval> delQueryWrapper = new QueryWrapper<>();
      delQueryWrapper.eq("auto_create_id",infoId);

      reportPustLeaderApprovalMapper.delete(delQueryWrapper);*/
      List<Integer> delList = new ArrayList<>();
      delList.add(infoId);
      reportPustLeaderApprovalMapper.delByAutoCreateId(delList);

      retJson.put("retCode","0");
      retJson.put("retMsg","删除成功");
    }else {
      retJson.put("retCode","-1");
      retJson.put("retMsg","删除失败");
    }

    return retJson;
  }

  /**
   * 单个删除自助取数我的任务
   * @param infoIds
   * @return
   */
  @Override
  public JSONObject batchRemove(String infoIds) {
    JSONObject retJson = new JSONObject();
    try {
      List<Integer> delList = new ArrayList<>();
      for (String infoId : infoIds.split(CommonConstant.COMMA)) {
        int iInfoId = Integer.parseInt(infoId);
        delList.add(iInfoId);
      }

      int result = reportAutoCreateInfoMapper.deleteBatchIds(delList);
      if(result >0){
        //删除推送至领导审批记录
        reportPustLeaderApprovalMapper.delByAutoCreateId(delList);

        retJson.put("retCode","0");
        retJson.put("retMsg","删除成功");
      }else{
        retJson.put("retCode","-1");
        retJson.put("retMsg","删除失败");
      }

    }catch (Exception e){
      e.printStackTrace();
      retJson.put("retCode","-1");
      retJson.put("retMsg","删除失败,系统异常,请联系管理员");
    }
    return retJson;
  }

  /**
   * 根据ID获取自助取数我的任务信息
   * @param classifyId
   * @return
   */
  @Override
  public ReportAutoCreateInfo getMyTaskById(int classifyId) {
    ReportAutoCreateInfo reportAutoCreateInfo = reportAutoCreateInfoMapper.selectById(classifyId);
    if(reportAutoCreateInfo != null){
      String pushObjectId = reportAutoCreateInfo.getPushObjectId();
      if(StringUtil.isNotEmpty(pushObjectId)){
        String[] pushObjectIdArr = pushObjectId.split(";");
        List<Integer> pushObjectIdList = new ArrayList<>();
        for(int i=0;i<pushObjectIdArr.length;i++){
          String strPushObjectId = pushObjectIdArr[i];
          int iPushObjectId = Integer.parseInt(strPushObjectId);
          pushObjectIdList.add(iPushObjectId);
        }

        List<ImPushObjectManage> pushObjectList = imPushObjectManageMapper.selectListByList(pushObjectIdList);
        if(pushObjectList != null && pushObjectList.size() >0){
          String pushObjectName = "";
          for(ImPushObjectManage imPushObjectManage : pushObjectList){
            if(pushObjectName.equals("")){
              pushObjectName = imPushObjectManage.getPushObjectName();
            }else{
              pushObjectName = pushObjectName+";"+imPushObjectManage.getPushObjectName();
            }
          }

          reportAutoCreateInfo.setPushObjectName(pushObjectName);
        }
      }
    }
    return reportAutoCreateInfo;
  }

  /**
   * 获取我的任务推送领导下拉框（选中值）
   * @param infoId
   * @return
   */
  @Override
  public Object getPushLeaderSel(int infoId) {
    List<JSONObject> retList = new ArrayList<>();

    ReportAutoCreateInfo reportAutoCreateInfo = reportAutoCreateInfoMapper.selectById(infoId);
    if(reportAutoCreateInfo != null){
      String pushObjectId = reportAutoCreateInfo.getPushLeaderId();

      QueryWrapper<ImPushObjectManage> queryWrapper = new QueryWrapper<>();
      queryWrapper.orderByDesc("create_time");
      queryWrapper.isNotNull("bind_time");
      List<ImPushObjectManage> list = imPushObjectManageMapper.selectList(queryWrapper);
      if(list != null && list.size() >0){

        String[] pushObjectIdArr = pushObjectId.split(";");

        for(ImPushObjectManage imPushObjectManage : list){
          String pushObjectType = imPushObjectManage.getPushObjectType();
          int queryManageId = imPushObjectManage.getManageId();

                /*if(pushObjectType.equals("users")){
                    pushObjectType = "用户";
                }else if(pushObjectType.equals("group")){
                    pushObjectType = "群";
                }*/

          JSONObject jsonObject = new JSONObject();
          //jsonObject.put("name","["+pushObjectType+"]"+imPushObjectManage.getPushObjectName());
          jsonObject.put("name","["+imPushObjectManage.getObjectArea()+"]"+imPushObjectManage.getPushObjectName());
          jsonObject.put("value",imPushObjectManage.getPushObjectId());
          jsonObject.put("type",imPushObjectManage.getPushObjectType());
          jsonObject.put("id",imPushObjectManage.getManageId());

          if(pushObjectIdArr != null && pushObjectIdArr.length >0){
            for(String strPushObjectId :pushObjectIdArr){
              int iPushObjectId = Integer.parseInt(strPushObjectId);
              if(iPushObjectId == queryManageId){
                jsonObject.put("selected",true);
              }
            }
          }

          retList.add(jsonObject);
        }
      }

    }

    return retList;
  }


  /**
   * 保存我的任务数据
   * @param reportAutoCreateInfo
   * @return
   */
  @Override
  public boolean saveMyTaskInfo(ReportAutoCreateInfo reportAutoCreateInfo) {
    boolean result = false;
    int infoId = reportAutoCreateInfo.getInfoId();
    ReportAutoCreateInfo queryReportAutoCreateInfo = reportAutoCreateInfoMapper.selectById(infoId);
    if(queryReportAutoCreateInfo != null){

      String classType = queryReportAutoCreateInfo.getPushObjectType() ==null?"":queryReportAutoCreateInfo.getPushObjectType();
      String addPushLeaderId = "";
      String addPushLeaderName  ="";

      String pushLeaderId = reportAutoCreateInfo.getAddPushLeaderId();
      if(StringUtil.isNotEmpty(pushLeaderId)){
        JSONArray pushLeaderArr = JSONObject.parseArray(pushLeaderId);
        for(Object pushLeaderObj : pushLeaderArr){
           JSONObject pushLeaderJson = (JSONObject) pushLeaderObj;
           classType = pushLeaderJson.getString("classType")==null?"":pushLeaderJson.getString("classType");
           String manageId = pushLeaderJson.getString("manageId")==null?"":pushLeaderJson.getString("manageId");
           String phone = pushLeaderJson.getString("phone")==null?"":pushLeaderJson.getString("phone");
           String name = pushLeaderJson.getString("name")==null?"":pushLeaderJson.getString("name");

           //不为组织维度时
           if(!classType.equals("zzwd")){
             if(StringUtil.isEmpty(addPushLeaderId)){
               addPushLeaderId = manageId;
             }else{
               addPushLeaderId = addPushLeaderId+";"+manageId;
             }
           }else{
             if(StringUtil.isEmpty(addPushLeaderId)){
               addPushLeaderId = phone;
             }else{
               addPushLeaderId = addPushLeaderId+";"+phone;
             }
           }

           if(StringUtil.isEmpty(addPushLeaderName)){
             addPushLeaderName = name;
           }else{
             addPushLeaderName = addPushLeaderName+";"+name;
           }

        }
      }

      queryReportAutoCreateInfo.setPushObjectType(classType);

      queryReportAutoCreateInfo.setReportName(reportAutoCreateInfo.getReportName());
      queryReportAutoCreateInfo.setSendCycle(reportAutoCreateInfo.getSendCycle());
      queryReportAutoCreateInfo.setSendDay(reportAutoCreateInfo.getSendDay());
      queryReportAutoCreateInfo.setSendTime(reportAutoCreateInfo.getSendTime());
      queryReportAutoCreateInfo.setPushLeaderId(addPushLeaderId);
      queryReportAutoCreateInfo.setPushLeaderName(addPushLeaderName);

      int updateResult = reportAutoCreateInfoMapper.updateById(queryReportAutoCreateInfo);
      if(updateResult > 0){

        QueryWrapper<ReportPustLeaderApproval> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("auto_create_id",infoId);

        ReportPustLeaderApproval reportPustLeaderApproval = reportPustLeaderApprovalMapper.selectOne(queryWrapper);
        //更新领导审批中的数据信息
        if(reportPustLeaderApproval != null){
          reportPustLeaderApproval.setReportName(reportAutoCreateInfo.getReportName());
          reportPustLeaderApproval.setPushLeaderId(reportAutoCreateInfo.getPushLeaderId());
          reportPustLeaderApproval.setCreateTime(LocalDateTime.now());

          String sendTime ="";
          if(reportAutoCreateInfo.getSendCycle().equals("daily")){ //每日
            sendTime =  reportAutoCreateInfo.getSendTime().replaceAll(":","时")+"分";
          }else{
            sendTime =  reportAutoCreateInfo.getSendDay()+"日"+reportAutoCreateInfo.getSendTime().replaceAll(":","时")+"分";
          }

          reportPustLeaderApproval.setPushTime(sendTime);

          reportPustLeaderApprovalMapper.updateById(reportPustLeaderApproval);

        }else{
          //推送领导不为空，时新增审批记录
          if(StringUtil.isNotEmpty(reportAutoCreateInfo.getPushLeaderId())){
            reportPustLeaderApproval = new ReportPustLeaderApproval();
            reportPustLeaderApproval.setReportName(reportAutoCreateInfo.getReportName());
            reportPustLeaderApproval.setPushLeaderId(reportAutoCreateInfo.getPushLeaderId());
            reportPustLeaderApproval.setApplyPersonId(queryReportAutoCreateInfo.getCreateUserId());
            reportPustLeaderApproval.setCreateTime(LocalDateTime.now());
            reportPustLeaderApproval.setApplyState("1");
            reportPustLeaderApproval.setAutoCreateId(infoId);

            reportPustLeaderApproval.setPushObjectType(classType);
            reportPustLeaderApproval.setPushLeaderName(addPushLeaderName);
            reportPustLeaderApproval.setPushLeaderId(addPushLeaderId);

            String sendTime ="";
            if(reportAutoCreateInfo.getSendCycle().equals("daily")){ //每日
              sendTime =  reportAutoCreateInfo.getSendTime().replaceAll(":","时")+"分";
            }else{
              sendTime =  reportAutoCreateInfo.getSendDay()+"日"+reportAutoCreateInfo.getSendTime().replaceAll(":","时")+"分";
            }

            reportPustLeaderApproval.setPushTime(sendTime);

            reportPustLeaderApprovalMapper.insert(reportPustLeaderApproval);

          }
        }

        result = true;

      }


    }

    return result;
  }


  /**
   * 根据ID获取审批状态
   * @param param
   * @return
   */
  @Override
  public JSONObject getTaskState(String param) {
    JSONObject retJsonObj = new JSONObject();
    try {
        JSONObject jsonObject = JSONObject.parseObject(param);
        int infoId = jsonObject.getIntValue("infoId");
        ReportAutoCreateInfo reportAutoCreateInfo = reportAutoCreateInfoMapper.selectById(infoId);
        if(reportAutoCreateInfo != null){
          retJsonObj.put("retCode","0");
          retJsonObj.put("repState",reportAutoCreateInfo.getApplyResult());
        }else{
          retJsonObj.put("retCode","-1");
          retJsonObj.put("retMsg","获取记录审批状态异常");
        }

    }catch (Exception e){
      e.printStackTrace();
      retJsonObj.put("retCode","-1");
      retJsonObj.put("retMsg","获取记录审批状态异常");

    }
    return retJsonObj;
  }


  /**
   * 修改Sql语句
   * @param reportAutoCreateInfo
   * @return
   */
  @Override
  public boolean updateSqlContentInfo(ReportAutoCreateInfo reportAutoCreateInfo) {

    int infoId = reportAutoCreateInfo.getInfoId();
    ReportAutoCreateInfo queryReportAutoCreateInfo = reportAutoCreateInfoMapper.selectById(infoId);
    if(queryReportAutoCreateInfo != null && StringUtil.isNotEmpty(reportAutoCreateInfo.getSqlContent())){
      queryReportAutoCreateInfo.setSqlContent(reportAutoCreateInfo.getSqlContent());
      int updateResult = reportAutoCreateInfoMapper.updateById(queryReportAutoCreateInfo);
      if(updateResult >0){
        return true;
      }else{
        return false;
      }
    }

    return false;
  }
}
