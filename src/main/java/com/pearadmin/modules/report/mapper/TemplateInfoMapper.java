package com.pearadmin.modules.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pearadmin.modules.report.domain.ReportTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TemplateInfoMapper extends BaseMapper<ReportTemplate> {
    public List<ReportTemplate> selTempInfoList(@Param("reportTemplate") ReportTemplate reportTemplate, @Param("isAdmin") boolean isAdmin, @Param("userId") String userId);
}
