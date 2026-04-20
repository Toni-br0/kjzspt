package com.pearadmin.modules.im.domain;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 *
 * 创建日期：2026-04-03
 * IM推送角色管理
 **/

@Data
@Alias("ImPushRoleManage")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImPushRoleManage {


    /**
     * 角色ID
     */
    @TableId
    private String roleId;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色等级 fgs分公司 xf县分 zj支局 wg网格
     */
    private String roleLevel;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;


    /**
     * 是否生成下一级文件  1是 0否
     */
    private String nextLevelFile;


    @TableField(exist = false)
    private String parentId;

    @TableField(exist = false)
    private JSONObject basicData;

    @TableField(exist = false)
    private String checkArr = "0";
}
