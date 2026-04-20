package com.pearadmin.modules.report.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pearadmin.common.constant.ControllerConstant;
import com.pearadmin.common.web.base.BaseController;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.modules.report.domain.ReportIndexWarningLog;
import com.pearadmin.modules.report.service.impl.ReportIndexWarningLogServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;

/**
 * 创建日期：2025-10-22
 * 自助取数指标预警日志
 **/

@RestController
@Api(tags = {"自助取数指标预警日志"})
@Slf4j
@RequestMapping(ControllerConstant.API_REPORT_PREFIX + "indexWarningLog")
public class ReportIndexWarningLogController  extends BaseController {

    //基础路径
    private final String modelPath = "autoData/indexWarningLog/";

    @Resource
    private ReportIndexWarningLogServiceImpl reportIndexWarningLogService;

    /**
     * 打开自助取数指标预警页面
     * @return
     */
    @GetMapping("toIndexWarningLog")
    @ApiOperation(value = "打开自助取数指标预警页面",notes = "打开自助取数指标预警页面")
    @PreAuthorize("hasPermission('/autoData/indexWarningLog/toIndexWarningLog','autoData:indexWarningLog:toIndexWarningLog')")
    public ModelAndView toIndexWarningLog(Model model) {
        return jumpPage(modelPath + "indexWarningLog");
    }


    /**
     * 获取自助取数指标预警列表数据
     * @param reportIndexWarningLog
     * @param pageDomain
     * @return
     */
    @GetMapping("getIndexWarningLogList")
    @ApiOperation(value = "获取自助取数指标预警列表数据",notes = "获取自助取数指标预警列表数据")
    public ResultTable getIndexWarningLogList(ReportIndexWarningLog reportIndexWarningLog, PageDomain pageDomain) {
        PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
        PageInfo<ReportIndexWarningLog> pageInfo = new PageInfo<>(reportIndexWarningLogService.getIndexWarningLogList(reportIndexWarningLog));
        return pageTable(pageInfo.getList(), pageInfo.getTotal());
    }

}
