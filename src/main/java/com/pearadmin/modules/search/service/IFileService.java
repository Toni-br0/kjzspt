/*
package com.pearadmin.modules.search.service;


*/
/**
 * 创建日期：2024-11-26
 * description: ES文件服务
 **//*




import com.pearadmin.modules.search.domain.FileInfo;
import com.pearadmin.modules.search.domain.FileDTO;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface IFileService {


*/
/**
     * 保存文件*//*

    void saveFile(MultipartFile file) throws Exception;


*/
/**
     * 关键字查询
     *
     * @return*//*


    List<SearchHit<FileInfo>> search(FileDTO dto);


*/
/**
     * 关键字查询
     *
     * @return*//*




    SearchHits<FileInfo> searchPage(FileDTO dto);

*/
/**
     * 根据ID删除
     * @param id
     * @return*//*



    String deleteById(String id);
}

*/
