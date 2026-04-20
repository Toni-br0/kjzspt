package com.pearadmin.modules.report.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.pearadmin.common.aop.annotation.Log;
import com.pearadmin.common.aop.enums.BusinessType;
import com.pearadmin.common.constant.CommonConstant;
import com.pearadmin.common.constant.ControllerConstant;
import com.pearadmin.common.tools.SequenceUtil;
import com.pearadmin.common.web.base.BaseController;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.common.web.domain.response.Result;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.common.web.domain.response.module.ResultTree;
import com.pearadmin.modules.report.domain.ReportClassify;
import com.pearadmin.modules.report.domain.ReportIndex;
import com.pearadmin.modules.report.service.RepClassManageService;
import com.pearadmin.modules.report.service.RepIndexManageService;
import com.pearadmin.modules.report.service.impl.RepIndexManageServiceImpl;
import com.pearadmin.modules.sys.domain.SysDept;
import com.pearadmin.modules.sys.domain.SysDict;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.List;

/**
 * 创建日期：2025-07-03
 * 指标管理
 **/

@RestController
@Api(tags = {"指标管理"})
@Slf4j
@RequestMapping(ControllerConstant.API_REPORT_PREFIX + "indexManage")
public class RepIndexManageController  extends BaseController {

    //基础路径
    private final String modelPath = "autoData/indexManage/";

    @Resource
    private RepIndexManageService repIndexManageService;

    @Resource
    private RepClassManageService repClassManageService;


    /**
     * 打开自助取数指标管理页面
     * @return
     */
    @GetMapping("toIndexManage")
    @ApiOperation(value = "打开自助取数指标管理页面",notes = "打开自助取数指标管理页面")
    @PreAuthorize("hasPermission('/autoData/indexManage/toIndexManage','autoData:indexManage:toIndexManage')")
    public ModelAndView toIndexManage(Model model) {
        return jumpPage(modelPath + "indexManage");
    }


    /**
     * Describe: 自助取数指标列表数据
     * Param SysDept PageDomain
     * Return 自助取数指标列表数据
     */
    @GetMapping("getIndexList")
    public ResultTable getIndexList(ReportIndex reportIndex) {
        List<ReportIndex> data = repIndexManageService.getIndexList(reportIndex);
        return dataTable(data);
    }

    /**
     * Describe: 自助取数指标列表数据
     * Param: reportIndex
     * Return: ResuTable
     */
    @GetMapping("getIndexListNew")
    public ResultTable getIndexListNew(ReportIndex reportIndex, PageDomain pageDomain) {
        PageInfo<ReportIndex> pageInfo = repIndexManageService.getIndexListNew(reportIndex, pageDomain);
        return pageTable(pageInfo.getList(), pageInfo.getTotal());
    }

    /**
     * Describe: 自助取数指标列表子数据
     * Param: reportIndex
     * Return: ResuTable
     */
    @GetMapping("getIndexSubList")
    public ResultTable getIndexSubList(ReportIndex reportIndex, PageDomain pageDomain) {
        PageInfo<ReportIndex> pageInfo = repIndexManageService.getIndexSubList(reportIndex, pageDomain);
        return pageTable(pageInfo.getList(), pageInfo.getTotal());
    }

    /**
     * Describe: 获取自助取数指标新增视图
     * Param ModelAndView
     * Return 获取自助取数指标新增视图
     */
    @GetMapping("toAdd")
    public ModelAndView toAdd(Model model,int selParentId) {
        List<ReportClassify> list = repClassManageService.getClassList();
        model.addAttribute("reportClassifys", list);
        model.addAttribute("selParentId", selParentId);

        ReportIndex parentReportIndex = repIndexManageService.getById(selParentId);
        model.addAttribute("parentReportIndex", parentReportIndex);

        return jumpPage(modelPath + "addIndex");
    }

    /**
     * Describe: 获取自助取数指标新增视图
     * Param ModelAndView
     * Return 获取自助取数指标新增视图
     */
    @GetMapping("toParentAdd")
    public ModelAndView toParentAdd(Model model) {
        List<ReportClassify> list = repClassManageService.getClassList();
        model.addAttribute("reportClassifys", list);
        return jumpPage(modelPath + "addParentIndex");
    }

