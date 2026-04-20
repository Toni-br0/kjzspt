package com.pearadmin.modules.report.service;

import com.alibaba.fastjson.JSONObject;
import com.pearadmin.common.aop.annotation.Log;
import com.pearadmin.common.aop.annotation.Repeat;
import com.pearadmin.common.aop.enums.BusinessType;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.modules.report.domain.ReportClassify;
import com.pearadmin.modules.report.domain.ReportIndexStateInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 创建日期：2025-08-15
 * 自助取数指标口径查询
 **/

public interface ReportIndexStateInfoService {

    /**
     * 获取指标口径查询列表数据
     * @param reportIndexStateInfo
     * @return
     */
    public List<ReportIndexStateInfo> getIndexStateList(ReportIndexStateInfo reportIndexStateInfo);

    /**
     * 保存自助取数指标口径查询数据
     * @param reportIndexStateInfo
     * @return
     */
    public JSONObject saveIndexStateInfo(ReportIndexStateInfo reportIndexStateInfo);

    /**
     * 修改自助取数指标口径查询数据
     * @param reportIndexStateInfo
     * @return
     */
    public JSONObject updateIndexStateInfo(ReportIndexStateInfo reportIndexStateInfo);

    /**
     * 单个删除自助取数指标口径查询数据
     * Param: id
     * Return: 文件
     */
    public JSONObject remove(int infoId);


    /**
     * 批量删除自助取数指标口径查询数据
     * @param infoIds
     * @return
     */
    public JSONObject batchRemove(String infoIds);

    /**
     * 根据ID查询自助取数指标口径查询数据
     * @param infoId
     * @return
     */
    public ReportIndexStateInfo getIndexStateInfoById(int infoId);

    /**
     * 导出报表
     * @return
     */
    public JSONObject fileDownload(ReportIndexStateInfo reportIndexStateInfo);



}
