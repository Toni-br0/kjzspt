package com.pearadmin.modules.hdjk.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 * 创建日期：2025-05-22
 * 活动监控模板管理表
 **/

@Data
@Alias("HdjkModelManage")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HdjkModelManage {

    /**
     * 模板ID
     */
    @TableId(value = "model_id", type = IdType.AUTO)
    private Integer modelId;

    /**
     * 模板名称
     */
    private String modelName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;


    /**
     * 模板源路径（上传路径）
     */
    private String modelSourcePath;

    /**
     * 模板目标径（生成路径）
     */
    private String modelTargetPath;

    /**
     * 创建人ID
     */
    private String createUserId;

    /**
     * 创建人名称
     */
    private String createUserName;

    /**
     * 模板所属区域
     */
    private String modelArea;

    /**
     * 模板大小 KB
     */
    private String fileSize;

    /**
     * 模板类型  1日模板  2月模板
     */
    private String modelType;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 文件生成时间
     */
    private LocalDateTime fileCreateTime;

    /**
     * 模板参数,;分隔
     */
    private String modelParam;

    /**
     * 背景颜色
     */
    private String backgroundColor;

    /**
     * 线条颜色
     */
    private String lineColor;

    /**
     * 是否生成图片  0否  1是
     */
    private String isCreateImg;

    /**
     * 推送文件名称
     */
    private String sendFileName;



}
