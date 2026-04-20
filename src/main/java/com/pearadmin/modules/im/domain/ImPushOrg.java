package com.pearadmin.modules.im.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 *
 * 创建日期：2026-02-03
 * IM 推送组织树
 **/

@Data
@Alias("ImPushOrg")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImPushOrg {

    /**
     * 组织ID
     */
    @TableId
    private String orgId;

    /**
     * 组织名称
     */
    private String orgName;

    /**
     * 父节点
     */
    private String parentOrgId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 组织层级
     */
    private String level;

    /**
     * 排序ID
     */
    private Integer sortId;

    /**
     * 分类类型  bqfz标签分组
     */
    private String classType;

}
