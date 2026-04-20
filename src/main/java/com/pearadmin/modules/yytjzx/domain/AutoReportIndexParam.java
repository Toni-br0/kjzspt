package com.pearadmin.modules.yytjzx.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

/**
 * 创建日期：2025-10-30
 * 自助取数指标查询参数类
 **/

@Data
@Alias("AutoReportIndexParam")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutoReportIndexParam {

    /**
     * 分类Id 分个用;拼接
     */
    private String classifyIds;

    /**
     * 分类编码
     */
    private String classifyCode;

    /**
     * 分类名称
     */
    private String classifyName;


    /**
     * 指标Id 分个用;拼接
     */
    private String indexIds;

    /**
     * 指标名称
     */
    private String indexName;


    /**
     * 指标统计结果
     */
    private int indexCount;


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

    /**
     * 区域名称
     */
    private String areaName;







}
