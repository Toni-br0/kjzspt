package com.pearadmin.modules.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pearadmin.modules.report.domain.ReportClassify;
import com.pearadmin.modules.report.domain.ReportCondit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 创建日期：2025-07-02
 * 自助报表条件配置表
 **/

@Mapper
public interface ReportConditMapper extends BaseMapper<ReportCondit> {

    /**
     * 查询自助报表条件管理数据
     * @param reportCondit
     * @return
     */
    public List<ReportCondit> selConditList(@Param("reportCondit") ReportCondit reportCondit);

    /**
     * 根据知识类别编码查询在已发布知识中的总数
     * @param classifyId
     * @return
     */
    public int selConditCountByClassid(List<Integer> classifyId);


    /**
     * 根据条件编码查询不在此条件编码中的条件列表
     * @param conditCodeList
     * @return
     */
    public List<ReportCondit> selConditNotConditCode(List<String> conditCodeList,int indexId);
}
