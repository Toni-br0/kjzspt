package com.pearadmin.modules.report.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 * 创建日期：2025-08-21
 * 自助取数报表推送至领导IM申请表
 **/

@Data
@Alias("ReportPustLeaderApproval")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportPustLeaderApproval {

  @TableId(value = "approval_id", type = IdType.AUTO)
  private Integer approvalId;

  /**
   * 报表名称
   */
  private String reportName;

  /**
   * 报表推送时间
   */
  private String pushTime;

  /**
   * 推送领导ID
   */
  private String pushLeaderId;

  /**
   * 申请人ID
   */
  private String applyPersonId;

  /**
   * 申请时间
   */
  private LocalDateTime createTime;

  /**
   * 审批状态  1待审批  2审批通过  3审批不通过
   */
  private String applyState;

  /**
   * 审批状态  1待审批  2审批通过  3审批不通过
   */
  private LocalDateTime applyTime;

  /**
   * 审批意见
   */
  private String applyOpinion;

  /**
   * 自动创建报表表主键
   */
  private int autoCreateId;

  /**
   * 审批人ID
   */
  private String approPersonId;

  /**
   * 推送领导名称s
   */
  private String pushLeaderName;

  /**
   * 推送对象类型  zzwd组织维度  bqwd标签分组
   */
  private String pushObjectType;

  /**
   * 申请人
   */
  @TableField(exist = false)
  private String applyPersonName;


  /**
   * 审批人姓名
   */
  @TableField(exist = false)
  private String approPersonName;


}
