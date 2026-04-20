package com.pearadmin.modules.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pearadmin.modules.report.domain.ReportIndex;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 创建日期：2025-07-03
 * 指标管理
 **/

@Mapper
public interface ReportIndexMapper extends BaseMapper<ReportIndex> {

    /**
     * 查询指标数据
     * @param reportIndex
     * @return
     */
    public List<ReportIndex> selIndexList(@Param("reportIndex") ReportIndex reportIndex);

    /**
     * 查询指标数据
     * @param reportIndex
     * @return
     */
    public List<ReportIndex> selSubIndexList(@Param("reportIndex") ReportIndex reportIndex);


    /**
     * 查询指标数据
     * @param reportIndex
     * @return
     */
    public List<ReportIndex> selIndexListTable(@Param("reportIndex") ReportIndex reportIndex);


    /**
     * 根据知识类别ID查询在指标中的总数
     * @param classifyId
     * @return
     */
    public int selIndexCountByClassid(List<Integer> classifyId);

    /**
     * 查询指标数据
     * @return
     */
    public List<ReportIndex> selIndexListByIdType(@Param("classifyId") int classifyId,@Param("indexType") String indexType,@Param("dateType") String dateType,@Param("roleType") String roleType);

    /**
     * 更新子指标
     * @param reportIndex
     * @return
     */
    public int updateSubIndex(@Param("reportIndex") ReportIndex reportIndex);
}
