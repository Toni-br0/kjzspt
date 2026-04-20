package com.pearadmin.modules.report.service;

import com.alibaba.fastjson.JSONObject;
import com.pearadmin.common.aop.annotation.Log;
import com.pearadmin.common.aop.annotation.Repeat;
import com.pearadmin.common.aop.enums.BusinessType;
import com.pearadmin.modules.report.domain.ReportDimension;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 创建日期：2025-11-07
 * 自助取数维度管理
 **/

public interface RepIndexDimensionService {

    /**
     * 获取维度树数据
     * @return
     */
    public Object dimensionTreeload(String isNonStand);

    /**
     * 保存自助取数维度管理数据
     * @param reportDimension
     * @return
     */
    public JSONObject saveDimensionInfo(ReportDimension reportDimension);

    /**
     * 删除自助取数维度数据
     * Param: id
     * Return: 文件
     */
    public JSONObject remove(String  dimensionId);

    /**
     * 根据ID获取数据信息
     * @param dimensionId
     * @return
     */
    public ReportDimension getById(String dimensionId);

    /**
     * 根据ID获取父节点数据信息
     * @param dimensionId
     * @return
     */
    public ReportDimension getParentById(String dimensionId);


    /**
     * 保存修改自助取数维度管理数据
     * @param reportDimension
     * @return
     */
    public JSONObject updateDimensionInfo(ReportDimension reportDimension);
}
