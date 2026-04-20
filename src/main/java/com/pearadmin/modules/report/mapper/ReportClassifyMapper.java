package com.pearadmin.modules.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.modules.report.domain.ReportClassify;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportClassifyMapper extends BaseMapper<ReportClassify> {

    /**
     * 查询自助报表分类管理数据
     * @param reportClassify
     * @return
     */
    public List<ReportClassify> selClassList(@Param("reportClassify") ReportClassify reportClassify);

}
