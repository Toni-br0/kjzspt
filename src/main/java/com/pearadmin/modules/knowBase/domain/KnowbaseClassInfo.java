package com.pearadmin.modules.knowBase.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 * 创建日期：2025-06-25
 * 类别信息表
 **/

@Data
@Alias("KnowbaseClassInfo")
@JsonIgnoreProperties(ignoreUnknown = true)
public class KnowbaseClassInfo {
    /**
     * 草稿ID
     */
    @TableId(value = "class_id", type = IdType.AUTO)
    private Integer classId;

    /**
     * 类别编码
     */
    private String classCode;

    /**
     * 类别名称
     */
    private String className;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 创建人ID
     */
    private String createUserId;

    /**
     * 创建人名称
     */
    private String createUserName;

    /**
     * 排序ID
     */
    private Integer sortId;

}
