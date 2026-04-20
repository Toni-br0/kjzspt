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
import com.pearadmin.modules.im.domain.ImFilePushManage;
import com.pearadmin.modules.im.service.impl.FilePushManageServiceImpl;
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
 * IM文件推送管理
 **/

@RestController
@Api(tags = {"IM文件推送管理"})
@Slf4j
@RequestMapping(ControllerConstant.API_IM_PREFIX + "filePushManage")
public class FilePushManageController extends BaseController {

    /**
     * 基础路径
     */
    private final String modulePath = "im/filePushManage/";

    @Resource
    private FilePushManageServiceImpl filePushManageService;



    /**
     * 打开IM文件推送管理页面
     * @return
     */
    @GetMapping("toFilePushManage")
    @ApiOperation(value = "打开IM文件推送管理页面",notes = "打开IM文件推送管理页面")
    @PreAuthorize("hasPermission('/im/filePushManage/toFilePushManage','im:filePushManage:toFilePushManage')")
    public ModelAndView filePushManage(Model model) {
        return jumpPage(modulePath + "main");
    }

    /**
     * 获取IM文件推送管理列表数据
     * @param imFilePushManage
     * @param pageDomain
     * @return
     */
    @GetMapping("filePushManageList")
    @ApiOperation(value = "获取IM文件推送管理列表数据",notes = "获取IM文件推送管理列表数据")
    public ResultTable filePushManageList(ImFilePushManage imFilePushManage, PageDomain pageDomain) {
        PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
        PageInfo<ImFilePushManage> pageInfo = new PageInfo<>(filePushManageService.getImFilePushManageList(imFilePushManage));
        return pageTable(pageInfo.getList(), pageInfo.getTotal());
    }

    /**
     * 打开IM文件推送管理新增页面
     * @return
     */
    @GetMapping("add")
    @ApiOperation(value = "打开IM文件推送管理新增页面",notes = "打开IM文件推送管理新增页面")
    public ModelAndView filePushManageAdd(Model model) {
        return jumpPage(modulePath + "add");
    }

    /**
     * 打开IM文件推送管理修改页面
     */
    @GetMapping("edit")
    @ApiOperation(value = "打开IM文件推送管理修改页面",notes = "打开IM文件推送管理修改页面")
    public ModelAndView edit(Model model,int manageId) {

        ImFilePushManage imFilePushManage = filePushManageService.getById(manageId);
        model.addAttribute("imFilePushManage", imFilePushManage);
        return jumpPage(modulePath + "edit");
    }

    /**
     * 保存IM文件推送管理数据
     * @param imFilePushManage
     * @return
     */
    @Repeat
    @PostMapping("save")
    @ApiOperation(value = "保存IM文件推送管理数据",notes = "保存IM文件推送管理数据")
    @Log(title = "保存IM文件推送管理数据", describe = "保存IM文件推送管理数据", type = BusinessType.ADD)
    public Result saveConverType(@RequestBody ImFilePushManage imFilePushManage){
        boolean result = filePushManageService.save(imFilePushManage);
        if(result){
            return success("保存成功");
        }else{
            return failure("保存失败");
        }
    }

    /**
     * Describe: 文件推送管理单个删除
     * Param: id
     * Return: 文件
     */
    @DeleteMapping("remove/{manageId}")
    @ApiOperation(value = "文件推送管理单个删除",notes = "文件推送管理单个删除")
    @Log(title = "文件推送管理单个删除", describe = "文件推送管理单个删除", type = BusinessType.REMOVE)
    public Result remove(@PathVariable("manageId") int manageId) {
        boolean result = filePushManageService.remove(manageId);
        return Result.decide(result, "删除成功", "删除失败");
    }


    /**
     * 文件推送管理批量删除
     * @param manageIds
     * @return
     */
    @DeleteMapping("batchRemove/{manageIds}")
    @ApiOperation(value = "文件推送管理批量删除",notes = "文件推送管理批量删除")
    @Log(title = "文件推送管理批量删除", describe = "文件推送管理批量删除", type = BusinessType.REMOVE)
    public Result batchRemove(@PathVariable("manageIds") String manageIds) {
        for (String manageId : manageIds.split(CommonConstant.COMMA)) {
            int iManageId = Integer.parseInt(manageId);
            filePushManageService.remove(iManageId);
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
        return filePushManageService.getModelSelect();
    }

    /**
     * 获取模板下拉树（选中值）
     * @return
     */
    @GetMapping("getModelSelectSel")
    @ApiOperation(value = "获取模板下拉树（选中值）",notes = "获取模板下拉树（选中值）")
    public Object getModelSelectSel(int modelId) {
        return filePushManageService.getModelSelectSel(modelId);
    }

    /**
     * 在线预览PPT文件
     * @param param
     * @param response
     */
    @RequestMapping(value = "/fileView", method = RequestMethod.POST)
    public JSONObject fileView(@RequestBody String param, HttpServletResponse response) {
        return filePushManageService.fileView(param, response);
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
        return filePushManageService.downloadFile(manageId);
    }

    /**
     * 一键绑定
     * @param param
     */
    @RequestMapping(value = "/binding", method = RequestMethod.POST)
    public JSONObject binding(@RequestBody String param) {
        return filePushManageService.binding(param);
    }

    /**
     * 批量一键绑定
     * @param param
     */
    @RequestMapping(value = "/batchBinding", method = RequestMethod.POST)
    public JSONObject batchBinding(@RequestBody String param) {
        return filePushManageService.batchBinding(param);
    }

    /**
     * 一键推送
     * @param param
     */
    @RequestMapping(value = "/push", method = RequestMethod.POST)
    public JSONObject push(@RequestBody String param) {
        return filePushManageService.push(param);
    }

    /**
     * 一键推送至领导
     * @param param
     */
    @RequestMapping(value = "/pushLeader", method = RequestMethod.POST)
    public JSONObject pushLeader(@RequestBody String param) {
        return filePushManageService.pushLeader(param);
    }

    /**
     * 批量一键推送
     * @param param
     */
    @RequestMapping(value = "/batchPush", method = RequestMethod.POST)
    public JSONObject batchPush(@RequestBody String param) {
        return filePushManageService.batchPush(param);
    }

    /**
     * 批量一键推送至领导
     * @param param
     */
    @RequestMapping(value = "/batchPushLeader", method = RequestMethod.POST)
    public JSONObject batchPushLeader(@RequestBody String param) {
        return filePushManageService.batchPushLeader(param);
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
     * 文件上传
     * @param file
     * @return
     */
    @RequestMapping(value = "upload")
    @ApiOperation(value = "IM文件推送管理-文件上传",notes = "IM文件推送管理-文件上传")
    @Log(title = "IM文件推送管理-文件上传", describe = "IM文件推送管理-文件上传", type = BusinessType.UPLOAD)
    @ResponseBody
    public Result upload(@RequestParam("file") MultipartFile file, int manageId) {
        boolean result = filePushManageService.upload(file,manageId);
        if (result) {
            return Result.success(0, "上传成功", result);
        } else {
            return Result.failure("上传失败");
        }
    }


}
