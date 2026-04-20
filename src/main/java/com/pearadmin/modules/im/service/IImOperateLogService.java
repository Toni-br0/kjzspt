package com.pearadmin.modules.im.service;

import com.pearadmin.modules.im.domain.ImOperateLog;

import java.util.List;

/**
 * 创建日期：2025-04-30
 * 描述：IM操作日志
 **/

public interface IImOperateLogService {

    /**
     * 获取IM操作日志列表数据
     * @param imOperateLog
     * @return
     */
    List<ImOperateLog> operateLogList(ImOperateLog imOperateLog);

}
