package com.pearadmin.modules.report.service;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.pearadmin.common.aop.annotation.Log;
import com.pearadmin.common.aop.enums.BusinessType;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.common.web.domain.response.Result;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.modules.report.domain.ReportIndex;
import com.pearadmin.modules.sys.domain.SysDict;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 创建日期：2025-07-03
 * 指标管理
 **/

public interface RepIndexManageService {

    /**
     * Describe: 自助取数指标列表数据
     * Param SysDept PageDomain
     * Return 自助取数指标列表数据
     */
    public List<ReportIndex> getIndexList(ReportIndex reportIndex);

    /**
     * Describe: 自助取数指标列表数据
     * Param SysDept PageDomain
     * Return 自助取数指标列表数据
     */
    public List<ReportIndex> getSubIndexList(ReportIndex reportIndex);


    /**
     * Describe: 自助取数指标列表数据
     * Param SysDept PageDomain
     * Return 自助取数指标列表数据
     */
    public PageInfo<ReportIndex> getIndexListNew(ReportIndex reportIndex, PageDomain pageDomain);

    /**
     * Describe: 自助取数指标列表子数据
     * Param SysDept PageDomain
     * Return 自助取数指标列表数据
     */
    public PageInfo<ReportIndex> getIndexSubList(ReportIndex reportIndex, PageDomain pageDomain);

    /**
     * Describe: 保存指标信息
     * Param SysDept
     * Return 执行结果
     */

    public JSONObject saveIndex(ReportIndex reportIndex);

    /**
     * 根据id获取子指标
     * @param indexId
     * @return
     */
    public List<ReportIndex> selectByParentId(int indexId);


    /**
     * 根据ID删除指标
     * @param indexId
     * @return
     */
    public JSONObject remove(int indexId);

    /**
     * 根据ID获取指标
     * @param indexId
     * @return
     */
    public ReportIndex getById(int indexId);


    /**
     * Describe: 修改部门信息
     * Param reportIndex
     * Return 执行结果
     */
    public JSONObject updateIndex(ReportIndex reportIndex);


    /**
     * Describe: 根据ID获取指标类型和分类
     * Param: id
     * Return: Result
     */
    public JSONObject getTypeClass(int indexId);

}