    /**
     * Describe: 获取指标树状数据结构
     * Param ModelAndView
     * Return ModelAndView
     */
    @GetMapping("getIndextree")
    public ResultTree getIndextree(ReportIndex reportIndex) {
        List<ReportIndex> data = repIndexManageService.getIndexList(reportIndex);
        return dataTree(data);
    }

    /**
     * Describe: 获取指标树状数据结构
     * Param ModelAndView
     * Return ModelAndView
     */
    @GetMapping("getSubIndextree")
    public ResultTree getSubIndextree(ReportIndex reportIndex) {
        List<ReportIndex> data = repIndexManageService.getSubIndexList(reportIndex);
        return dataTree(data);
    }

    /**
     * Describe: 保存指标信息
     * Param SysDept
     * Return 执行结果
     */
    @PostMapping("saveIndex")
    @ApiOperation(value = "保存指标信息")
    @Log(title = "保存指标信息", describe = "保存指标信息", type = BusinessType.ADD)
    public JSONObject saveIndex(@RequestBody ReportIndex reportIndex) {
        return repIndexManageService.saveIndex(reportIndex);
    }

    /**
     * Describe: 删除指标
     * Param: id
     * Return: Result
     */
    @DeleteMapping("remove/{indexId}")
    @Log(title = "删除指标", describe = "删除指标", type = BusinessType.REMOVE)
    public JSONObject remove(@PathVariable int indexId) {
        JSONObject retJson = new JSONObject();
        if (repIndexManageService.selectByParentId(indexId).size() > 0) {
            retJson.put("retCode","-1");
            retJson.put("retMsg","请先删除下级指标");
            return retJson;
        }
        return repIndexManageService.remove(indexId);
    }

    /**
     * Describe: 删除指标
     * Param: id
     * Return: Result
     */
    @Transactional(rollbackFor = Exception.class)
    @DeleteMapping("batchRemove/{indexIds}")
    public JSONObject batchRemove(@PathVariable String indexIds) {
        JSONObject retJson = new JSONObject();

        for (String manageId : indexIds.split(CommonConstant.COMMA)) {
            int indexId = Integer.parseInt(manageId);
            repIndexManageService.remove(indexId);
        }

        retJson.put("retCode","0");
        retJson.put("retMsg","删除成功");

        return retJson;
    }

    /**
     * Describe: 获取部门修改视图
     * Param ModelAndView
     * Return 部门修改视图
     */
    @GetMapping("toEdit")
    public ModelAndView toEdit(ModelAndView modelAndView, int indexId) {
        modelAndView.addObject("reportIndex", repIndexManageService.getById(indexId));
        modelAndView.setViewName(modelPath + "editIndex");

        List<ReportClassify> list = repClassManageService.getClassList();
        modelAndView.addObject("reportClassifys", list);

        return modelAndView;
    }

    /**
     * Describe: 获取部门修改视图
     * Param ModelAndView
     * Return 部门修改视图
     */
    @GetMapping("toParentEdit")
    public ModelAndView toParentEdit(ModelAndView modelAndView, int indexId) {
        modelAndView.addObject("reportIndex", repIndexManageService.getById(indexId));
        modelAndView.setViewName(modelPath + "parentEditIndex");

        List<ReportClassify> list = repClassManageService.getClassList();
        modelAndView.addObject("reportClassifys", list);

        return modelAndView;
    }


    /**
     * Describe: 修改部门信息
     * Param reportIndex
     * Return 执行结果
     */
    @PutMapping("updateIndex")
    @Log(title = "修改指标", describe = "修改指标", type = BusinessType.EDIT)
    public JSONObject updateIndex(@RequestBody ReportIndex reportIndex) {
        return repIndexManageService.updateIndex(reportIndex);
    }


    /**
     * Describe: 根据ID获取指标类型和分类
     * Param: id
     * Return: Result
     */
    @GetMapping("getTypeClass/{indexId}")
    public JSONObject getTypeClass(@PathVariable int indexId) {
        return repIndexManageService.getTypeClass(indexId);
    }


    /**
     * Describe: 获取指标树状数据结构
     * Param ModelAndView
     * Return ModelAndView
     */
    @GetMapping("indexTree")
    public ResultTree indexTree() {
        ReportIndex reportIndex = new ReportIndex();
        reportIndex.setIsQuery("1");
        List<ReportIndex> data = repIndexManageService.getIndexList(reportIndex);
        return dataTree(data);
    }


}
