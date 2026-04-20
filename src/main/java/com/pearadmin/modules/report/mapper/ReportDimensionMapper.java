package com.pearadmin.modules.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pearadmin.modules.report.domain.ReportData;
import com.pearadmin.modules.report.domain.ReportDimension;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReportDimensionMapper extends BaseMapper<ReportDimension> {
}
