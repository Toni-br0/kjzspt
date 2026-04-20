package com.pearadmin.modules.report.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;
/*
    报表信息
 */

@Data
@Alias("ReportInfo")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportInfo {
    @TableId(value = "report_id", type = IdType.AUTO)
    private Integer reportId;

    private String reportName;
    private String cols;
    private String fields;
    private String custZq;
    private String custGz;
    private String dateType;
    private String startDate;
    private String endDate;
    private String state;
    private String createBy;
    private LocalDateTime createTime;

    private String reportPath;
    private String quota;
    private String dimension;

    private String roleType;
    private String indexType;
    private String whereStr;
    private String whereCheck;
    private String reportClassify;

    /**
     * 客户类型
     */
    private String custType;

    /**
     * 是否非标准  1非标准   0或空为标准
     */
    private String isNonStand;

    /**
     * 下一级维度  0不选中  1选中
     */
    private String xyjwd;

    /**
     * 创建人名称
     */
    @TableField(exist = false)
    private String createUserName;

}
