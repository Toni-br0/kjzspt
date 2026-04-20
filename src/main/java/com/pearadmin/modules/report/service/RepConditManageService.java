package com.pearadmin.modules.report.service;

import com.alibaba.fastjson.JSONObject;
import com.pearadmin.common.aop.annotation.Log;
import com.pearadmin.common.aop.annotation.Repeat;
import com.pearadmin.common.aop.enums.BusinessType;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.modules.report.domain.ReportClassify;
import com.pearadmin.modules.report.domain.ReportCondit;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 创建日期：2025-07-02
 * 自助报表条件管理
 **/

public interface RepConditManageService {

    /**
     * 获取条件管理列表数据
     * @param reportCondit
     * @return
     */
    public List<ReportCondit> getConditList(ReportCondit reportCondit);

    /**
     * 保存条件数据
     * @param reportCondit
     * @return
     */
    public JSONObject saveConditInfo(ReportCondit reportCondit);


    /**
     * 根据条件ID查询条件数据
      * @param conditId
     * @return
     */
    public ReportCondit getConditById(int conditId);

    /**
     * 单个删除条件数据
     * Param: id
     * Return: 文件
     */
    public JSONObject remove(int conditId);

    /**
     * 批量删除条件数据
     * @param conditIds
     * @return
     */
    public JSONObject batchRemove(String conditIds);

}
