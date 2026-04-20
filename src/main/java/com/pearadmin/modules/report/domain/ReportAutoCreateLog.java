package com.pearadmin.modules.report.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 * 创建日期：2025-8-4
 * 自动化报表日志
 *
 **/

@Data
@Alias("ReportAutoCreateLog")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportAutoCreateLog {

  @TableId(value = "log_id", type = IdType.AUTO)
  private Integer logId;

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
   * SQL语句
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
   * 是否成功 0成功  -1失败
   */
  private String isSuccess;

  /**
   * 返回信息
   */
  private String interRetmsg;

  /**
   * 文件路径
   */
  private String filePath;

  /**
   * 数据周期   day 日  month 月
   */
  private String dateType;

  /**
   * 所选指标ID，多个用;拼接
   */
  private String indexId;

  /**
   * 是否非标准  1非标准   0或空为标准
   */
  private String isNonStand;

  /**
   * 推送对象名称
   */
  private String pushObjectName;

  /**
   * 推送对象类型  zzwd组织维度  bqwd标签分组
   */
  private String pushObjectType;


  /**
   * 创建人名称
   */
  @TableField(exist = false)
  private String createUserName;

}
