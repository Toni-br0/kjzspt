package com.pearadmin.modules.report.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pearadmin.common.aop.annotation.Log;
import com.pearadmin.common.aop.enums.BusinessType;
import com.pearadmin.common.constant.ControllerConstant;
import com.pearadmin.common.web.base.BaseController;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.common.web.domain.response.Result;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.modules.ppt.service.impl.AutoValueServiceImpl;
import com.pearadmin.modules.report.domain.*;
import com.pearadmin.modules.report.service.impl.ReportServiceImpl;
import com.pearadmin.modules.sys.domain.SysDictData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 自助取数
 */
@RestController
@Api(tags = {"自助取数"})
@Slf4j
@RequestMapping(ControllerConstant.API_REPORT_PREFIX + "report")
public class ReportController extends BaseController {

    //基础路径
    private final String MODULE_PATH = "autoData/";

    @Resource
    private ReportServiceImpl reportService;

    @Resource
    private AutoValueServiceImpl autoValueService;

    /**
     * 新建报表页面
     * @return
     */
    @GetMapping("reportAdd")
    @ApiOperation(value = "新建报表页面",notes = "新建报表页面")
    @PreAuthorize("hasPermission('/autoData/report/reportAdd','autoData:report:reportAdd')")
    public ModelAndView reportAdd(Model model) {
        //获取分类
        model.addAttribute("reportClassifys", reportService.searchClassify());
        //客户类型下拉框
        List<SysDictData> zzqsKhlxs = autoValueService.getSysDictDataList("zzqs_khlx_select");

        model.addAttribute("zzqsKhlxs", zzqsKhlxs);

        String currArea = reportService.getCurrAreaInfo();

        model.addAttribute("currArea", currArea);

        return jumpPage(MODULE_PATH + "reportAdd");
    }

    /**
     * 查看报表
     * @return
     */
    @GetMapping("reportView")
    @ApiOperation(value = "查看报表",notes = "查看报表")
    public ModelAndView reportView(Model model) {
        model.addAttribute("reportClassifys", reportService.searchClassify());

        //客户类型下拉框
        List<SysDictData> zzqsKhlxs = autoValueService.getSysDictDataList("zzqs_khlx_select");

        model.addAttribute("zzqsKhlxs", zzqsKhlxs);

        String currArea = reportService.getCurrAreaInfo();

        model.addAttribute("currArea", currArea);

        return jumpPage(MODULE_PATH + "reportView");
    }

    /**
     * 查看模板
     * @return
     */
    @GetMapping("templateView")
    @ApiOperation(value = "查看模板",notes = "查看模板")
    public ModelAndView templateView(Model model) {
        model.addAttribute("reportClassifys", reportService.searchClassify());
        //客户类型下拉框
        List<SysDictData> zzqsKhlxs = autoValueService.getSysDictDataList("zzqs_khlx_select");

        model.addAttribute("zzqsKhlxs", zzqsKhlxs);

        String currArea = reportService.getCurrAreaInfo();

        model.addAttribute("currArea", currArea);

        return jumpPage(MODULE_PATH + "templateView");
    }

    /**
     * 新建模板页面
     * @return
     */
    @GetMapping("templateAdd")
    @ApiOperation(value = "新建模板页面",notes = "新建模板页面")
    @PreAuthorize("hasPermission('/autoData/report/templateAdd','autoData:report:templateAdd')")
    public ModelAndView templateAdd(Model model) {
        return jumpPage(MODULE_PATH + "templateAdd");
    }

    /**
     * 我的报表页面
     * @return
     */
    @GetMapping("myReport")
    @ApiOperation(value = "我的报表页面",notes = "我的报表页面")
    @PreAuthorize("hasPermission('/autoData/report/myReport','autoData:report:myReport')")
    public ModelAndView myReport(Model model) {
        return jumpPage(MODULE_PATH + "myReport");
    }

    /**
     * 我的模板页面
     * @return
     */
    @GetMapping("myTemplate")
    @ApiOperation(value = "我的模板页面",notes = "我的模板页面")
    @PreAuthorize("hasPermission('/autoData/report/myTemplate','autoData:report:myTemplate')")
    public ModelAndView myTemplate(Model model) {
        return jumpPage(MODULE_PATH + "myTemplate");
    }


    /**
     * 获取维度树数据
     * @return
     */

    @RequestMapping(value = "/dimensionTreeload")
    @ResponseBody
    public Object dimensionTreeload(@RequestParam String isNonStand){
      return reportService.dimensionTreeload(isNonStand);
    }

    /**
     * 获取维度树数据(根节点)
     * @return
     */

