package com.pearadmin.modules.report.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 创建日期：2025-07-08
 **/

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportParam {

    /**
     * 报表名称
     */
    private String reportName;

    /**
     * 数据周期
     */
    private String dateType;

    /**
     * 分析角色
     */
    private String roleType;

    /**
     * 指标类型
     */
    private String indexType;

    /**
     * 政企
     */
    private String custZq;

    /**
     * 公众
     */
    private String custGz;

    /**
     * 维度
     */
    private String dimensionInfo;

    /**
     * 指标
     */
    private String indexInfo;

    /**
     * 条件
     */
    private String whereInfo;

    /**
     * 发送周期
     */
    private String sendCycle;

    /**
     * 发送月份
     */
    private String sendMonth;

    /**
     * 发送时间
     */
    private String sendTime;

    /**
     * IM发送对象ID
     */
    private String pushObjectId;

    /**
     * 开始日期
     */
    private String startDate;

    /**
     * 结束日期
     */
    private String endDate;

    /**
     * 推送领导ID
     */
    private String pushLeaderId;

    /**
     * 客户类型
     */
    private String custType;

    /**
     * 是否非标准  1非标准   0或空为标准
     */
    private String isNonNtand;

    /**
     * 下一级维度
     */
    private String xyjwd;

    /**
     * 推送领导ID
     */
    private String addPushLeaderId;

}
