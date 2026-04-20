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
import com.pearadmin.common.web.domain.response.Result;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.modules.im.domain.ImHdjkPushManage;
import com.pearadmin.modules.report.domain.ReportAutoCreateInfo;
import com.pearadmin.modules.report.domain.ReportClassify;
import com.pearadmin.modules.report.service.impl.RepMyTaskServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;

/**
 * 创建日期：2025-08-05
 * 自助取数我的任务管理
 **/

@RestController
@Api(tags = {"自助取数我的任务管理"})
@Slf4j
@RequestMapping(ControllerConstant.API_REPORT_PREFIX + "myTask")
public class RepMyTaskManageController  extends BaseController {
  //基础路径
  private final String modelPath = "autoData/myTask/";

  @Resource
  private RepMyTaskServiceImpl repMyTaskService;

  /**
   * 打开自助取数我的任务管理页面
   * @return
   */
  @GetMapping("toMyTask")
  @ApiOperation(value = "打开自助取数我的任务管理页面",notes = "打开自助取数我的任务管理页面")
  @PreAuthorize("hasPermission('/autoData/myTask/toMyTask','autoData:myTask:toMyTask')")
  public ModelAndView toMyTask(Model model) {
    return jumpPage(modelPath + "myTaskManage");
  }


  /**
   * 获取自助取数我的任务列表数据
   * @param reportAutoCreateInfo
   * @param pageDomain
   * @return
   */
  @GetMapping("getMyTaskList")
  @ApiOperation(value = "获取自助取数我的任务列表数据",notes = "获取自助取数我的任务列表数据")
  public ResultTable getMyTaskList(ReportAutoCreateInfo reportAutoCreateInfo, PageDomain pageDomain) {
    PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
    PageInfo<ReportAutoCreateInfo> pageInfo = new PageInfo<>(repMyTaskService.getMyTaskList(reportAutoCreateInfo));
    return pageTable(pageInfo.getList(), pageInfo.getTotal());
  }

  /**
   * 单个删除自助取数我的任务
   * Param: id
   * Return: 文件
   */
  @DeleteMapping("remove/{infoId}")
  @ApiOperation(value = "单个删除自助取数我的任务",notes = "单个删除自助取数我的任务")
  @Log(title = "单个删除自助取数我的任务", describe = "单个删除自助取数我的任务", type = BusinessType.REMOVE)
  public JSONObject remove(@PathVariable("infoId") int infoId) {
    return repMyTaskService.remove(infoId);
  }

  /**
   * 批量删除自助取数我的任务
   * @param infoIds
   * @return
   */
  @DeleteMapping("batchRemove/{infoIds}")
  @ApiOperation(value = "批量删除自助取数我的任务",notes = "批量删除自助取数我的任务")
  @Log(title = "批量删除自助取数我的任务", describe = "批量删除自助取数我的任务", type = BusinessType.REMOVE)
  public JSONObject batchRemove(@PathVariable("infoIds") String infoIds) {
    return repMyTaskService.batchRemove(infoIds);
  }

  /**
   * 打开自助取数我的任务修改页面
   */
  @GetMapping("toEditTask")
  @ApiOperation(value = "打开自助取数我的任务修改页面",notes = "打开自助取数我的任务修改页面")
  public ModelAndView toEditTask(Model model,int infoId) {

    ReportAutoCreateInfo reportAutoCreateInfo = repMyTaskService.getMyTaskById(infoId);
    model.addAttribute("reportAutoCreateInfo", reportAutoCreateInfo);

    return jumpPage(modelPath + "editTask");
  }

  /**
   * 打开自助取数我的任务修改页面
   */
  @GetMapping("toEditSqlTask")
  public ModelAndView toEditSqlTask(Model model,int infoId) {

    ReportAutoCreateInfo reportAutoCreateInfo = repMyTaskService.getMyTaskById(infoId);
    model.addAttribute("reportAutoCreateInfo", reportAutoCreateInfo);

    return jumpPage(modelPath + "editSqlTask");
  }


  /**
   * 获取我的任务推送领导下拉框（选中值）
   * @return
   */
  @GetMapping("getPushLeaderSel")
  @ApiOperation(value = "获取我的任务推送领导下拉框（选中值）",notes = "获取我的任务推送领导下拉框（选中值）")
  public Object getPushLeaderSel(int infoId){
    return  repMyTaskService.getPushLeaderSel(infoId);
  }

  /**
   * 保存我的任务数据
   * @param reportAutoCreateInfo
   * @return
   */
  @Repeat
  @PostMapping("saveMyTaskInfo")
  @ApiOperation(value = "保存我的任务数据",notes = "保存我的任务数据")
  @Log(title = "保存我的任务数据", describe = "保存我的任务数据", type = BusinessType.ADD)
  public Result saveMyTaskInfo(@RequestBody ReportAutoCreateInfo reportAutoCreateInfo){
    boolean result = repMyTaskService.saveMyTaskInfo(reportAutoCreateInfo);
    if(result){
      return success("保存成功");
    }else{
      return failure("保存失败");
    }
  }


  /**
   * 根据ID获取审批状态
   * @param param
   * @return
   */
  @PostMapping("getTaskState")
  @ApiOperation(value = "根据ID获取审批状态",notes = "根据ID获取审批状态")
  public JSONObject getTaskState(@RequestBody String param) {
    return repMyTaskService.getTaskState(param);
  }


  /**
   * 修改Sql语句
   * @param reportAutoCreateInfo
   * @return
   */
  @Repeat
  @PostMapping("updateSqlContentInfo")
  @ApiOperation(value = "修改Sql语句",notes = "修改Sql语句")
  @Log(title = "修改Sql语句", describe = "修改Sql语句", type = BusinessType.ADD)
  public Result updateSqlContentInfo(@RequestBody ReportAutoCreateInfo reportAutoCreateInfo){
    boolean result = repMyTaskService.updateSqlContentInfo(reportAutoCreateInfo);
    if(result){
      return success("保存成功");
    }else{
      return failure("保存失败");
    }
  }



}
