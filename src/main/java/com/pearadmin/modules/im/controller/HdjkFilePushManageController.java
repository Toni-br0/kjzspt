package com.pearadmin.modules.im.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pearadmin.common.aop.annotation.Log;
import com.pearadmin.common.aop.annotation.Repeat;
import com.pearadmin.common.aop.enums.BusinessType;
import com.pearadmin.common.constant.CommonConstant;
import com.pearadmin.common.constant.ControllerConstant;
import com.pearadmin.common.web.base.BaseController;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.common.web.domain.response.Result;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.modules.im.domain.ImHdjkPushManage;
import com.pearadmin.modules.im.service.impl.HdjkFilePushManageServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * 创建日期：2025-04-25
 * 活动监控文件推送管理
 **/

@RestController
@Api(tags = {"活动监控文件推送管理"})
@Slf4j
@RequestMapping(ControllerConstant.API_IM_PREFIX + "hdjkFilePushManage")
public class HdjkFilePushManageController extends BaseController {

    /**
     * 基础路径
     */
    private final String modulePath = "im/hdjkFilePushManage/";

    @Resource
    private HdjkFilePushManageServiceImpl rbFilePushManageService;



    /**
     * 打开IM日报文件推送管理页面
     * @return
     */
    @GetMapping("toHdjkFilePushManage")
    @ApiOperation(value = "打开IM日报文件推送管理页面",notes = "打开IM日报文件推送管理页面")
    @PreAuthorize("hasPermission('/im/hdjkFilePushManage/toHdjkFilePushManage','im:hdjkFilePushManage:toHdjkFilePushManage')")
    public ModelAndView filePushManage(Model model) {
        return jumpPage(modulePath + "main");
    }

    /**
     * 获取IM日报文件推送管理列表数据
     * @param imRbFilePushManage
     * @param pageDomain
     * @return
     */
    @GetMapping("filePushManageList")
    @ApiOperation(value = "获取IM日报文件推送管理列表数据",notes = "获取IM日报文件推送管理列表数据")
    public ResultTable filePushManageList(ImHdjkPushManage imRbFilePushManage, PageDomain pageDomain) {
        PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
        PageInfo<ImHdjkPushManage> pageInfo = new PageInfo<>(rbFilePushManageService.getImRbFilePushManageList(imRbFilePushManage));
        return pageTable(pageInfo.getList(), pageInfo.getTotal());
    }

    /**
     * 打开IM日报文件推送管理新增页面
     * @return
     */
    @GetMapping("add")
    @ApiOperation(value = "打开IM日报文件推送管理新增页面",notes = "打开IM日报文件推送管理新增页面")
    public ModelAndView filePushManageAdd(Model model) {
        return jumpPage(modulePath + "add");
    }

    /**
     * 打开IM日报文件推送管理修改页面
     */
    @GetMapping("edit")
    @ApiOperation(value = "打开IM日报文件推送管理修改页面",notes = "打开IM日报文件推送管理修改页面")
    public ModelAndView edit(Model model,int manageId) {

        ImHdjkPushManage imRbFilePushManage = rbFilePushManageService.getById(manageId);
        model.addAttribute("imRbFilePushManage", imRbFilePushManage);
        return jumpPage(modulePath + "edit");
    }

    /**
     * 保存IM日报文件推送管理数据
     * @param imHdjkPushManage
     * @return
     */
    @Repeat
    @PostMapping("save")
    @ApiOperation(value = "保存IM日报文件推送管理数据",notes = "保存IM日报文件推送管理数据")
    @Log(title = "保存IM日报文件推送管理数据", describe = "保存IM日报文件推送管理数据", type = BusinessType.ADD)
    public Result save(@RequestBody ImHdjkPushManage imHdjkPushManage){
        boolean result = rbFilePushManageService.save(imHdjkPushManage);
        if(result){
            return success("保存成功");
        }else{
            return failure("保存失败");
        }
    }

    /**
     * Describe: 日报文件推送管理单个删除
     * Param: id
     * Return: 文件
     */
    @DeleteMapping("remove/{manageId}")
    @ApiOperation(value = "日报文件推送管理单个删除",notes = "日报文件推送管理单个删除")
    @Log(title = "日报文件推送管理单个删除", describe = "日报文件推送管理单个删除", type = BusinessType.REMOVE)
    public Result remove(@PathVariable("manageId") int manageId) {
        boolean result = rbFilePushManageService.remove(manageId);
        return Result.decide(result, "删除成功", "删除失败");
    }


    /**
     * 日报文件推送管理批量删除
     * @param manageIds
     * @return
     */
    @DeleteMapping("batchRemove/{manageIds}")
    @ApiOperation(value = "日报文件推送管理批量删除",notes = "日报文件推送管理批量删除")
    @Log(title = "日报文件推送管理批量删除", describe = "日报文件推送管理批量删除", type = BusinessType.REMOVE)
    public Result batchRemove(@PathVariable("manageIds") String manageIds) {
        for (String manageId : manageIds.split(CommonConstant.COMMA)) {
            int iManageId = Integer.parseInt(manageId);
            rbFilePushManageService.remove(iManageId);
        }
        return Result.success("删除成功");
    }

