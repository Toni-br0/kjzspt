package com.pearadmin.modules.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pearadmin.modules.report.domain.ReportAutoCreateLog;
import com.pearadmin.modules.report.domain.ReportIndexWarningLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 创建日期：2025-10-16
 * 指标预警日志表
 **/

@Mapper
public interface ReportIndexWarningLogMapper extends BaseMapper<ReportIndexWarningLog> {

    /**
     * 根据查询条件查询预警日志
     * @param reportIndexWarningLog
     * @param loginUserId
     * @return
     */
    public List<ReportIndexWarningLog> getListByWhere(@Param("reportIndexWarningLog") ReportIndexWarningLog reportIndexWarningLog, @Param("loginUserId") String loginUserId);
}
