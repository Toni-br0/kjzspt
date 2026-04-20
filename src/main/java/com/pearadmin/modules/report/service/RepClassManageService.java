package com.pearadmin.modules.report.service;

import com.alibaba.fastjson.JSONObject;
import com.pearadmin.modules.report.domain.ReportClassify;
import java.util.List;

/**
 * 创建日期：2025-07-01
 * 分类管理
 **/

public interface RepClassManageService {

    /**
     * 获取分类管理列表数据
     * @param reportClassify
     * @return
     */
    public List<ReportClassify> getClassList(ReportClassify reportClassify);


    /**
     * 保存分类数据
     * @param reportClassify
     * @return
     */
    public JSONObject saveClassInfo(ReportClassify reportClassify);

    /**
     * 通过分类编码获取分类信息
     */
    public ReportClassify getClassById(String classifyId);

    /**
     * 单个删除分类数据
     * Param: id
     * Return: 文件
     */
    public JSONObject remove(int classifyId);

    /**
     * 批量删除分类数据
     * @param classifyIds
     * @return
     */
    public JSONObject batchRemove(String classifyIds);

    /**
     * 获取分类下拉框数据
     * @return
     */
    public Object getClassSelect();

    /**
     * 获取分类管理列表数据
     * @return
     */
    public List<ReportClassify> getClassList();

}
