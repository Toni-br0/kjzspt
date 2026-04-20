package com.pearadmin.modules.hdjk.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pearadmin.common.aop.annotation.Log;
import com.pearadmin.common.aop.enums.BusinessType;
import com.pearadmin.common.constant.CommonConstant;
import com.pearadmin.common.constant.ControllerConstant;
import com.pearadmin.common.web.base.BaseController;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.common.web.domain.response.Result;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.modules.hdjk.domain.HdjkModelManage;
import com.pearadmin.modules.hdjk.service.impl.HdjkModelManageServiceImpl;
import com.pearadmin.modules.im.domain.ImHdjkPushManage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 创建日期：2025-05-22
 *  活动监控模块管理控制器
 **/

@RestController
@Api(tags = {"活动监控模块管理控制器"})
@Slf4j
@RequestMapping(ControllerConstant.API_RBHDJK_PREFIX + "modelManage")
public class HdjkModelManageController extends BaseController {

    @Resource
    private HdjkModelManageServiceImpl rbhdjkModelManageService;

    /**
     * 基础路径
     */
    private final String modulePath = "hdjk/modelManage/";

    /**
     * 打开活动监控模块管理页面
     * @return
     */
    @GetMapping("toModelManage")
    @ApiOperation(value = "打开活动监控模块管理页面",notes = "打开活动监控模块管理页面")
    @PreAuthorize("hasPermission('/hdjk/modelManage/toModelManage','hdjk:modelManage:toModelManage')")
    public ModelAndView toModelManage() {
        return jumpPage(modulePath + "modelManage");
    }

    /**
     * 获取活动监控模块管理列表数据
     * @param rbhdjkModelManage
     * @param pageDomain
     * @return
     */
    @GetMapping("modelDataList")
    @ApiOperation(value = "获取活动监控模块管理列表数据",notes = "获取活动监控模块管理列表数据")
    public ResultTable modelDataList(HdjkModelManage rbhdjkModelManage, PageDomain pageDomain) {
        PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
        PageInfo<HdjkModelManage> pageInfo = new PageInfo<>(rbhdjkModelManageService.modelDataList(rbhdjkModelManage));
        return pageTable(pageInfo.getList(), pageInfo.getTotal());
    }

    /**
     * 打开活动监控模块管理上传页面
     * @return
     */
    @GetMapping("modelAdd")
    @ApiOperation(value = "打开活动监控模块管理上传页面",notes = "打开活动监控模块管理上传页面")
    public ModelAndView modelAdd() {
        return jumpPage(modulePath + "modelAdd");
    }

    /**
     * 活动监控模块管理文件上传
     * @param file
     * @return
     */
    @RequestMapping(value = "upload")
    @ApiOperation(value = "活动监控模块管理文件上传",notes = "活动监控模块管理文件上传")
    @Log(title = "活动监控模块管理文件上传", describe = "活动监控模块管理文件上传", type = BusinessType.UPLOAD)
    @ResponseBody
    public Result upload(@RequestParam("file") MultipartFile file, String qyValue, String tableName, String modelType,String modelParam,String backgroundColor,String lineColor,String isCreateImg,String sendFileName) {
        boolean result = rbhdjkModelManageService.upload(file,qyValue,tableName,modelType,modelParam,backgroundColor,lineColor,isCreateImg,sendFileName);
        if (result) {
            return Result.success(0, "上传成功", result);
        } else {
            return Result.failure("上传失败");
        }
    }

    /**
     * 活动监控模块管理修改
     * @param file
     * @return
     */
    @RequestMapping(value = "editHdjkMb")
    @ApiOperation(value = "活动监控模块管理修改",notes = "活动监控模块管理修改")
    @Log(title = "活动监控模块管理修改", describe = "活动监控模块管理修改", type = BusinessType.EDIT)
    @ResponseBody
    public Result editHdjkMb(@RequestParam("file") MultipartFile file, String qyValue, String tableName, String modelType,String modelParam,String backgroundColor,String lineColor,String isCreateImg,int modelId,String sendFileName) {
        boolean result = rbhdjkModelManageService.editHdjkMb(file,qyValue,tableName,modelType,modelParam,backgroundColor,lineColor,isCreateImg,modelId,sendFileName);
        if (result) {
            return Result.success(0, "修改成功", result);
        } else {
            return Result.failure("修改失败");
        }
    }

