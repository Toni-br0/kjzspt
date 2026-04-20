package com.pearadmin.modules.hdjk.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 * 创建日期：2025-05-22
 * 活动监日志表
 **/

@Data
@Alias("HdjkLog")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HdjkLog {

    /**
     * 日志ID
     */
    @TableId(value = "log_id", type = IdType.AUTO)
    private Integer logId;

    /**
     * 模板名称
     */
    private String modelName;

    /**
     * 日志类型
     */
    private String logType;

    /**
     * 日志时间
     */
    private LocalDateTime logTime;


    /**
     * 模板源路径（上传路径）
     */
    private String modelSourcePath;


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
     * 是否成功 0成功 -1失败
     */
    private String isSuccess;

    /**
     * 操作信息
     */
    private String logMsg;

    /**
     * 模板类型  1日模板
     */
    private String modelType;

}
