package com.pearadmin.modules.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pearadmin.modules.report.domain.ReportAutoCreateInfo;
import com.pearadmin.modules.report.domain.ReportIndexWarning;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 创建日期：2025-10-16
 * 指标预警配置表
 **/

@Mapper
public interface ReportIndexWarningMapper extends BaseMapper<ReportIndexWarning> {

  /**
   * 根据查询条件查询指标预警配置表列表
   * @param reportIndexWarning
   * @return
   */
  public List<ReportIndexWarning> getReportIndexWarningList(@Param("reportIndexWarning") ReportIndexWarning reportIndexWarning,@Param("loginUserId") String loginUserId);


  /**
   * 根据开始日期和结束日期查询指标预警配置表
   * @param startTime
   * @param endTime
   * @return
   */
  public List<ReportIndexWarning> getListByTime(@Param("startTime") String startTime, @Param("endTime") String endTime);


  /**
   * 根据查询条件查询指标预警审批列表数据
   * @param reportIndexWarning
   * @return
   */
  public List<ReportIndexWarning> getReportIndexWarningApprovList(@Param("reportIndexWarning") ReportIndexWarning reportIndexWarning,@Param("loginUserId") String loginUserId);

  /**
   * 通过List查询有已审批过的记录
   * @param warningIdList
   * @return
   */
  public List<ReportIndexWarning> selApprStateByList(@Param("warningIdList") List<String> warningIdList);


  /**
   * 批量修改审批结果
   * @param warningIdList
   * @param applyResult
   * @param applyOpinion
   * @return
   */
  public int updateApplyResult(@Param("warningIdList") List<String> warningIdList,@Param("applyResult") String applyResult,@Param("applyOpinion") String applyOpinion,@Param("userId") String userId);

}