    /**
     * 获取页面的模板下拉框
     * @return
     */
    @GetMapping("getModelSelect")
    @ApiOperation(value = "获取页面的模板下拉框",notes = "获取页面的模板下拉框")
    public Object getModelSelect() {
        return rbFilePushManageService.getModelSelect();
    }

    /**
     * 获取模板下拉树（选中值）
     * @return
     */
    @GetMapping("getModelSelectSel")
    @ApiOperation(value = "获取模板下拉树（选中值）",notes = "获取模板下拉树（选中值）")
    public Object getModelSelectSel(int modelId) {
        return rbFilePushManageService.getModelSelectSel(modelId);
    }

    /**
     * 在线预览
     * @param param
     * @param response
     */
    @RequestMapping(value = "/fileView", method = RequestMethod.POST)
    public JSONObject fileView(@RequestBody String param, HttpServletResponse response) {
        return rbFilePushManageService.fileView(param, response);
    }

    /**
     * 文件下载
     * @param manageId
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/downloadFile", method = RequestMethod.GET)
    @ApiOperation(value = "文件下载",notes = "文件下载")
    @Log(title = "文件下载", describe = "文件下载", type = BusinessType.DOWNLOAD)
    public ResponseEntity<InputStreamResource> downloadFile(@RequestParam(name = "manageId") int manageId) throws Exception{
        return rbFilePushManageService.downloadFile(manageId);
    }

    /**
     * 一键推送
     * @param param
     */
    @RequestMapping(value = "/push", method = RequestMethod.POST)
    public JSONObject push(@RequestBody String param) {
        return rbFilePushManageService.push(param);
    }

    /**
     * 一键推送
     * @param param
     */
    @RequestMapping(value = "/pushNew", method = RequestMethod.POST)
    public JSONObject pushNew(@RequestBody String param) {
        return rbFilePushManageService.pushNew(param);
    }

    /**
     * 一键推送至领导
     * @param param
     */
    @RequestMapping(value = "/pushLeader", method = RequestMethod.POST)
    public JSONObject pushLeader(@RequestBody String param) {
        return rbFilePushManageService.pushLeader(param);
    }

    /**
     * 一键推送至领导
     * @param param
     */
    @RequestMapping(value = "/pushLeaderNew", method = RequestMethod.POST)
    public JSONObject pushLeaderNew(@RequestBody String param) {
        return rbFilePushManageService.pushLeaderNew(param);
    }

    /**
     * 批量一键发送
     * @param param
     */
    @RequestMapping(value = "/batchPush", method = RequestMethod.POST)
    public JSONObject batchPush(@RequestBody String param) {
        return rbFilePushManageService.batchPush(param);
    }

    /**
     * 批量一键发送
     * @param param
     */
    @RequestMapping(value = "/batchPushNew", method = RequestMethod.POST)
    public JSONObject batchPushNew(@RequestBody String param) {
        return rbFilePushManageService.batchPushNew(param);
    }

    /**
     * 批量一键发送至领导
     * @param param
     */
    @RequestMapping(value = "/batchLeaderPush", method = RequestMethod.POST)
    public JSONObject batchLeaderPush(@RequestBody String param) {
        return rbFilePushManageService.batchLeaderPush(param);
    }

    /**
     * 批量一键发送至领导
     * @param param
     */
    @RequestMapping(value = "/batchLeaderPushNew", method = RequestMethod.POST)
    public JSONObject batchLeaderPushNew(@RequestBody String param) {
        return rbFilePushManageService.batchLeaderPushNew(param);
    }

    /**
     * 打开文件上传页面
     * @return
     */
    @GetMapping("upload/{manageId}")
    @ApiOperation(value = "打开文件上传页面",notes = "打开文件上传页面")
    public ModelAndView upload(@PathVariable("manageId") int manageId,Model model) {
        model.addAttribute("manageId", manageId);
        return jumpPage(modulePath + "upload");
    }

    /**
     * IM日报文件推送管理-文件上传
     * @param file
     * @return
     */
    @RequestMapping(value = "upload")
    @ApiOperation(value = "IM日报文件推送管理-文件上传",notes = "IM日报文件推送管理-文件上传")
    @Log(title = "IM日报文件推送管理-文件上传", describe = "IM日报文件推送管理-文件上传", type = BusinessType.UPLOAD)
    @ResponseBody
    public Result upload(@RequestParam("file") MultipartFile file, int manageId) {
        boolean result = rbFilePushManageService.upload(file,manageId);
        if (result) {
            return Result.success(0, "上传成功", result);
        } else {
            return Result.failure("上传失败");
        }
    }

    /**
     * 校验文件生成时间
     * @param param
     */
    @RequestMapping(value = "/checkFileCreateTime", method = RequestMethod.POST)
    public JSONObject checkFileCreateTime(@RequestBody String param) {
        return rbFilePushManageService.checkFileCreateTime(param);
    }




}
