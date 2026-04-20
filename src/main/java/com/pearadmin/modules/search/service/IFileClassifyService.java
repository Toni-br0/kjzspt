/*
package com.pearadmin.modules.search.service;

import com.pearadmin.modules.search.domain.SearchFileClassify;
import java.util.List;

*/
/**
 * 创建日期：2025-01-15
 * 文件分类
 **//*


public interface IFileClassifyService {

  */
/**
   * 文件分类列表
   * @return
   *//*

  List<SearchFileClassify> list();


  */
/**
   * 文件分类列表
   * @return
   *//*

  List<SearchFileClassify> queryList();

  */
/**
   * 保存文件分类信息
   * @param searchFileClassify
   * @return
   *//*

  int save(SearchFileClassify searchFileClassify);

  */
/**
   * 查询文件分类下的子分类
   * @param classifyId
   * @return
   *//*

  List<SearchFileClassify> selectByParentId(String classifyId);

  */
/**
   * 文件分类删除
   * @param classifyId
   * @return
   *//*

  int remove(String classifyId);

}
*/
