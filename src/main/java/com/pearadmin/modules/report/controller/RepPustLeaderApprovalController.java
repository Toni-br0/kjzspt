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
import com.pearadmin.modules.report.domain.ReportAutoCreateInfo;
import com.pearadmin.modules.report.domain.ReportPustLeaderApproval;
import com.pearadmin.modules.report.service.impl.RepPustLeaderApprovalServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;

/**
 * 创建日期：2025-08-22
 * 自助取数报表推送至领导IM申请表
 **/

@RestController
@Api(tags = {"自助取数报表推送至领导IM申请"})
@Slf4j
@RequestMapping(ControllerConstant.API_REPORT_PREFIX + "pustLeaderApproval")
public class RepPustLeaderApprovalController  extends BaseController {

    //基础路径
    private final String modelPath = "autoData/pustLeaderApproval/";

    @Resource
    private RepPustLeaderApprovalServiceImpl repPustLeaderApprovalService;

    /**
     * 打开自助取数报表推送至领导IM申请管理页面
     * @return
     */
    @GetMapping("toPustLeaderApproval")
    @ApiOperation(value = "打开自助取数报表推送至领导IM申请管理页面",notes = "打开自助取数报表推送至领导IM申请管理页面")
    @PreAuthorize("hasPermission('/autoData/pustLeaderApproval/toPustLeaderApproval','autoData:pustLeaderApproval:toPustLeaderApproval')")
    public ModelAndView toPustLeaderApproval(Model model) {
        return jumpPage(modelPath + "pustLeaderApproval");
    }

    /**
     * 获取自助取数报表推送至领导IM申请管理列表数据
     * @param reportPustLeaderApproval
     * @param pageDomain
     * @return
     */
    @GetMapping("getPustLeaderApprovalList")
    @ApiOperation(value = "获取自助取数报表推送至领导IM申请管理列表数据",notes = "获取自助取数报表推送至领导IM申请管理列表数据")
    public ResultTable getPustLeaderApprovalList(ReportPustLeaderApproval reportPustLeaderApproval, PageDomain pageDomain) {
        PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
        PageInfo<ReportPustLeaderApproval> pageInfo = new PageInfo<>(repPustLeaderApprovalService.getPustLeaderApprovalList(reportPustLeaderApproval));
        return pageTable(pageInfo.getList(), pageInfo.getTotal());
    }


    /**
     * 打开自助取数报表推送至领导IM申请管理页面
     * @return
     */
    @GetMapping("toLeaderAppr")
    public ModelAndView toLeaderAppr(Model model) {
        return jumpPage(modelPath + "leaderAppr");
    }

    /**
     * 保存审批结果信息
     * @param param
     * @return
     */
    @PostMapping("saveApplyResult")
    @ApiOperation(value = "保存审批结果信息",notes = "保存审批结果信息")
    public JSONObject saveApplyResult(@RequestBody String param) {
        return repPustLeaderApprovalService.saveApplyResult(param);
    }

    /**
     * 根据ID获取审批状态
     * @param param
     * @return
     */
    @PostMapping("getApplyState")
    @ApiOperation(value = "根据ID获取审批状态",notes = "根据ID获取审批状态")
    public JSONObject getApplyState(@RequestBody String param) {
        return repPustLeaderApprovalService.getApplyState(param);
    }

    /**
     * 获取审批人
     * @return
     */
    @PostMapping("getApplyPerson")
    @ApiOperation(value = "获取审批人",notes = "获取审批人")
    public JSONObject getApplyPerson() {
        return repPustLeaderApprovalService.getApplyPerson();
    }


}
