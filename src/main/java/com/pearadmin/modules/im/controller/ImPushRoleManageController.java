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
import com.pearadmin.common.web.domain.response.module.ResultTree;
import com.pearadmin.modules.im.domain.ImPushObjectManage;
import com.pearadmin.modules.im.domain.ImPushOrg;
import com.pearadmin.modules.im.domain.ImPushRoleManage;
import com.pearadmin.modules.im.domain.ImPushRolePersonManage;
import com.pearadmin.modules.im.service.IImPushRoleManageService;
import com.pearadmin.modules.wgppt.domain.WgDtreeData;
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
import java.io.IOException;
import java.util.List;

/**
 *
 * 创建日期：2026-04-03
 *
 **/
@RestController
@Api(tags = {"推送角色管理"})
@Slf4j
@RequestMapping(ControllerConstant.API_IM_PREFIX + "pushRoleManage")
public class ImPushRoleManageController extends BaseController {

    /**
     * 基础路径
     */
    private final String modulePath = "im/pushRoleManage/";

    @Resource
    private IImPushRoleManageService iImPushRoleManageService;

    /**
     * 打开IM推送角色管理页面
     * @return
     */
    @GetMapping("toPushRoleManage")
    @ApiOperation(value = "打开IM推送角色管理页面",notes = "打开IM推送角色管理页面")
    @PreAuthorize("hasPermission('/im/pushRoleManage/toPushRoleManage','im:pushRoleManage:toPushRoleManage')")
    public ModelAndView filePushManage(Model model) {
        return jumpPage(modulePath + "main");
    }

    /**
     * Describe: 获取角色树
     */
    @GetMapping("getRoleTree")
    public ResultTree getRoleTree() {
        List<ImPushRoleManage> data = iImPushRoleManageService.getRoleTree();
        return dataTree(data);
    }

    /**
     * 角色新增
     * @return
     */
    @GetMapping("roleAdd")
    public ModelAndView roleAdd(Model model) {
        return jumpPage(modulePath + "roleAdd");
    }

    /**
     * 保存角色信息
     * @param imPushRoleManage
     * @return
     */
    @Repeat
    @PostMapping("saveRole")
    @ApiOperation(value = "保存角色信息",notes = "保存角色信息")
    @Log(title = "保存角色信息", describe = "保存角色信息", type = BusinessType.ADD)
    public Result saveRole(@RequestBody ImPushRoleManage imPushRoleManage){
        JSONObject retJsonObject = iImPushRoleManageService.saveRole(imPushRoleManage);
        String retCode = retJsonObject.getString("retCode");
        if(retCode.equals("0")){
            return success("保存成功");
        }else{
            String retMsg = retJsonObject.getString("retMsg");
            return failure(retMsg);
        }

    }

    /**
     * Describe: 角色树节点删除
     * Param: id
     * Return: 文件
     */
    @DeleteMapping("roleDel/{roleId}")
    @ApiOperation(value = "角色树节点删除",notes = "角色树节点删除")
    @Log(title = "角色树节点删除", describe = "角色树节点删除", type = BusinessType.REMOVE)
    public Object roleDel(@PathVariable("roleId") String roleId) {
        return iImPushRoleManageService.roleDel(roleId);
    }

    /**
     * 打开角色节点修改页面
     */
    @GetMapping("roleEdit")
    @ApiOperation(value = "打开角色节点修改页面",notes = "打开角色节点修改页面")
    public ModelAndView roleEdit(Model model,String roleId) {

        ImPushRoleManage imPushRoleManage = iImPushRoleManageService.getImPushRoleManageById(roleId);
        model.addAttribute("imPushRoleManage", imPushRoleManage);
        return jumpPage(modulePath + "roleEdit");
    }

    /**
     * 保存编辑角色信息
     * @param imPushRoleManage
     * @return
     */
    @Repeat
    @PostMapping("saveRoleEdit")
    @ApiOperation(value = "保存编辑角色信息",notes = "保存编辑角色信息")
    @Log(title = "保存编辑角色信息", describe = "保存编辑角色信息", type = BusinessType.EDIT)
    public Result saveRoleEdit(@RequestBody ImPushRoleManage imPushRoleManage){
        JSONObject retJsonObject = iImPushRoleManageService.saveRoleEdit(imPushRoleManage);
        String retCode = retJsonObject.getString("retCode");
        if(retCode.equals("0")){
            return success("保存成功");
        }else{
            String retMsg = retJsonObject.getString("retMsg");
            return failure(retMsg);
        }

    }

    /**
     * 获取推送角色对应的人员信息表
     * @param imPushRolePersonManage
     * @param pageDomain
     * @return
     */
    @GetMapping("getRolePersonTableList")
    @ApiOperation(value = "获取推送角色对应的人员信息表",notes = "获取推送角色对应的人员信息表")
    public ResultTable getRolePersonTableList(ImPushRolePersonManage imPushRolePersonManage, PageDomain pageDomain) {
        PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
        PageInfo<ImPushRolePersonManage> pageInfo = new PageInfo<>(iImPushRoleManageService.getRolePersonTableList(imPushRolePersonManage));
        return pageTable(pageInfo.getList(), pageInfo.getTotal());
    }

