package com.pearadmin.modules.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pearadmin.modules.report.domain.ReportPustLeaderApproval;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportPustLeaderApprovalMapper extends BaseMapper<ReportPustLeaderApproval> {

    /**
     * 根据自动创建报表表主键删除信息
     * @param createIdList
     * @return
     */
    public int delByAutoCreateId(@Param("createIdList") List<Integer> createIdList);

    public List<ReportPustLeaderApproval> selPustLeaderApprovalList(@Param("reportPustLeaderApproval") ReportPustLeaderApproval reportPustLeaderApproval);


}
