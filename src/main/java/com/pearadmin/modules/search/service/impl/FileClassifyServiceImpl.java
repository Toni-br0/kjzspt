/*
package com.pearadmin.modules.search.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.tools.SequenceUtil;
import com.pearadmin.modules.search.domain.SearchFileClassify;
import com.pearadmin.modules.search.domain.SearchFileManage;
import com.pearadmin.modules.search.mapper.SearchFileClassifyMapper;
import com.pearadmin.modules.search.mapper.SearchFileManageMapper;
import com.pearadmin.modules.search.service.IFileClassifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

*/
/**
 * 创建日期：2025-01-15
 * 文件分类
 **//*


@Slf4j
@Service
public class FileClassifyServiceImpl implements IFileClassifyService {

    @Resource
    private SearchFileClassifyMapper searchFileClassifyMapper;
    @Autowired
    private SearchFileManageMapper searchFileManageMapper;

    */
/**
     * 文件分类列表
     * @return
     *//*

    @Override
    public List<SearchFileClassify> list() {
        List<SearchFileClassify> list = searchFileClassifyMapper.selectList(null);
        return list;
    }

    */
/**
     * 文件分类列表
     * @return
     *//*

    @Override
    public List<SearchFileClassify> queryList() {
        List<SearchFileClassify> list = searchFileClassifyMapper.selectList(null);
        SearchFileClassify oldSearchFileClassify = new SearchFileClassify();
        oldSearchFileClassify.setClassifyId("1");
        oldSearchFileClassify.setParentId("0");
        oldSearchFileClassify.setClassifyCode("sjfwywfl");
        oldSearchFileClassify.setClassifyName("数据服务业务分类");

        SearchFileClassify newSearchFileClassify = new SearchFileClassify();
        newSearchFileClassify.setClassifyId("1");
        newSearchFileClassify.setParentId("0");
        newSearchFileClassify.setClassifyCode("sjfwywfl");
        newSearchFileClassify.setClassifyName("全部");

        Collections.replaceAll(list, oldSearchFileClassify, newSearchFileClassify);
        return list;
    }

    */
/**
     * 保存文件分类信息
     * @param searchFileClassify
     * @return
     *//*

    @Override
    public int save(SearchFileClassify searchFileClassify) {
        int result = 0;

        QueryWrapper<SearchFileClassify> codeWrapper = new QueryWrapper<>();
        codeWrapper.eq("classify_code", searchFileClassify.getClassifyCode());
        codeWrapper.last("limit 1");
        SearchFileClassify codeSearchFileClassify = searchFileClassifyMapper.selectOne(codeWrapper);
        if(codeSearchFileClassify != null){
            result = -1;
            return result;
        }

        QueryWrapper<SearchFileClassify> nameWrapper = new QueryWrapper<>();
        nameWrapper.eq("classify_name", searchFileClassify.getClassifyName());
        nameWrapper.last("limit 1");
        SearchFileClassify nameSearchFileClassify = searchFileClassifyMapper.selectOne(nameWrapper);
        if(nameSearchFileClassify != null){
            result = -2;
            return result;
        }

        if (null == searchFileClassify.getParentId()) {
            searchFileClassify.setParentId("0");
        }

        searchFileClassify.setClassifyId(SequenceUtil.makeStringId());

        int addResult = searchFileClassifyMapper.insert(searchFileClassify);
        if(addResult >0){
            result = 0;
        }else{
            result = -3;
        }

        return result;
    }

    */
/**
     * 查询文件分类下的子分类
     * @param classifyId
     * @return
     *//*

    @Override
    public List<SearchFileClassify> selectByParentId(String classifyId) {
        LambdaQueryWrapper<SearchFileClassify> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SearchFileClassify::getParentId,classifyId);
        return searchFileClassifyMapper.selectList(wrapper);
    }

    */
/**
     * 文件分类删除
     * @param classifyId
     * @return
     *//*

    @Override
    public int remove(String classifyId) {
        int result = 0;
        QueryWrapper<SearchFileClassify> wrapper = new QueryWrapper<>();
        wrapper.eq("classify_id", classifyId);
        SearchFileClassify searchFileClassify = searchFileClassifyMapper.selectOne(wrapper);
        if(searchFileClassify != null){
            String classifyName = searchFileClassify.getClassifyName();
            QueryWrapper<SearchFileManage> manageWrapper = new QueryWrapper<>();
            manageWrapper.eq("file_classify_name", classifyName);
            manageWrapper.last("limit 1");
            SearchFileManage searchFileManages = searchFileManageMapper.selectOne(manageWrapper);
            if(searchFileManages != null){
                return -2;
            }

            int addResult = searchFileClassifyMapper.deleteById(classifyId);
            if(addResult ==0){
                return -1;
            }
            return result;
        }else{
            return -1;
        }

    }
}
*/
