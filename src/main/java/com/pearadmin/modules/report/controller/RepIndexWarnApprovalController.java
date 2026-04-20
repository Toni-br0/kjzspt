package com.pearadmin.modules.report.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pearadmin.common.aop.annotation.Log;
import com.pearadmin.common.aop.enums.BusinessType;
import com.pearadmin.common.constant.ControllerConstant;
import com.pearadmin.common.web.base.BaseController;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.modules.report.domain.ReportIndexWarning;
import com.pearadmin.modules.report.service.impl.RepIndexWarnApprovalServiceImpl;
import com.pearadmin.modules.sys.domain.SysDictData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.List;

/**
 * 创建日期：2025-10-24
 * 指标预警审批
 **/

@RestController
@Api(tags = {"指标预警审批"})
@Slf4j
@RequestMapping(ControllerConstant.API_REPORT_PREFIX + "repIndexWarnApproval")
public class RepIndexWarnApprovalController extends BaseController {

    //基础路径
    private final String modelPath = "autoData/repIndexWarnApproval/";

    @Resource
    private RepIndexWarnApprovalServiceImpl repIndexWarnApprovalService;


    /**
     * 打开指标预警审批页面
     * @return
     */
    @GetMapping("toRepIndexWarnApproval")
    @ApiOperation(value = "打开指标预警审批页面",notes = "打开指标预警审批页面")
    @PreAuthorize("hasPermission('/autoData/repIndexWarnApproval/toRepIndexWarnApproval','autoData:repIndexWarnApproval:toRepIndexWarnApproval')")
    public ModelAndView toRepIndexWarnApproval(Model model) {

        return jumpPage(modelPath + "repIndexWarnApproval");
    }

    /**
     * 获取指标预警审批列表数据
     * @param reportIndexWarning
     * @param pageDomain
     * @return
     */
    @GetMapping("getRepIndexWarnApprovalList")
    @ApiOperation(value = "获取指标预警审批列表数据",notes = "获取指标预警审批列表数据")
    public ResultTable getRepIndexWarnApprovalList(ReportIndexWarning reportIndexWarning, PageDomain pageDomain) {
        PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
        PageInfo<ReportIndexWarning> pageInfo = new PageInfo<>(repIndexWarnApprovalService.getRepIndexWarnApprovalList(reportIndexWarning));
        return pageTable(pageInfo.getList(), pageInfo.getTotal());
    }

    /**
     * 根据ID获取审批状态
     * @param param
     * @return
     */
    @PostMapping("getApplyState")
    @ApiOperation(value = "根据ID获取指标预警审批状态",notes = "根据ID获取指标预警审批状态")
    public JSONObject getApplyState(@RequestBody String param) {
        return repIndexWarnApprovalService.getApplyState(param);
    }

    /**
     * 根据所选ID批量获取审批状态
     * @param param
     * @return
     */
    @PostMapping("getBatchApplyState")
    @ApiOperation(value = "根据ID获取指标预警审批状态",notes = "根据ID获取指标预警审批状态")
    public JSONObject getBatchApplyState(@RequestBody String param) {
        return repIndexWarnApprovalService.getBatchApplyState(param);
    }

    /**
     * 打开自助取数指标预警审批弹出页面
     * @return
     */
    @GetMapping("toIndexWarnAppr")
    public ModelAndView toIndexWarnAppr(Model model) {
        return jumpPage(modelPath + "indexWarnAppr");
    }

    /**
     * 保存审批结果信息
     * @param param
     * @return
     */
    @PostMapping("saveApplyResult")
    @ApiOperation(value = "保存指标预警审批审批结果信息",notes = "保存指标预警审批审批结果信息")
    @Log(title = "保存指标预警审批审批结果信息", describe = "保存指标预警审批审批结果信息", type = BusinessType.ADD)
    public JSONObject saveApplyResult(@RequestBody String param) {
        return repIndexWarnApprovalService.saveApplyResult(param);
    }

    /**
     * 批量保存指标预警审批审批结果信息
     * @param param
     * @return
     */
    @PostMapping("saveBatchApplyResult")
    @ApiOperation(value = "批量保存指标预警审批审批结果信息",notes = "批量保存指标预警审批审批结果信息")
    @Log(title = "批量保存指标预警审批审批结果信息", describe = "批量保存指标预警审批审批结果信息", type = BusinessType.ADD)
    public JSONObject saveBatchApplyResult(@RequestBody String param) {
        return repIndexWarnApprovalService.saveBatchApplyResult(param);
    }


}
