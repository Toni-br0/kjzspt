package com.pearadmin.modules.report.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pearadmin.common.aop.annotation.Log;
import com.pearadmin.common.aop.annotation.Repeat;
import com.pearadmin.common.aop.enums.BusinessType;
import com.pearadmin.common.constant.ControllerConstant;
import com.pearadmin.common.web.base.BaseController;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.modules.report.domain.ReportClassify;
import com.pearadmin.modules.report.domain.ReportIndexStateInfo;
import com.pearadmin.modules.report.domain.ReportParam;
import com.pearadmin.modules.report.service.ReportIndexStateInfoService;
import com.pearadmin.modules.report.service.impl.RepClassManageServiceImpl;
import com.pearadmin.modules.report.service.impl.ReportIndexStateInfoServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 创建日期：2025-08-15
 * 自助取数指标口径查询
 **/

@RestController
@Api(tags = {"指标口径查询"})
@Slf4j
@RequestMapping(ControllerConstant.API_REPORT_PREFIX + "indexStateInfo")
public class ReportIndexStateInfoController extends BaseController {
    //基础路径
    private final String modelPath = "autoData/indexStateInfo/";

    @Resource
    private ReportIndexStateInfoServiceImpl reportIndexStateInfoService;

    @Resource
    private RepClassManageServiceImpl reportClassifyService;

    /**
     * 打开指标口径查询页面
     * @return
     */
    @GetMapping("toIndexStateInfo")
    @ApiOperation(value = "打开指标口径查询页面",notes = "打开指标口径查询页面")
    public ModelAndView toIndexStateInfo(Model model) {
        return jumpPage(modelPath + "indexStateInfo");
    }

    /**
     * 获取指标口径查询列表数据
     * @param reportIndexStateInfo
     * @param pageDomain
     * @return
     */
    @GetMapping("getIndexStateList")
    @ApiOperation(value = "获取指标口径查询列表数据",notes = "获取指标口径查询列表数据")
    public ResultTable getIndexStateList(ReportIndexStateInfo reportIndexStateInfo, PageDomain pageDomain) {
        PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
        PageInfo<ReportIndexStateInfo> pageInfo = new PageInfo<>(reportIndexStateInfoService.getIndexStateList(reportIndexStateInfo));
        return pageTable(pageInfo.getList(), pageInfo.getTotal());
    }

    /**
     * 打开指标口径新增页面
     * @return
     */
    @GetMapping("toAddIndexState")
    @ApiOperation(value = "打开指标口径新增页面",notes = "打开指标口径新增页面")
    public ModelAndView toAddIndexState(Model model) {
        List<ReportClassify> reportClassifyList = reportClassifyService.getClassList();
        model.addAttribute("classifys", reportClassifyList);
        return jumpPage(modelPath + "addIndexState");
    }

    /**
     * 保存自助取数指标口径查询数据
     * @param reportIndexStateInfo
     * @return
     */
    @Repeat
    @PostMapping("saveIndexStateInfo")
    @ApiOperation(value = "保存自助取数指标口径查询数据",notes = "保存自助取数指标口径查询数据")
    @Log(title = "保存自助取数指标口径查询数据", describe = "保存自助取数指标口径查询数据", type = BusinessType.ADD)
    public JSONObject saveIndexStateInfo(@RequestBody ReportIndexStateInfo reportIndexStateInfo){
        return reportIndexStateInfoService.saveIndexStateInfo(reportIndexStateInfo);
    }

    /**
     * 修改自助取数指标口径查询数据
     * @param reportIndexStateInfo
     * @return
     */
    @Repeat
    @PostMapping("updateIndexStateInfo")
    @ApiOperation(value = "修改自助取数指标口径查询数据",notes = "修改自助取数指标口径查询数据")
    @Log(title = "修改自助取数指标口径查询数据", describe = "修改自助取数指标口径查询数据", type = BusinessType.ADD)
    public JSONObject updateIndexStateInfo(@RequestBody ReportIndexStateInfo reportIndexStateInfo){
        return reportIndexStateInfoService.updateIndexStateInfo(reportIndexStateInfo);
    }

    /**
     * 单个删除自助取数指标口径查询数据
     * Param: id
     * Return: 文件
     */
    @DeleteMapping("remove/{infoId}")
    @ApiOperation(value = "单个删除自助取数指标口径查询数据",notes = "单个删除自助取数指标口径查询数据")
    @Log(title = "单个删除自助取数指标口径查询数据", describe = "单个删除自助取数指标口径查询数据", type = BusinessType.REMOVE)
    public JSONObject remove(@PathVariable("infoId") int infoId) {
        return reportIndexStateInfoService.remove(infoId);
    }

    /**
     * 批量删除自助取数指标口径查询数据
     * @param infoIds
     * @return
     */
    @DeleteMapping("batchRemove/{infoIds}")
    @ApiOperation(value = "批量删除自助取数指标口径查询数据",notes = "批量删除自助取数指标口径查询数据")
    @Log(title = "批量删除自助取数指标口径查询数据", describe = "批量删除自助取数指标口径查询数据", type = BusinessType.REMOVE)
    public JSONObject batchRemove(@PathVariable("infoIds") String infoIds) {
        return reportIndexStateInfoService.batchRemove(infoIds);
    }

    /**
     * 打开自助取数指标口径查询修改页面
     */
    @GetMapping("toEditIndexState")
    @ApiOperation(value = "打开自助取数指标口径查询修改页面",notes = "打开自助取数指标口径查询修改页面")
    public ModelAndView toEditIndexState(Model model,int infoId) {

        List<ReportClassify> reportClassifyList = reportClassifyService.getClassList();
        model.addAttribute("classifys", reportClassifyList);

        ReportIndexStateInfo reportIndexStateInfo = reportIndexStateInfoService.getIndexStateInfoById(infoId);
        model.addAttribute("reportIndexStateInfo", reportIndexStateInfo);

        return jumpPage(modelPath + "editIndexState");
    }

    /**
     * 导出报表
     * @param reportIndexStateInfo
     * @return
     */
    @PostMapping("fileDownload")
    @ApiOperation(value = "导出报表",notes = "导出报表")
    public JSONObject fileDownload(@RequestBody ReportIndexStateInfo reportIndexStateInfo) {
        return reportIndexStateInfoService.fileDownload(reportIndexStateInfo);
    }


}
