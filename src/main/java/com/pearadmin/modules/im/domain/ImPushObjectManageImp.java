package com.pearadmin.modules.im.domain;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 * 创建日期：2025-04-27
 * IM推送对象管理
 **/

@Data
public class ImPushObjectManageImp {

    /**
     * 所属区域
     */
    @ExcelProperty("所属区域")
    private String objectArea;

    /**
     * 推送对象类型
     */
    @ExcelProperty("推送对象类型")
    private String pushObjectType;

    /**
     * 推送对象ID
     */
    @ExcelProperty("推送对象ID")
    private String pushObjectId;

    /**
     * 推送对象名称
     */
    @ExcelProperty("推送对象名称")
    private String pushObjectName;


}
