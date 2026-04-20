package com.pearadmin.modules.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pearadmin.modules.report.domain.ReportData;
import com.pearadmin.modules.report.domain.RetReportData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReportDataMapper  extends BaseMapper<ReportData> {

    /**
     * 根据SQL语句查询报表数据
     * @param reportSql
     * @return
     */
    //@Select({"<script> ${reportSql} </script>"})
    public List<RetReportData> selReportDataList(@Param("reportSql") String reportSql);

    /**
     * 根据SQL语句查询报表数据
     * @param reportSql
     * @return
     */
    //@Select({"<script> ${reportSql} </script>"})
    public List<Map<String, Object>> selReportDataListMap(@Param("reportSql") String reportSql);
}

