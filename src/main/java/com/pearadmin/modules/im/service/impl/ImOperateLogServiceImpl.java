package com.pearadmin.modules.im.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.im.domain.ImOperateLog;
import com.pearadmin.modules.im.mapper.ImOperateLogMapper;
import com.pearadmin.modules.im.service.IImOperateLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 创建日期：2025-04-30
 * IM操作日志
 **/

@Slf4j
@Service
public class ImOperateLogServiceImpl implements IImOperateLogService {

    @Resource
    private ImOperateLogMapper imOperateLogMapper;

    /**
     * 获取IM操作日志列表数据
     * @param imOperateLog
     * @return
     */
    @Override
    public List<ImOperateLog> operateLogList(ImOperateLog imOperateLog) {
        QueryWrapper<ImOperateLog> queryWrapper = new QueryWrapper<>();

        if(StringUtil.isNotEmpty(imOperateLog.getFileName())){
            queryWrapper.like("file_name", imOperateLog.getFileName());
        }
        if(StringUtil.isNotEmpty(imOperateLog.getObjectName())){
            queryWrapper.like("object_name", imOperateLog.getObjectName());
        }
        if(StringUtil.isNotEmpty(imOperateLog.getOperateType())){
            queryWrapper.eq("operate_type", imOperateLog.getOperateType());
        }

        if(StringUtil.isNotEmpty(imOperateLog.getQueryOperateTime())){
            queryWrapper.apply("DATE_FORMAT(operate_time, '%Y-%m-%d') = {0}", imOperateLog.getQueryOperateTime());
        }

        queryWrapper.orderByDesc("operate_time");

        List<ImOperateLog> list = imOperateLogMapper.selectList(queryWrapper);

        return list;
    }
}