    @RequestMapping(value = "/dimensionTreeloadRoot")
    @ResponseBody
    public Object dimensionTreeloadRoot(@RequestParam String isNonStand){
        return reportService.dimensionTreeloadRoot(isNonStand);
    }

    /**
     * 获取维度树数据(子节点)
     * @return
     */

    @RequestMapping(value = "/dimensionTreeloadChild")
    @ResponseBody
    public Object dimensionTreeloadChild(@RequestParam String isNonStand,@RequestParam String parentId){
        return reportService.dimensionTreeloadChild(isNonStand,parentId);
    }

    /**
     * 获取报表数据
     * @param reportParam
     * @param pageDomain
     * @return
     */
    @PostMapping("reportList")
    @ApiOperation(value = "获取报表数据",notes = "获取报表数据")
    public ResultTable reportList(ReportParam reportParam, PageDomain pageDomain) {

        PageInfo<RetReportData> pageInfo = reportService.getReportList(reportParam,pageDomain);
        return pageTable(pageInfo.getList(), pageInfo.getTotal());
    }

    /**
     * 获取报表数据
     * @param reportParam
     * @param pageDomain
     * @return
     */
    @PostMapping("reportListNew")
    @ApiOperation(value = "获取报表数据",notes = "获取报表数据")
    public Map<String, Object> reportListNew(ReportParam reportParam, PageDomain pageDomain) {
        return reportService.reportListNew(reportParam,pageDomain);
    }

    /**
     * 保存报表
     * @param param
     * @param response
     */
    @RequestMapping(value = "/saveReport", method = RequestMethod.POST)
    public JSONObject saveReport(@RequestBody String param, HttpServletResponse response) {
        JSONObject paramObj = JSONObject.parseObject(param);
        return reportService.saveReport(paramObj);
    }

    /**
     * 保存模板
     * @param param
     * @param response
     */
    @RequestMapping(value = "/saveTemplate", method = RequestMethod.POST)
    public JSONObject saveTemplate(@RequestBody String param, HttpServletResponse response) {
        JSONObject paramObj = JSONObject.parseObject(param);
        return reportService.saveTemplate(paramObj);
    }

    /**
     * 导出报表
     * @param param
     * @param response
     */
//    @GetMapping("exportReport")
    @RequestMapping(value = "/exportReport", method = RequestMethod.POST)
    public void exportExcel(@RequestBody String param,  HttpServletResponse response) throws IOException {
        JSONObject paramObj = JSONObject.parseObject(param);

        // 调用Service层生成Excel并写入响应流
        reportService.exportExcel(paramObj, response);
    }

    /**
     * 报表下载
     * @param reportId
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/downloadReport", method = RequestMethod.GET)
    @ApiOperation(value = "报表下载",notes = "报表下载")
    @Log(title = "报表下载", describe = "报表下载", type = BusinessType.DOWNLOAD)
    public ResponseEntity<InputStreamResource> downloadReport(@RequestParam(name = "reportId") String reportId) {
        return reportService.downloadReport(reportId);
    }

    /**
     * 模板下载
     * @param templateId
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/downloadTemplate", method = RequestMethod.GET)
    @ApiOperation(value = "模板下载",notes = "模板下载")
    @Log(title = "模板下载", describe = "模板下载", type = BusinessType.DOWNLOAD)
    public ResponseEntity<InputStreamResource> downloadTemplate(@RequestParam(name = "templateId") int templateId) {
        return reportService.downloadTemplate(templateId);
    }

    /**
     * 我的报表数据
     * @param reportInfo
     * @param pageDomain
     * @return
     */
    @GetMapping("reportInfoList")
    @ApiOperation(value = "我的报表数据",notes = "我的报表数据")
    public ResultTable reportInfoList(ReportInfo reportInfo, PageDomain pageDomain) {
        PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
        PageInfo<ReportInfo> pageInfo = new PageInfo<ReportInfo>(reportService.getReportInfoList(reportInfo));
        return pageTable(pageInfo.getList(), pageInfo.getTotal());
    }

    /**
     * 我的模板数据
     * @param templateInfo
     * @param pageDomain
     * @return
     */
    @GetMapping("templateInfoList")
    @ApiOperation(value = "我的模板数据",notes = "我的模板数据")
    public ResultTable templateInfoList(ReportTemplate templateInfo, PageDomain pageDomain) {
        PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
        PageInfo<ReportTemplate> pageInfo = new PageInfo<ReportTemplate>(reportService.getTemplateInfoList(templateInfo));
        return pageTable(pageInfo.getList(), pageInfo.getTotal());
    }

