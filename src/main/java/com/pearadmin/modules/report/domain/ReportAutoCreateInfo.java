package com.pearadmin.modules.report.domain;

import com.aspose.slides.internal.oe.ass;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 * 创建日期：2025-8-4
 * 自动化报表信息体类
 *
 **/

@Data
@Alias("ReportAutoCreateInfo")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportAutoCreateInfo {

  @TableId(value = "info_id", type = IdType.AUTO)
  private Integer infoId;

  /**
   * 报表名称
   */
  private String reportName;

  /**
   * 发送周期  daily 每日  monthly 每月
   */
  private String sendCycle;

  /**
   * 发送日期的某一天
   */
  private String sendDay;

  /**
   * 发送时间
   */
  private String sendTime;

  /**
   * 推送对象ID
   */
  private String pushObjectId;

  /**
   * SQL语名
   */
  private String sqlContent;

  /**
   * 指标信息
   */
  private String indexInfo;

  /**
   * 创建时间
   */
  private LocalDateTime createTime;

  /**
   * 创建人ID
   */
  private String createUserId;


  /**
   * 状态  1执行中  0 未执行
   */
  private String state;

  /**
   * 数据周期   day 日  month 月
   */
  private String dateType;

  /**
   * 推送领导ID
   */
  private String pushLeaderId;

  /**
   * 申请结果  1待审批  2审批通过  3审批不通过
   */
  private String applyResult;

  /**
   * 审批时间
   */
  private LocalDateTime applyTime;

  /**
   * 审批意见
   */
  private String applyOpinion;

  /**
   * 审批人ID
   */
  private String approPersonId;


  /**
   * 所选指标ID，多个用;拼接
   */
  private String indexId;


  /**
   * 是否非标准  1非标准   0或空为标准
   */
  private String isNonStand;

  /**
   * 推送对象类型  zzwd组织维度  bqwd标签分组
   */
  private String pushObjectType;

  /**
   * 推送领导名称
   */
  private String pushLeaderName;


  /**
   * 推送对象名称
   */
  @TableField(exist = false)
  private String pushObjectName;

  /**
   * 创建人名称
   */
  @TableField(exist = false)
  private String createUserName;

  /**
   * 推送领导名称
   */
  /*@TableField(exist = false)
  private String pushLeaderName;*/


  /**
   * 审批人名称
   */
  @TableField(exist = false)
  private String approPersonName;

  /**
   * 新增推送领导ID
   */
  @TableField(exist = false)
  private String addPushLeaderId;





}
