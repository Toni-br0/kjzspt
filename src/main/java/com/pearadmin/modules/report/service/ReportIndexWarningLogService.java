package com.pearadmin.modules.report.service;

import com.pearadmin.modules.report.domain.ReportIndexWarningLog;

import java.util.List;

/**
 * 创建日期：2025-10-22
 * 自助取数指标预警日志
 **/

public interface ReportIndexWarningLogService {

    /**
     * 获取自助取数指标预警列表数据
     * @param reportIndexWarningLog
     * @return
     */
    public List<ReportIndexWarningLog> getIndexWarningLogList(ReportIndexWarningLog reportIndexWarningLog);
}
