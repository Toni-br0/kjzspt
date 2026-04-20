package com.pearadmin.modules.report.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.modules.report.domain.ReportParam;
import com.pearadmin.modules.report.domain.RetReportData;
import com.pearadmin.modules.report.mapper.ReportDataMapper;
import com.pearadmin.modules.report.service.RepConnSecondTableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 创建日期：2025-07-14
 **/

@Slf4j
@Service
public class RepConnSecondTableServiceImpl implements RepConnSecondTableService {

    @Resource
    private ReportDataMapper reportDataMapper;


    /**
     * 获取报表数据列表
     * @param sqlStr
     * @return
     */
    @Override
    @DS("second")
    public List<RetReportData> getReportDataList(String sqlStr) {
        List<RetReportData> list = reportDataMapper.selReportDataList(sqlStr);
        return list;
    }

    /**
     * 获取报表数据列表 Map
     * @param sqlStr
     * @return
     */
    @Override
    @DS("second")
    public List<Map<String, Object>> getReportDataListMap(String sqlStr) {
        List<Map<String, Object>> list = reportDataMapper.selReportDataListMap(sqlStr);
        return list;
    }
}
