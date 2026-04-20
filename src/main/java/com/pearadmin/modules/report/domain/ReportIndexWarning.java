package com.pearadmin.modules.report.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 * 创建日期：2025-10-16
 * 指标预警配置表
 **/

@Data
@Alias("ReportIndexWarning")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportIndexWarning {

  /**
   * 预警ID
   */
  @TableId
  private String warningId;


  /**
   * 预警指标分类
   */
  private Integer indexClass;

  /**
   * 预警指标ID
   */
  private Integer warningIndexId;

  /**
   * 所属地市
   */
  private String localCity;

  /**
   * 指标标准值
   */
  private String indexStanValue;


  /**
   * 指标偏离范围  高于above   低于 below
   */
  private String indexDeviRange;

  /**
   * 指标触发推送对象Id
   */
  private String pushObjectId;

  /**
   * 指标触发推送其他对象ID
   */
  private String pushOtherObjectId;


  /**
   * 执行周期 month月 week周 day日
   */
  private String execPeriod;


  /**
   * 数据周期  month月 day日
   */
  private String dataPeriod;


  /**
   * 执行日期(天)
   */
  private String execDay;

  /**
   * 执行周 MONDAY 周一, TUESDAY 周二,WEDNESDAY 周三,THURSDAY 周四,FRIDAY 周五,SATURDAY 周六,SUNDAY 周日
   */
  private String execWeek;

  /**
   * 执行日期 时间点
   */
  private String execTime;

  /**
   * 执行次数
   */
  private Integer execCount;

  /**
   * 创建时间
   */
  private LocalDateTime createTime;

  /**
   * 创建人ID
   */
  private String createUserId;


  /**
   * 预警级别  紧急 urgent  普通 ordinary  告警 alarm
   */
  private String warningLevel;

  /**
   * 执行状态  1执行中  0未执行
   */
  private String execState;


  /**
   * 预警信息
   */
  private String warningMsg;


  /**
   * 审批状态  0未审批，1审批通过，2审批不通过
   */
  private String apprState;


  /**
   * 审批人ID
   */
  private String apprPersonId;


  /**
   * 审批时间
   */
  @TableField(value = "appr_time", updateStrategy = FieldStrategy.IGNORED)
  private LocalDateTime apprTime;

  /**
   * 审批意见
   */
  private String applyOpinion;

  /**
   * 预警名称
   */
  private String warningName;


  /**
   * 创建人名称
   */
  @TableField(exist = false)
  private String realName;

  /**
   * 指标名称
   */
  @TableField(exist = false)
  private String indexName;

  /**
   * 指标触发推送对象名称
   */
  @TableField(exist = false)
  private String pushObjectName;

  /**
   * 指标触发推送其他对象名称
   */
  @TableField(exist = false)
  private String pushOtherObjectName;


  /**
   * 审批人姓名
   */
  @TableField(exist = false)
  private String apprPersonName;


}
