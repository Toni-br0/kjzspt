package com.pearadmin.modules.report.domain;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 * 创建日期：2025-08-15
 * 自助取数指标口径查询表
 **/

@Data
@Alias("ReportIndexStateInfo")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportIndexStateInfo {

    @TableId(value = "info_id", type = IdType.AUTO)
    @ExcelIgnore
    private Integer infoId;

    /**
     * 指标名称
     */
    @ExcelProperty(value = "指标名称", index = 0)
    private String indexName;

    /**
     * 指标字段
     */
    @ExcelProperty(value = "指标字段", index = 1)
    private String indexField;

    /**
     * 分类名称
     */
    @ExcelProperty(value = "所属分类", index = 2)
    private String classifyName;

    /**
     * 所属宽表
     */
    @ExcelProperty(value = "所属宽表", index = 3)
    private String belongTable;

    /**
     * 业务口径说明
     */
    @ExcelProperty(value = "业务口径说明", index = 4)
    private String businessStatement;

    /**
     * 业务口径负责人
     */
    @ExcelProperty(value = "业务口径负责人", index = 5)
    private String businessCaliberLeader;

    /**
     * 数据口径
     */
    @ExcelProperty(value = "数据口径", index = 6)
    private String dataCaliber;

    /**
     * 涉及宽表
     */
    @ExcelProperty(value = "涉及宽表", index = 7)
    private String involvingTable;

    /**
     * 数据口径负责人
     */
    @ExcelProperty(value = "数据口径负责人", index = 8)
    private String dataCaliberLeader;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间", index = 9)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 所属分类
     */
    /*@TableField(exist = false)
    @ExcelProperty(value = "所属分类", index = 2)
    private String classifyName;*/

}