    /**
     * 打开角色对象新增页面
     * @return
     */
    @GetMapping("personAdd")
    @ApiOperation(value = "打开角色对象新增页面",notes = "打开角色对象新增页面")
    public ModelAndView  personAdd(Model model,String roleId,String roleName) {
        model.addAttribute("roleId",roleId);
        model.addAttribute("roleName",roleName);
        return jumpPage(modulePath + "personAdd");
    }

    /**
     * 保存角色人员信息
     * @param imPushRolePersonManage
     * @return
     */
    @Repeat
    @PostMapping("personSave")
    @ApiOperation(value = "保存角色人员信息",notes = "保存角色人员信息")
    @Log(title = "保存角色人员信息", describe = "保存角色人员信息", type = BusinessType.ADD)
    public Result save(@RequestBody ImPushRolePersonManage imPushRolePersonManage){
        JSONObject retJsonObject = iImPushRoleManageService.personSave(imPushRolePersonManage);
        String retCode = retJsonObject.getString("retCode");
        if(retCode.equals("0")){
            return success("保存成功");
        }else{
            String retMsg = retJsonObject.getString("retMsg");
            return failure(retMsg);
        }

    }

    /**
     * 编辑保存角色人员信息
     * @param imPushRolePersonManage
     * @return
     */
    @Repeat
    @PostMapping("personEditSave")
    @ApiOperation(value = "编辑保存角色人员信息",notes = "编辑保存角色人员信息")
    @Log(title = "编辑保存角色人员信息", describe = "编辑保存角色人员信息", type = BusinessType.EDIT)
    public Result personEditSave(@RequestBody ImPushRolePersonManage imPushRolePersonManage){
        JSONObject retJsonObject = iImPushRoleManageService.personEditSave(imPushRolePersonManage);
        String retCode = retJsonObject.getString("retCode");
        if(retCode.equals("0")){
            return success("保存成功");
        }else{
            String retMsg = retJsonObject.getString("retMsg");
            return failure(retMsg);
        }

    }

    /**
     * Describe: 角色下的推送对象单个删除
     * Param: id
     * Return: 文件
     */
    @DeleteMapping("personRemove/{manageId}")
    @ApiOperation(value = "角色下的推送对象单个删除",notes = "角色下的推送对象单个删除")
    @Log(title = "角色下的推送对象单个删除", describe = "角色下的推送对象单个删除", type = BusinessType.REMOVE)
    public Result personRemove(@PathVariable("manageId") String manageId) {
        boolean result = iImPushRoleManageService.remove(manageId);
        return Result.decide(result, "删除成功", "删除失败");
    }


    /**
     * 批量删除角色下的推送对象
     * @param manageIds
     * @return
     */
    @DeleteMapping("batchPersonRemove/{manageIds}")
    //@ApiOperation(value = "批量删除角色下的推送对象",notes = "批量删除角色下的推送对象")
    //@Log(title = "批量删除角色下的推送对象", describe = "批量删除角色下的推送对象", type = BusinessType.REMOVE)
    public Result batchPersonRemove(@PathVariable("manageIds") String manageIds) {
        for (String manageId : manageIds.split(CommonConstant.COMMA)) {
            iImPushRoleManageService.remove(manageId);
        }
        return Result.success("删除成功");
    }

    /**
     * 角色推送对象修改页面
     */
    @GetMapping("personEdit")
    @ApiOperation(value = "角色推送对象修改页面",notes = "角色推送对象修改页面")
    public ModelAndView personEdit(Model model, String manageId) {

        ImPushRolePersonManage imPushRolePersonManage = iImPushRoleManageService.getById(manageId);
        model.addAttribute("imPushRolePersonManage", imPushRolePersonManage);
        return jumpPage(modulePath + "personEdit");
    }

    /**
     * 角色推送对象批量导入
     * @return
     */
    @GetMapping("batchImport")
    public ModelAndView batchImport(Model model,String roleId,String roleName) {
        model.addAttribute("roleId",roleId);
        model.addAttribute("roleName",roleName);
        return jumpPage(modulePath + "batchImport");
    }

    /**
     * 角色推送对象批量导入模板下载
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/downloadModelFile", method = RequestMethod.GET)
    @ApiOperation(value = "角色推送对象批量导入模板下载",notes = "角色推送对象批量导入模板下载")
    @Log(title = "角色推送对象批量导入模板下载", describe = "角色推送对象批量导入模板下载", type = BusinessType.DOWNLOAD)
    public ResponseEntity<InputStreamResource> downloadModelFile() {
        return iImPushRoleManageService.downloadModelFile();
    }

    /**
     * 角色推送对象管理批量导入数据
     * @param file
     * @return
     */
    @RequestMapping(value = "batchImportData")
    @ApiOperation(value = "角色推送对象管理批量导入数据",notes = "角色推送对象管理批量导入数据")
    @Log(title = "角色推送对象管理批量导入数据", describe = "角色推送对象管理批量导入数据", type = BusinessType.UPLOAD)
    @ResponseBody
    public Result batchImportData(@RequestParam("file") MultipartFile file,String roleId) {
        boolean result = iImPushRoleManageService.batchImportData(file,roleId);
        if (result) {
            return Result.success(0, "上传成功", result);
        } else {
            return Result.failure("上传失败");
        }
    }


}
