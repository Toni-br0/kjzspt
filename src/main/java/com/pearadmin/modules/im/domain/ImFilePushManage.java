package com.pearadmin.modules.im.domain;

import com.aspose.slides.internal.oe.ame;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * 创建日期：2025-04-25
 * IM文件推送管理
 **/

@Data
@Alias("ImFilePushManage")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImFilePushManage {
    /**
     * 管理ID
     */
    @TableId(value = "manage_id", type = IdType.AUTO)
    private Integer manageId;

    /**
     * 模板ID
     */
    private Integer modelId;

    /**
     * 模板名称
     */
    private String modelName;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 推送对象管理ID
     */
    private String pushObjectId;

    /**
     * 推送对象名称
     */
    @TableField(exist = false)
    private String pushObjectName;

    /**
     * 文件生成时间
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private LocalDateTime fileCreatTime;

    /**
     * 文件推送时间
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private LocalDateTime filePushTime;


    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 是否自动推送  0 否  1是
     */
    private String isAutoPush;

    /**
     * 推送对象领导ID
     */
    private String pushObjectLeaderId;

    /**
     * 推送对象领导名称
     */
    @TableField(exist = false)
    private String pushObjectLeaderName;

    /**
     * 推送文字信息内容
     */
    private String pustTxtMsg;

    /**
     * 推送文字信息内容
     */
    private String isPushTxtMsg;

}
