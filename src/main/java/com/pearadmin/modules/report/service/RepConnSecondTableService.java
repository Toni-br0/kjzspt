package com.pearadmin.modules.report.service;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.modules.report.domain.ReportParam;
import com.pearadmin.modules.report.domain.RetReportData;

import java.util.List;
import java.util.Map;

/**
 * 创建日期：2025-07-14
 **/

public interface RepConnSecondTableService {

    /**
     * 获取报表数据列表
     * @param sqlStr
     * @return
     */
    public List<RetReportData> getReportDataList(String sqlStr);

    /**
     * 获取报表数据列表 Map
     * @param sqlStr
     * @return
     */
    public List<Map<String, Object>> getReportDataListMap(String sqlStr);

}
