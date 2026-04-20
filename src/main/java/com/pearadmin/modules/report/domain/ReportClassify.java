package com.pearadmin.modules.report.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 * 分类
 */


@Data
@Alias("ReportClassify")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportClassify {

    @TableId(value = "classify_id", type = IdType.AUTO)
    private Integer classifyId;

    /**
     * 分类编码
     */
    private String classifyCode;


    /**
     * 分类名称
     */
    private String classifyName;


    /**
     * 创建人
     */
    private String createUserId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 排序ID
     */
    private int sortId;

    /**
     * 创建人姓名
     */
    @TableField(exist = false)
    private String realName;

}
