package com.pearadmin.modules.report.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 * 创建日期：2025-07-02
 * 自助报表条件配置表
 **/

@Data
@Alias("ReportCondit")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportCondit {

    /**
     * 条件ID
     */
    @TableId(value = "condit_id", type = IdType.AUTO)
    private Integer conditId;

    /**
     * 条件编码
     */
    private String conditCode;

    /**
     * 条件名称
     */
    private String conditName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 排序ID
     */
    private Integer sortId;

    /**
     * 创建人ID
     */
    private String createUserId;

    /**
     * 指标ID
     */
    private Integer indexId;

    /**
     * 条件值1
     */
    private String conditValue1;

    /**
     * 条件值2
     */
    private String conditValue2;

    /**
     * 条件值3
     */
    private String conditValue3;


    /**
     * 条件值4
     */
    private String conditValue4;


    /**
     * 条件值5
     */
    private String conditValue5;

    /**
     * 条件值6
     */
    private String conditValue6;

    /**
     * 条件值7
     */
    private String conditValue7;

    /**
     * 条件值8
     */
    private String conditValue8;

    /**
     * 条件值9
     */
    private String conditValue9;

    /**
     * 条件值10
     */
    private String conditValue10;

    /**
     * 条件值11
     */
    private String conditValue11;

    /**
     * 条件值12
     */
    private String conditValue12;

    /**
     * 条件值13
     */
    private String conditValue13;

    /**
     * 条件值14
     */
    private String conditValue14;

    /**
     * 条件值15
     */
    private String conditValue15;

    /**
     * 创建人姓名
     */
    @TableField(exist = false)
    private String realName;

    /**
     * 指标名称
     */
    @TableField(exist = false)
    private String indexName;


}
