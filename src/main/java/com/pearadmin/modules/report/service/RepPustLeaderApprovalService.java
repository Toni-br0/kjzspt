package com.pearadmin.modules.report.service;

import com.alibaba.fastjson.JSONObject;
import com.pearadmin.modules.report.domain.ReportPustLeaderApproval;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 创建日期：2025-08-22
 * 自助取数报表推送至领导IM申请表
 **/

public interface RepPustLeaderApprovalService {

  /**
   * 获取自助取数报表推送至领导IM申请管理列表数据
   * @param reportPustLeaderApproval
   * @return
   */
  public List<ReportPustLeaderApproval> getPustLeaderApprovalList(ReportPustLeaderApproval reportPustLeaderApproval);

  /**
   * 保存审批结果信息
   * @param param
   * @return
   */
  public JSONObject saveApplyResult(String param);


  /**
   * 根据ID获取审批状态
   * @param param
   * @return
   */
  public JSONObject getApplyState(String param);


  /**
   * 获取审批人
   * @return
   */
  public JSONObject getApplyPerson();

}
