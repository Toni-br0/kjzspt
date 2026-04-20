/*
package com.pearadmin.modules.search.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

*/
/**
 * 创建日期：2024-12-16
 * 文件搜索文件分类实体类
 **//*


@Data
@Alias("searchFileClassify")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchFileClassify {
    */
/**
     * 分类ID
     *//*

    @TableId(value = "classify_id")
    private String classifyId;

    */
/**
     * 父分类ID
     *//*

    private String parentId;

    */
/**
     * 分类编码
     *//*

    private String classifyCode;

    */
/**
     * 分类名称
     *//*

    private String classifyName;

}
*/
