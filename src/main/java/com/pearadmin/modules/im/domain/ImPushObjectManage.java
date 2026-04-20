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
 * 创建日期：2025-04-27
 * IM推送对象管理
 **/

@Data
@Alias("ImPushObjectManage")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImPushObjectManage {

    /**
     * 管理ID
     */
    @TableId(value = "manage_id", type = IdType.AUTO)
    private Integer manageId;

    /**
     * 推送对象类型 users 用户 group 群
     */
    private String pushObjectType;

    /**
     * 推送对象ID
     */
    private String pushObjectId;

    /**
     * 推送对象名称
     */
    private String pushObjectName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 绑定时间
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private LocalDateTime bindTime;

    /**
     * 对象所属区域
     */
    private String objectArea;

    /**
     * 对象所属区域ID
     */
    private String objectAreaId;


    /**
     * 分类类型  zzwd组织维度  bqwd标签维度
     */
    private String classType;


    /**
     * 父区域ID
     */
    private String parentAreaId;

    /**
     * 创建人ID
     */
    private String createUserId;

    /**
     * 绑定状态  1已绑定  0未绑定
     */
    private String bindState;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建用户名称
     */
    @TableField(exist = false)
    private String createUserName;

    /**
     * 数据级别
     */
    @TableField(exist = false)
    private String dataLevel;


}
