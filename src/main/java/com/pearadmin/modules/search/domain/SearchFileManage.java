/*
package com.pearadmin.modules.search.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

*/
/**
 * 创建日期：2024-12-06
 * 智能搜索文件管理
 **//*

@Data
@Alias("searchFileManage")
public class SearchFileManage {
    */
/**
     * 文件ID
     *//*

    @TableId(value = "file_id")
    private String fileId;

    */
/**
     * 文件名称
     *//*

    private String fileName;

    */
/**
     * 文件路径
     *//*

    private String filePath;

    */
/**
     * 文件大小 KB
     *//*

    private String fileSize;

    */
/**
     * 文件类型
     *//*

    private String fileType;

    */
/**
     * 创建时间
     *//*

    private LocalDateTime createTime;

    */
/**
     * 创建人ID
     *//*

    private String createUserId;

    */
/**
     * 创建人名称
     *//*

    private String createUserName;

    */
/**
     * 文件分类编码
     *//*

    private String fileClassifyCode;

    */
/**
     * 文件分类名称
     *//*

    private String fileClassifyName;

}
*/
