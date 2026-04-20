package com.pearadmin.modules.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pearadmin.modules.report.domain.ReportAutoCreateInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportAutoCreateInfoMapper extends BaseMapper<ReportAutoCreateInfo> {

    /**
     * 根据开始日期和结束日期查询报表自动创建信息列表
     * @param startTime
     * @param endTime
     * @return
     */
    public List<ReportAutoCreateInfo> getListByTime(@Param("startTime") String startTime, @Param("endTime") String endTime);

    /**
     * 根据查询条件查询报表自动创建信息列表
     * @param reportAutoCreateInfo
     * @param loginUserId
     * @return
     */
    public List<ReportAutoCreateInfo> getListByWhere(@Param("reportAutoCreateInfo") ReportAutoCreateInfo reportAutoCreateInfo,@Param("loginUserId") String loginUserId);

}
