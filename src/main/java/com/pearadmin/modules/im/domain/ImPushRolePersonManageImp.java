package com.pearadmin.modules.im.domain;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 *
 * 创建日期：2026-04-03
 * IM推送角色人员管理
 **/

@Data
public class ImPushRolePersonManageImp {

    @ExcelProperty("推送对象ID")
    private String personId;

    @ExcelProperty("推送对象名称")
    private String personName;

    @ExcelProperty("本地网ID")
    private String hxLatnId;

    @ExcelProperty("本地网名称")
    private String hxLatnName;

    @ExcelProperty("县分ID")
    private String hxAreaId;

    @ExcelProperty("县分名称")
    private String hxAreaName;

    @ExcelProperty("支局ID")
    private String hxRegionId;

    @ExcelProperty("支局名称")
    private String hxRegionName;

    @ExcelProperty("网格ID")
    private String xHx5BpId;

    @ExcelProperty("网格名称")
    private String xHx5BpName;

}
