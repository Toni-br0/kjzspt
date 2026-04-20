package com.pearadmin.modules.im.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 *
 * 创建日期：2026-04-03
 * IM推送角色人员管理
 **/

@Data
@Alias("ImPushRolePersonManage")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImPushRolePersonManage {

    /**
     * 管理ID
     */
    @TableId
    private String manageId;

    /**
     * 角色ID
     */
    private String roleId;

    /**
     * 推送人ID
     */
    private String personId;

    /**
     * 推送人名称
     */
    private String personName;

    /**
     * 本地网ID
     */
    private String hxLatnId;

    /**
     * 本地网名称
     */
    private String hxLatnName;

    /**
     * 县分ID
     */
    private String hxAreaId;

    /**
     * 县分名称
     */
    private String hxAreaName;

    /**
     * 支局ID
     */
    private String hxRegionId;

    /**
     * 支局名称
     */
    private String hxRegionName;

    /**
     * 网格ID
     */
    @JsonProperty("xHx5BpId")
    private String xHx5BpId;

    /**
     * 网格名称
     */
    @JsonProperty("xHx5BpName")
    private String xHx5BpName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 角色名称
     */
    @TableField(exist = false)
    private String roleName;

    /**
     * 角色等级
     */
    @TableField(exist = false)
    private String roleLevel;

}
