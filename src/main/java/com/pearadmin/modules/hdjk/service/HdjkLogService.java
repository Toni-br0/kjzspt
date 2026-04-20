package com.pearadmin.modules.hdjk.service;

import com.pearadmin.modules.hdjk.domain.HdjkLog;

import java.util.List;

/**
 * 创建日期：2025-05-26
 * 日报活动监日志
 **/

public interface HdjkLogService {

    /**
     * 获取日报活动监控日志列表数据
     * @param rbhdjkLog
     * @return
     */
    public List<HdjkLog> logDataList(HdjkLog rbhdjkLog);

}
