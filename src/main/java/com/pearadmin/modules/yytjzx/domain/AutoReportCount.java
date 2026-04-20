package com.pearadmin.modules.yytjzx.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

/**
 * 创建日期：2025-10-29
 * 自助取数统计类
 **/
@Data
@Alias("AutoReportCount")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutoReportCount {

  /**
   * 区域名称
   */
  private String areaName;

  /**
   * 服务用户数
   */
  private int yhFwyhs;

  /**
   * 活跃用户数
   */
  private int yhHyyhs;

  /**
   * 用户活跃率
   */
  private String yhYhhylv;

  /**
   * 指标基础数据
   */
  private int zbJcsj;

  /**
   * 指标宽带控流失
   */
  private int zbKdkls;

  /**
   * 指标高值保拓
   */
  private int zbGzbt;

  /**
   * 指标流量深耕
   */
  private int zbLlsg;

  /**
   * 指标融合重耕
   */
  private int zbRhcg;

  /**
   * 指标移动控流失
   */
  private int zbYdkls;

  /**
   * 生成报表基础数据
   */
  private int scbbJcsj;

  /**
   * 生成报表宽带控流失
   */
  private int scbbKdkls;

  /**
   * 生成报表高值保拓
   */
  private int scbbGzbt;

  /**
   * 生成报表流量深耕
   */
  private int scbbLlsg;

  /**
   * 生成报表融合重耕
   */
  private int scbbRhcg;

  /**
   * 生成报表移动控流失
   */
  private int scbbYdkls;

  /**
   * 自动推送报表基础数据
   */
  private int zdtsbbJcsj;

  /**
   * 自动推送报表宽带控流失
   */
  private int zdtsbbKdkls;

  /**
   * 自动推送报表高值保拓
   */
  private int zdtsbbGzbt;

  /**
   * 自动推送报表流量深耕
   */
  private int zdtsbbLlsg;

  /**
   * 自动推送报表融合重耕
   */
  private int zdtsbbRhcg;

  /**
   * 自动推送报表移动控流失
   */
  private int zdtsbbYdkls;

  /**
   * 预警指标总数
   */
  private int warningIndexCount;

  /**
   * 预警指标紧急数
   */
  private int warningIndexUrgentCount;

  /**
   * 预警指标告警数
   */
  private int warningIndexAlarmCount;

  /**
   * 预警指标普通数
   */
  private int warningIndexOrdinaryCount;


  /**
   * 预警总次数
   */
  private int warningTotalCount;


}
