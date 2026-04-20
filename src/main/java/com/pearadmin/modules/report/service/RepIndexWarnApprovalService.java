package com.pearadmin.modules.report.service;

import com.alibaba.fastjson.JSONObject;
import com.pearadmin.modules.report.domain.ReportIndexWarning;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 创建日期：2025-10-24
 *  指标预警审批
 **/

public interface RepIndexWarnApprovalService {

    /**
     * 获取指标预警审批列表数据
     * @param reportIndexWarning
     * @return
     */
    List<ReportIndexWarning> getRepIndexWarnApprovalList(ReportIndexWarning reportIndexWarning);


    /**
     * 根据ID获取指标预警审批状态
     * @param param
     * @return
     */
    JSONObject getApplyState(String param);


    /**
     * 根据ID获取指标预警审批状态
     * @param param
     * @return
     */
    JSONObject getBatchApplyState(String param);


    /**
     * 保存指标预警审批审批结果信息
     * @param param
     * @return
     */
    JSONObject saveApplyResult(String param);


    /**
     * 批量保存指标预警审批审批结果信息
     * @param param
     * @return
     */
    JSONObject saveBatchApplyResult(String param);
}