    /**
     * Describe: 活动监控模块管理文件删除
     * Param: id
     * Return: 文件流
     */
    @DeleteMapping("modelRemove/{modelId}")
    @ApiOperation(value = "活动监控模块管理文件删除",notes = "活动监控模块管理文件删除")
    @Log(title = "活动监控模块管理文件删除", describe = "活动监控模块管理文件删除", type = BusinessType.REMOVE)
    public Result modelRemove(@PathVariable("modelId") int modelId) {
        boolean result = rbhdjkModelManageService.modelRemove(modelId);
        return Result.decide(result, "删除成功", "删除失败");
    }

    /**
     * 活动监控模块管理文件批量删除
     * @param modelIds
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @ApiOperation(value = "活动监控模块管理文件批量删除",notes = "活动监控模块管理文件批量删除")
    @DeleteMapping("batchModelRemove/{modelIds}")
    @Log(title = "活动监控模块管理文件批量删除", describe = "活动监控模块管理文件批量删除", type = BusinessType.REMOVE)
    public Result batchRemove(@PathVariable("modelIds") String modelIds) {
        for (String modelId : modelIds.split(CommonConstant.COMMA)) {
            int iModelId = Integer.parseInt(modelId);
            rbhdjkModelManageService.modelRemove(iModelId);
        }
        return Result.success("删除成功");
    }

    /**
     * 文件下载
     * @param modelId
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/downloadFile", method = RequestMethod.GET)
    @ApiOperation(value = "活动监控模块管理文件下载",notes = "活动监控模块管理文件下载")
    @Log(title = "活动监控模块管理文件下载", describe = "活动监控模块管理文件下载", type = BusinessType.DOWNLOAD)
    public ResponseEntity<InputStreamResource> downloadFile(@RequestParam(name = "modelId") int modelId) {
        return rbhdjkModelManageService.downloadFile(modelId);
    }

    /**
     * Describe: 活动监控模块管理生成文件
     * Param: id
     * Return: 文件流
     */
    @RequestMapping(value = "/createFile", method = RequestMethod.GET)
    @ApiOperation(value = "活动监控模块管理生成文件",notes = "活动监控模块管理生成文件")
    @Log(title = "活动监控模块管理生成文件", describe = "活动监控模块管理生成文件", type = BusinessType.ADD)
    public Result createFile(@RequestParam(name = "modelId") int modelId) {
        boolean result = rbhdjkModelManageService.createFile(modelId);
        return Result.decide(result, "生成文件成功", "生成文件失败");
    }

    /**
     * Describe: 活动监控模块管理批量生成文件
     * Param: id
     * Return: 文件流
     */
    @RequestMapping(value = "/batchCreateFile", method = RequestMethod.POST)
    @ApiOperation(value = "活动监控模块管理批量生成文件",notes = "活动监控模块管理批量生成文件")
    @Log(title = "活动监控模块管理批量生成文件", describe = "活动监控模块管理批量生成文件", type = BusinessType.ADD)
    public Result batchCreateFile(@RequestBody String param) {
        boolean result = rbhdjkModelManageService.batchCreateFile(param);
        return Result.decide(result, "生成文件成功", "生成文件失败");
    }

    /**
     * 在线预览
     * @param param
     * @param response
     */
    @RequestMapping(value = "/fileView", method = RequestMethod.POST)
    public JSONObject fileView(@RequestBody String param, HttpServletResponse response) {
        return rbhdjkModelManageService.fileView(param, response);
    }

    /**
     * 打开活动监控模板管理修改页面
     */
    @GetMapping("edit")
    @ApiOperation(value = "打开活动监控模板管理修改页面",notes = "打开活动监控模板管理修改页面")
    public ModelAndView edit(Model model, int modelId) {

        HdjkModelManage hdjkModelManage = rbhdjkModelManageService.getById(modelId);
        model.addAttribute("hdjkModelManage", hdjkModelManage);
        return jumpPage(modulePath + "modelEdit");
    }


}
