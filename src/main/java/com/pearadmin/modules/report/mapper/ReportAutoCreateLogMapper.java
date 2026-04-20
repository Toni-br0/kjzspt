package com.pearadmin.modules.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pearadmin.modules.report.domain.ReportAutoCreateInfo;
import com.pearadmin.modules.report.domain.ReportAutoCreateLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface ReportAutoCreateLogMapper extends BaseMapper<ReportAutoCreateLog> {

    /**
     * 根据查询条件查询任务日志
     * @param reportAutoCreateLog
     * @param loginUserId
     * @return
     */
    public List<ReportAutoCreateLog> getListByWhere(@Param("reportAutoCreateLog") ReportAutoCreateLog reportAutoCreateLog, @Param("loginUserId") String loginUserId);

}
