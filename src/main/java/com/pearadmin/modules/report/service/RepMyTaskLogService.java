package com.pearadmin.modules.report.service;

import com.pearadmin.modules.report.domain.ReportAutoCreateLog;

import java.util.List;

/**
 * 创建日期：2025-08-06
 * 自助取数任务日志
 **/

public interface RepMyTaskLogService {

    /**
     * 获取自助取数我的任务日志列表数据
     * @param reportAutoCreateLog
     * @return
     */
    public List<ReportAutoCreateLog> getMyTaskLogList(ReportAutoCreateLog reportAutoCreateLog);
}
