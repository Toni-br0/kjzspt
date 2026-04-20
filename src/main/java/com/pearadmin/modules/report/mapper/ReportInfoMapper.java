package com.pearadmin.modules.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pearadmin.modules.report.domain.ReportInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportInfoMapper extends BaseMapper<ReportInfo> {

    public List<ReportInfo> selReportInfoList(@Param("reportInfo") ReportInfo reportInfo,@Param("isAdmin") boolean isAdmin,@Param("userId") String userId);
}
