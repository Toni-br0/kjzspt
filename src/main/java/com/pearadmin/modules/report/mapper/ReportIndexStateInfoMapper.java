package com.pearadmin.modules.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pearadmin.modules.report.domain.ReportClassify;
import com.pearadmin.modules.report.domain.ReportIndexStateInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * 创建日期：2025-07-03
 * 自助取数指标口径查询表
 **/

@Mapper
public interface ReportIndexStateInfoMapper extends BaseMapper<ReportIndexStateInfo> {

    /**
     * 查询自助报表分类管理数据
     * @param reportIndexStateInfo
     * @return
     */
    public List<ReportIndexStateInfo> selIndexStateList(@Param("reportIndexStateInfo") ReportIndexStateInfo reportIndexStateInfo);

}