    /**
     * 删除报表
     * Param: id
     * Return: 文件流
     */
    @DeleteMapping("reportRemove/{id}")
    @ApiOperation(value = "报表文件删除",notes = "报表文件删除")
    public Result reportRemove(@PathVariable("id") int id) {
        boolean result = reportService.reportRemove(id);
        return Result.decide(result, "删除成功", "删除失败");
    }

    /**
     * 删除模板
     * Param: id
     * Return: 文件流
     */
    @DeleteMapping("templateRemove/{id}")
    @ApiOperation(value = "模板文件删除",notes = "模板文件删除")
    public Result templateRemove(@PathVariable("id") int id) {
        boolean result = reportService.templateRemove(id);
        return Result.decide(result, "删除成功", "删除失败");
    }

    /**
     * 生成ppt并下载
     * @param reportId
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/reportToPpt", method = RequestMethod.GET)
    @ApiOperation(value = "报表下载",notes = "报表下载")
    @Log(title = "报表下载", describe = "报表下载", type = BusinessType.DOWNLOAD)
    public ResponseEntity<InputStreamResource> reportToPpt(@RequestParam(name = "reportId") int reportId) throws Exception{
//        return reportService.reportToPpt(reportId);
        return null;
    }

    /**
     * 通过报表ID获取报表详情信息
     * @param param
     * @param response
     */
    @RequestMapping(value = "/reportInfo", method = RequestMethod.POST)
    public JSONObject reportInfo(@RequestBody String param, HttpServletResponse response) {
        JSONObject paramObj = JSONObject.parseObject(param);
        return reportService.reportInfo(paramObj.getString("reportId"));
    }

    /**
     * 通过模板ID获取模板详情信息
     * @param param
     * @param response
     */
    @RequestMapping(value = "/templateInfo", method = RequestMethod.POST)
    public JSONObject templateInfo(@RequestBody String param, HttpServletResponse response) {
        JSONObject paramObj = JSONObject.parseObject(param);
        return reportService.templateInfo(paramObj.getInteger("templateId"));
    }

    /**
     * 通过分类Id获取指标树数据
     * @return
     */

    @RequestMapping(value = "/getQuotaByClassId")
    @ResponseBody
    public Object getQuotaByClassId(int classifyId, String indexType,String dateType,String roleType){
        return reportService.getQuotaByClassId(classifyId,indexType,dateType,roleType);

    }

    /**
     * 根据指标Id获取条件
     * @param indexId
     * @return
     */
    @RequestMapping(value = "/getConditByIndexId")
    @ResponseBody
    public Object getConditByIndexId(int indexId,String tjCodeStr){
        return reportService.getConditByIndexId(indexId,tjCodeStr);

    }

    /**
     * 导出报表
     * @param reportParam
     * @return
     */
    @PostMapping("fileDownload")
    @ApiOperation(value = "导出报表",notes = "导出报表")
    public JSONObject fileDownload(@RequestBody ReportParam reportParam) {
        return reportService.fileDownload(reportParam);
    }


    /**
     * 自动化报表
     * @param reportParam
     * @return
     */
    @PostMapping("autoCreateReport")
    @ApiOperation(value = "自动化报表",notes = "自动化报表")
    public JSONObject autoCreateReport(@RequestBody ReportParam reportParam) {
        return reportService.autoCreateReport(reportParam);
    }


    /**
     * 获取当前登录人所属的所有地市
     */
    @RequestMapping(value = "/getAllLatnInfo", method = RequestMethod.POST)
    public JSONObject getAllLatnInfo() {
        return reportService.getAllLatnInfo();
    }


    /**
     * 通过数据周期,分析角色,指标类型 获取指标中已配置的分类信息
     * @return
     */

    @RequestMapping(value = "/getClassByWhere")
    @ResponseBody
    public Object getClassByWhere(String checkdateType, String checkRoleType,String checkIndexType){
        return reportService.getClassByWhere(checkdateType,checkRoleType,checkIndexType);

    }

    /**
     * 打开自助报表页面
     * @return
     */
    @GetMapping("reportAutoContent")
    @ApiOperation(value = "打开自助报表页面",notes = "打开自助报表页面")
    public ModelAndView reportAutoContent(Model model) {
        return jumpPage("autoData/report/reportAutoContent");
    }

    /**
     * 打开推送至领导页面
     * @return
     */
    @GetMapping("selPustLeader")
    @ApiOperation(value = "打开推送至领导页面",notes = "打开推送至领导页面")
    public ModelAndView selPustLeader(Model model) {
        return jumpPage("autoData/report/selPustLeader");
    }


}
