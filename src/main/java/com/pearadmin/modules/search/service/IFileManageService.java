/*
package com.pearadmin.modules.search.service;

import com.alibaba.fastjson.JSONObject;
import com.pearadmin.modules.search.domain.SearchFileManage;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

*/
/**
 * 创建日期：2024-12-06
 * 智能搜索文件管理
 **//*


public interface IFileManageService {

    */
/**
     * 获取智能搜索文件管理列表数据
     * @param searchFileManage
     * @return
     *//*

    List<SearchFileManage> fileDataList(SearchFileManage searchFileManage);

    */
/**
     * 智能搜索文件管理文件上传
     * @param file
     * @return
     *//*

    JSONObject uploadFile(MultipartFile file,String fileClassifyCode,String fileClassifyName);

    */
/**
     *  智能搜索文件管理文件删除
     * @param fileId
     * @return
     *//*

    boolean fileRemove(String fileId);

    */
/**
     * 文件下载
     * @param fileId
     * @return
     * @throws Exception
     *//*

    ResponseEntity<InputStreamResource> downloadFile(String fileId) throws Exception;

    */
/**
     * 获取文件分类树
     * @return
     *//*

    public Object treeload();
}
*/
