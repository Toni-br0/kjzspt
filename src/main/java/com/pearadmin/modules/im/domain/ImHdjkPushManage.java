package com.pearadmin.modules.im.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 * 创建日期：2025-04-25
 * IM活动监控推送管理
 **/

@Data
@Alias("ImHdjkPushManage")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImHdjkPushManage {
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
     * 图片路径
     */
    private String imgPath;

    /**
     * 是否推送图片 0否 1是
     */
    private String isPushImg;

    /**
     * 推送领导ID
     */
    private String pushLeaderId;

    /**
     * 推送对象周期  day天  week周 month月
     */
    private String pushObjectPeriod;

    /**
     * 发送对象的某个周
     */
    private String sendObjectWeek;

    /**
     * 发送对象的某一天
     */
    private String sendObjectDay;


    /**
     * 执行状态  1执行中  0未执行
     */
    private String execState;

    /**
     * 是否推送至领导  1是  0否
     */
    private String isPushLeader;

    /**
     * 发送对象时间点 时+分
     */
    private String sendObjectTime;


    /**
     * 周末正常推送  1是  0否
     */
    private String isWeekendPush;

    /**
     * 文件推送至领导时间
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private LocalDateTime filePushLeaderTime;

    /**
     * 推送领导名称
     */
    @TableField(exist = false)
    private String pushLeaderName;

    /**
     * 运行状态 1运行中  0已停止
     */
    private String runState;

    /**
     * 推送文件名称
     */
    @TableField(exist = false)
    private String sendFileName;

    /**
     * 模板类型 1日模板  2月模板
     */
    @TableField(exist = false)
    private String modelType;


}
