package com.pearadmin.modules.report.domain;


import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Alias("ReportData")
public class ReportData {

    @TableId(value = "data_id", type = IdType.AUTO)
    private String dataId;

    private String monthId;
    private String custId;
    private String lx;
    private String hxLatnId;
    private String hxLatnName;
    private String hxAreaId;
    private String hxAreaName;
    private String hxRegionId;
    private String hxRegionName;
    private String xHx5BpId;
    private String xHx5BpName;
    private String ydcp;
    private String kdcp;
    private String itvcp;
    private String zjcp;
    private String isQf;
    private String isHy;
    private String isFiveg;
    private String isFivegb;
    private String isFivegtc;
    private String isQzxsp;
    private String isQzsl;
    private String isSfllb;
    private String isQzxq;
    private String dimension;
    private String quota;


    private transient String strWhere;
}
