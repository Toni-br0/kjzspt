package com.pearadmin.modules.yytjzx.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pearadmin.common.aop.annotation.Log;
import com.pearadmin.common.aop.enums.BusinessType;
import com.pearadmin.common.constant.ControllerConstant;
import com.pearadmin.common.context.UserContext;
import com.pearadmin.common.web.base.BaseController;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.modules.report.domain.ReportIndexStateInfo;
import com.pearadmin.modules.sys.domain.SysDept;
import com.pearadmin.modules.sys.domain.SysUser;
import com.pearadmin.modules.sys.service.SysDeptService;
import com.pearadmin.modules.sys.service.impl.SysDeptServiceImpl;
import com.pearadmin.modules.yytjzx.domain.AutoReportCount;
import com.pearadmin.modules.yytjzx.service.impl.AutoReportCountServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * 创建日期：2025-10-29
 * 自助取数统计
 **/

@RestController
@Api(tags = {"自助取数统计控制器"})
@Slf4j
@RequestMapping(ControllerConstant.API_YYTJZX_PREFIX + "autoReportCount")
public class AutoReportCountController  extends BaseController {

  @Resource
  private AutoReportCountServiceImpl autoReportCountService;

  @Autowired
  private SysDeptServiceImpl sysDeptService;

  //基础路径
  private final String modelPath = "yytjzx/autoReportCount/";


  /**
   * 跳转至自助取数统计页面
   * @return
   */
  @GetMapping("toAutoReportCount")
  @ApiOperation(value = "跳转至自助取数统计页面",notes = "跳转至自助取数统计页面")
  @PreAuthorize("hasPermission('/yytjzx/autoReportCount/toAutoReportCount','yytjzx:autoReportCount:toAutoReportCount')")
  public ModelAndView toAutoReportCount(Model model) {
    //当前登录人信息
    SysUser currentUser = UserContext.currentUser();
    String deptId  = currentUser.getDeptId()==null?"":currentUser.getDeptId();
    String area="";
    if(deptId.equals("1")){//全疆
      area="全疆";
    }else{
      SysDept sysDept = sysDeptService.getById(deptId);
      if(sysDept != null){
        area = sysDept.getDeptName();
      }
    }
    model.addAttribute("area", area);
    model.addAttribute("areaId", deptId);
    return jumpPage(modelPath + "autoReportCount");
  }


  /**
   * 获取维度树数据
   * @return
   */

  @RequestMapping(value = "/dimensionTreeload")
  @ResponseBody
  public Object dimensionTreeload(){
    return autoReportCountService.dimensionTreeload();
  }

  /**
   * 获取整体应用统计数据
   * @return
   */

  @RequestMapping(value = "/getAppCount")
  @ResponseBody
  public ResultTable getAppCount(@RequestParam String area, @RequestParam String startDate, @RequestParam String endDate,PageDomain pageDomain){
    PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
    PageInfo<AutoReportCount> pageInfo = new PageInfo<>(autoReportCountService.getAppCount(area,startDate,endDate));
    return pageTable(pageInfo.getList(), pageInfo.getTotal());
  }

  /**
   * 获取整体应用下一级统计数据
   * @return
   */

  @RequestMapping(value = "/getAppSubCount")
  @ResponseBody
  public ResultTable getAppSubCount(@RequestParam String area,@RequestParam String areaCode, @RequestParam String startDate, @RequestParam String endDate,PageDomain pageDomain){
    PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
    PageInfo<AutoReportCount> pageInfo = new PageInfo<>(autoReportCountService.getAppSubCount(area,areaCode,startDate,endDate));
    return pageTable(pageInfo.getList(), pageInfo.getTotal());
  }


  /**
   * 导出自助取数统计文件
   * @param area
   * @return
   * @throws IOException
   */
  @RequestMapping(value = "/downloadFile", method = RequestMethod.GET)
  @ApiOperation(value = "导出自助取数统计文件",notes = "导出自助取数统计文件")
  @Log(title = "导出自助取数统计文件", describe = "导出自助取数统计文件", type = BusinessType.DOWNLOAD)
  public ResponseEntity<InputStreamResource> downloadFile(@RequestParam(name = "area") String area,@RequestParam(name = "areaCode") String areaCode,@RequestParam(name = "startDate") String startDate,@RequestParam(name = "endDate") String endDate) throws Exception{
    return autoReportCountService.downloadFile(area,areaCode,startDate,endDate);
  }


}
