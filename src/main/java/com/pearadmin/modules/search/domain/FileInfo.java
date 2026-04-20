/*
package com.pearadmin.modules.search.domain;

*/
/**
 * 创建日期：2024-11-26
 **//*



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "file")
//@Setting(settingPath = "/es-config/settings.json")
public class FileInfo {

    @Id
    private String fileId;

*/
/**
     * 文件名称*//*



    //@Field(type = FieldType.Text, analyzer = "ik_max_word")
    //@Field(type = FieldType.Text, analyzer = "custom_pyramid_analyzer" ,searchAnalyzer = "ik_smart")
    //@Field(type = FieldType.Text, analyzer = "smartcn")
    @Field(type = FieldType.Text, analyzer = "ik_smart")
    private String fileName;

*/
/**
     * 文件分类*//*



    @Field(type = FieldType.Keyword)
    private String fileCategory;

*/
/**
     * 文件内容*//*



    //@Field(type = FieldType.Text, analyzer = "ik_max_word")
    //@Field(type = FieldType.Text, analyzer = "custom_pyramid_analyzer" ,searchAnalyzer = "ik_smart")
    //@Field(type = FieldType.Text, analyzer = "smartcn")
    @Field(type = FieldType.Text, analyzer = "ik_smart")
    private String fileContent;

*/
/**
     * 文件存储路径*//*



    @Field(type = FieldType.Keyword, index = false)
    private String filePath;

*/
/**
     * 文件大小*//*



    @Field(type = FieldType.Keyword, index = false)
    private String fileSize;

*/
/**
     * 文件类型*//*



    @Field(type = FieldType.Keyword, index = false)
    private String fileType;

*/
/**
     * 创建人*//*



    @Field(type = FieldType.Keyword, index = false)
    private String createBy;

*/
/**
     * 创建日期*//*



    @Field(type = FieldType.Keyword, index = false)
    private Date createTime;

*/
/**
     * 更新人*//*



    @Field(type = FieldType.Keyword, index = false)
    private String updateBy;

*/
/**
     * 更新日期*//*



    @Field(type = FieldType.Keyword, index = false)
    private Date updateTime;

*/
/**
     * 文件分类名称*//*



    @Field(type = FieldType.Text, analyzer = "ik_smart")
    private String fileClassifyName;

}
*/
