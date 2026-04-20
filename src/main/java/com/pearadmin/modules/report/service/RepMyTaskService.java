package com.pearadmin.modules.report.service;

import com.alibaba.fastjson.JSONObject;
import com.pearadmin.modules.report.domain.ReportAutoCreateInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 创建日期：2025-08-05
 * 自助取数我的任务
 **/

public interface RepMyTaskService {

  /**
   * 获取自助取数我的任务列表数据
   * @param reportAutoCreateInfo
   * @return
   */
  public List<ReportAutoCreateInfo> getMyTaskList(ReportAutoCreateInfo reportAutoCreateInfo);

  /**
   * 单个删除自助取数我的任务
   * @param infoId
   * @return
   */
  public JSONObject remove(int infoId);


  /**
   * 单个删除自助取数我的任务
   * @param infoIds
   * @return
   */
  public JSONObject batchRemove(String infoIds);

  /**
   * 根据ID获取自助取数我的任务信息
   * @param classifyId
   * @return
   */
  public ReportAutoCreateInfo getMyTaskById(int classifyId);

  /**
   * 获取我的任务推送领导下拉框（选中值）
   * @return
   */
  public Object getPushLeaderSel(int infoId);


  /**
   * 保存我的任务数据
   * @param reportAutoCreateInfo
   * @return
   */
  public boolean saveMyTaskInfo(ReportAutoCreateInfo reportAutoCreateInfo);


  /**
   * 根据ID获取审批状态
   * @param param
   * @return
   */
  public JSONObject getTaskState(String param);


  /**
   * 修改Sql语句
   * @param reportAutoCreateInfo
   * @return
   */
  public boolean  updateSqlContentInfo(ReportAutoCreateInfo reportAutoCreateInfo);

}
