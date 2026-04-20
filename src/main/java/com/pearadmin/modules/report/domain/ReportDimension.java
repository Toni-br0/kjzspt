package com.pearadmin.modules.report.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

@Data
@Alias("ReportDimension")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportDimension {

    /**
     * 维度ID
     */
    @TableId(value = "dimension_id", type = IdType.AUTO)
    private String dimensionId;

    /**
     * 父级id
     */
    private String parentId;

    /**
     * 维度名称
     */
    private String dimensionName;

    /**
     * 区域  地市 hx_latn_name  区县 hx_area_name  网络 x_hx5_bp_name
     */
    private String field;

    /**
     * state
     */
    private String state;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 层级
     */
    private String level;


    /**
     * 排序ID
     */
    private int sortId;

    /**
     * 是否非标准  1非标准   0或空为标准
     */
    private String isNonStand;


}
