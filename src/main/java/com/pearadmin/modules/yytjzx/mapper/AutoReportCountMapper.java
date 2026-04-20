package com.pearadmin.modules.yytjzx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pearadmin.modules.xfppt.domain.KhjyPptChart;
import com.pearadmin.modules.yytjzx.domain.AutoReportCount;
import com.pearadmin.modules.yytjzx.domain.AutoReportIndexParam;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 创建日期：2025-09-22
 * 县分PPT图表底表
 **/

@Mapper
public interface AutoReportCountMapper extends BaseMapper<AutoReportCount> {

    /**
     * 执行SQL查询自助取数信息
     * @param sql
     * @return
     */
    public List<AutoReportCount> getAutoReportCountList(@Param("sql") String sql);

 
    public List<AutoReportIndexParam> getAutoReportCountMap(@Param("sql") String sql);

}
