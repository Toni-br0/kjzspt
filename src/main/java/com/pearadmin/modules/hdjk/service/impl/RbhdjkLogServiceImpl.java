package com.pearadmin.modules.hdjk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.hdjk.domain.HdjkLog;
import com.pearadmin.modules.hdjk.mapper.HdjkLogMapper;
import com.pearadmin.modules.hdjk.service.HdjkLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 创建日期：2025-05-26
 * 日报活动监日志
 **/

@Slf4j
@Service
public class RbhdjkLogServiceImpl implements HdjkLogService {

    @Resource
    private HdjkLogMapper rbhdjkLogMapper;

    /**
     * 获取日报活动监控日志列表数据
     * @param rbhdjkLog
     * @return
     */
    @Override
    public List<HdjkLog> logDataList(HdjkLog rbhdjkLog) {
        QueryWrapper<HdjkLog> queryWrapper = new QueryWrapper<>();
        if(StringUtil.isNotEmpty(rbhdjkLog.getModelName())){
            queryWrapper.like("model_name",rbhdjkLog.getModelName());
        }
        queryWrapper.orderByDesc("log_time");

        List<HdjkLog> list = rbhdjkLogMapper.selectList(queryWrapper);
        return list;
    }
}
