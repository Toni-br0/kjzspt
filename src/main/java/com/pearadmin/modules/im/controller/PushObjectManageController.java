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
import com.pearadmin.modules.im.domain.ImFilePushManage;
import com.pearadmin.modules.im.domain.ImPushObjectManage;
import com.pearadmin.modules.im.domain.ImPushOrg;
import com.pearadmin.modules.im.service.IPushObjectManageService;
import com.pearadmin.modules.im.service.impl.PushObjectManageServiceImpl;
import com.pearadmin.modules.ppt.domain.PptConvertType;
import com.pearadmin.modules.sys.domain.SysDept;
import com.pearadmin.modules.wgppt.domain.VWgzsUserLevelD;
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
 * 创建日期：2025-04-27
 * 推送对象管理
 **/

@RestController
@Api(tags = {"推送对象管理"})
@Slf4j
@RequestMapping(ControllerConstant.API_IM_PREFIX + "pushObjectManage")
public class PushObjectManageController extends BaseController {

    /**
     * 基础路径
     */
    private final String modulePath = "im/pushObjectManage/";

    @Resource
    private IPushObjectManageService pushObjectManageService;

    /**
     * 打开推送对象管理页面
     * @return
     */
    @GetMapping("toPushObjectManage")
    @ApiOperation(value = "打开推送对象管理页面",notes = "打开推送对象管理页面")
    @PreAuthorize("hasPermission('/im/pushObjectManage/toPushObjectManage','im:pushObjectManage:toPushObjectManage')")
    public ModelAndView toPushObjectManage(Model model) {
        return jumpPage(modulePath + "main");
    }

    /**
     * 获取标签分组推送对象管理页面列表数据
     * @param imPushObjectManage
     * @param pageDomain
     * @return
     */
    @GetMapping("pushObjectManageList")
    @ApiOperation(value = "获取推送对象管理页面列表数据",notes = "获取推送对象管理页面列表数据")
    public ResultTable pushObjectManageList(ImPushObjectManage imPushObjectManage, PageDomain pageDomain) {
        PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
        PageInfo<ImPushObjectManage> pageInfo = new PageInfo<>(pushObjectManageService.getPushObjectManageList(imPushObjectManage));
        return pageTable(pageInfo.getList(), pageInfo.getTotal());
    }

    /**
     * 获取组织维度推送对象管理页面列表数据
     * @param imPushObjectManage
     * @param pageDomain
     * @return
     */
    @GetMapping("pushZzwdObjectManageList")
    @ApiOperation(value = "获取组织维度推送对象管理页面列表数据",notes = "获取组织维度推送对象管理页面列表数据")
    public ResultTable pushZzwdObjectManageList(ImPushObjectManage imPushObjectManage, PageDomain pageDomain) {
        PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
        PageInfo<VWgzsUserLevelD> pageInfo = new PageInfo<>(pushObjectManageService.getZzwdPushObjectManageList(imPushObjectManage));
        return pageTable(pageInfo.getList(), pageInfo.getTotal());
    }

    /**
     * 打开推送对象管理新增页面
     * @return
     */
    @GetMapping("add")
    @ApiOperation(value = "打开推送对象管理新增页面",notes = "打开推送对象管理新增页面")
    public ModelAndView  add(Model model,String orgId,String orgName) {
        model.addAttribute("orgId",orgId);
        model.addAttribute("orgName",orgName);
        return jumpPage(modulePath + "add");
    }

    /**
     * 打开数据转换类型修改页面
     */
    @GetMapping("edit")
    @ApiOperation(value = "打开推送对象管理修改页面",notes = "打开推送对象管理修改页面")
    public ModelAndView edit(Model model,int manageId) {

        ImPushObjectManage imPushObjectManage = pushObjectManageService.getById(manageId);
        model.addAttribute("imPushObjectManage", imPushObjectManage);
        return jumpPage(modulePath + "edit");
    }

    /**
     * 保存推送对象管理数据
     * @param imPushObjectManage
     * @return
     */
    @Repeat
    @PostMapping("save")
    @ApiOperation(value = "保存推送对象管理数据",notes = "保存推送对象管理数据")
    @Log(title = "保存推送对象管理数据", describe = "保存推送对象管理数据", type = BusinessType.ADD)
    public Result save(@RequestBody ImPushObjectManage imPushObjectManage){
        JSONObject retJsonObject = pushObjectManageService.save(imPushObjectManage);
        String retCode = retJsonObject.getString("retCode");
        if(retCode.equals("0")){
            return success("保存成功");
        }else{
            String retMsg = retJsonObject.getString("retMsg");
            return failure(retMsg);
        }

    }

    /**
     * Describe: 推送对象管理单个删除
     * Param: id
     * Return: 文件
     */
    @DeleteMapping("remove/{manageId}")
    @ApiOperation(value = "推送对象管理单个删除",notes = "推送对象管理单个删除")
    @Log(title = "推送对象管理单个删除", describe = "推送对象管理单个删除", type = BusinessType.REMOVE)
    public Result remove(@PathVariable("manageId") int manageId) {
        boolean result = pushObjectManageService.remove(manageId);
        return Result.decide(result, "删除成功", "删除失败");
    }



    /**
     * 推送对象管理批量删除
     * @param manageIds
     * @return
     */
    @DeleteMapping("batchRemove/{manageIds}")
    @ApiOperation(value = "推送对象管理批量删除",notes = "推送对象管理批量删除")
    @Log(title = "推送对象管理批量删除", describe = "推送对象管理批量删除", type = BusinessType.REMOVE)
    public Result batchRemove(@PathVariable("manageIds") String manageIds) {
        for (String manageId : manageIds.split(CommonConstant.COMMA)) {
            int iManageId = Integer.parseInt(manageId);
            pushObjectManageService.remove(iManageId);
        }
        return Result.success("删除成功");
    }

    /**
     * 获取推送对象管理下拉框
     * @return
     */
    @GetMapping("getPushObjectManageSelect")
    @ApiOperation(value = "获取推送对象管理下拉框",notes = "获取推送对象管理下拉框")
    public Object getPushObjectManageSelect(){
        return  pushObjectManageService.getPushObjectManageSelect();
    }

    /**
     * 根据当前登录人的部门获取推送对象管理下拉框
     * @return
     */
    @GetMapping("getPushObjectByAreaSelect")
    @ApiOperation(value = "根据当前登录人的部门获取推送对象管理下拉框",notes = "根据当前登录人的部门获取推送对象管理下拉框")
    public Object getPushObjectByAreaSelect(){
        return  pushObjectManageService.getPushObjectByAreaSelect();
    }

    /**
     * 获取推送对象管理下拉框（选中值）
     * @return
     */
    @GetMapping("getPushObjectManageSelectSel")
    @ApiOperation(value = "获取推送对象管理下拉框（选中值）",notes = "获取推送对象管理下拉框（选中值）")
    public Object getPushObjectManageSelectSel(String manageId){
        return  pushObjectManageService.getPushObjectManageSelectSel(manageId);
    }

    /**
     * 获取推送对象管理领导下拉框（选中值）
     * @return
     */
    @GetMapping("getPushObjectManageLeaderSelectSel")
    @ApiOperation(value = "获取推送对象管理领导下拉框（选中值）",notes = "获取推送对象管理领导下拉框（选中值）")
    public Object getPushObjectManageLeaderSelectSel(String manageId){
        return  pushObjectManageService.getPushObjectManageLeaderSelectSel(manageId);
    }

    /**
     * 获取日报推送对象管理下拉框（选中值）
     * @return
     */
    @GetMapping("getRbPushObjectManageSelectSel")
    @ApiOperation(value = "获取日报推送对象管理下拉框（选中值）",notes = "获取日报推送对象管理下拉框（选中值）")
    public Object getRbPushObjectManageSelectSel(String manageId){
        return  pushObjectManageService.getRbPushObjectManageSelectSel(manageId);
    }

    /**
     * 获取日报推送领导下拉框（选中值）
     * @return
     */
    @GetMapping("getRbPushLeaderManageSelectSel")
    @ApiOperation(value = "获取日报推送领导下拉框（选中值）",notes = "获取日报推送领导下拉框（选中值）")
    public Object getRbPushLeaderManageSelectSel(String manageId){
        return  pushObjectManageService.getRbPushLeaderManageSelectSel(manageId);
    }


    /**
     * 标签分组一键绑定
     * @param param
     */
    @RequestMapping(value = "/binding", method = RequestMethod.POST)
    public JSONObject binding(@RequestBody String param) {
        return pushObjectManageService.binding(param);
    }

    /**
     * 组织维度一键绑定
     * @param param
     */
    @RequestMapping(value = "/zzwdBinding", method = RequestMethod.POST)
    public JSONObject zzwdBinding(@RequestBody String param) {
        return pushObjectManageService.zzwdBinding(param);
    }

    /**
     * 标签分组批量一键绑定
     * @param param
     */
    @RequestMapping(value = "/batchBinding", method = RequestMethod.POST)
    public JSONObject batchBinding(@RequestBody String param) {
        return pushObjectManageService.batchBinding(param);
    }

    /**
     * 组织维度批量一键绑定
     * @param param
     */
    @RequestMapping(value = "/zzwdBatchBinding", method = RequestMethod.POST)
    public JSONObject zzwdBatchBinding(@RequestBody String param) {
        return pushObjectManageService.zzwdBatchBinding(param);
    }

    /**
     * 获取当前登录人在推送对象中的信息
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/getPushObjectByLogin", method = RequestMethod.POST)
    @ApiOperation(value = "获取当前登录人在推送对象中的信息",notes = "获取当前登录人在推送对象中的信息")
    public JSONObject getPushObjectByLogin(){
        return pushObjectManageService.getPushObjectByLogin();
    }

    /**
     * 批量导入
     * @return
     */
    @GetMapping("batchImport")
    public ModelAndView batchImport(Model model) {
        return jumpPage(modulePath + "batchImport");
    }

    /**
     * IM推送对象批量导入模板下载
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/downloadModelFile", method = RequestMethod.GET)
    @ApiOperation(value = "IM推送对象批量导入模板下载",notes = "IM推送对象批量导入模板下载")
    @Log(title = "IM推送对象批量导入模板下载", describe = "IM推送对象批量导入模板下载", type = BusinessType.DOWNLOAD)
    public ResponseEntity<InputStreamResource> downloadModelFile() {
        return pushObjectManageService.downloadModelFile();
    }

    /**
     * IM推送对象管理批量导入数据
     * @param file
     * @return
     */
    @RequestMapping(value = "batchImportData")
    @ApiOperation(value = "IM推送对象管理批量导入数据",notes = "IM推送对象管理批量导入数据")
    @Log(title = "IM推送对象管理批量导入数据", describe = "IM推送对象管理批量导入数据", type = BusinessType.UPLOAD)
    @ResponseBody
    public Result batchImportData(@RequestParam("file") MultipartFile file) {
        boolean result = pushObjectManageService.batchImportData(file);
        if (result) {
            return Result.success(0, "上传成功", result);
        } else {
            return Result.failure("上传失败");
        }
    }

    /**
     * Describe: 获取组织维度树
     */
    @GetMapping("zzWdtree")
    public ResultTree zzWdtree() {
        List<WgDtreeData> data = pushObjectManageService.zzWdtree();
        return dataTree(data);
    }

    /**
     * Describe: 获取标签分组树
     */
    @GetMapping("bqfzTree")
    public ResultTree bqfzTree() {
        List<WgDtreeData> data = pushObjectManageService.bqfzTree();
        return dataTree(data);
    }

    /**
     * 标签分组树节点新增
     * @return
     */
    @GetMapping("bqfzTreeAdd")
    public ModelAndView bqfzTreeAdd(Model model) {
        return jumpPage(modulePath + "bqfzTreeAdd");
    }

    /**
     * 保存新增标签分组树节点
     * @param imPushOrg
     * @return
     */
    @Repeat
    @PostMapping("saveAddBqfzTree")
    @ApiOperation(value = "保存新增标签分组树节点",notes = "保存新增标签分组树节点")
    @Log(title = "保存新增标签分组树节点", describe = "保存新增标签分组树节点", type = BusinessType.ADD)
    public Result saveAddBqfzTree(@RequestBody ImPushOrg imPushOrg){
        JSONObject retJsonObject = pushObjectManageService.saveAddBqfzTree(imPushOrg);
        String retCode = retJsonObject.getString("retCode");
        if(retCode.equals("0")){
            return success("保存成功");
        }else{
            String retMsg = retJsonObject.getString("retMsg");
            return failure(retMsg);
        }

    }

    /**
     * Describe: 标签分组树节点删除
     * Param: id
     * Return: 文件
     */
    @DeleteMapping("bqfzTreeDel/{orgId}")
    @ApiOperation(value = "标签分组树节点删除",notes = "标签分组树节点删除")
    @Log(title = "标签分组树节点删除", describe = "标签分组树节点删除", type = BusinessType.REMOVE)
    public Object bqfzTreeDel(@PathVariable("orgId") String orgId) {
        return pushObjectManageService.bqfzTreeDel(orgId);
    }

    /**
     * 打开标签分组节点修改页面
     */
    @GetMapping("bqfzTreeEdit")
    @ApiOperation(value = "打开标签分组节点修改页面",notes = "打开标签分组节点修改页面")
    public ModelAndView bqfzTreeEdit(Model model,String orgId) {

        ImPushOrg imPushOrg = pushObjectManageService.getBqfzTreeNodeById(orgId);
        model.addAttribute("imPushOrg", imPushOrg);
        return jumpPage(modulePath + "bqfzTreeEdit");
    }

    /**
     * 保存修改标签分组树节点
     * @param imPushOrg
     * @return
     */
    @Repeat
    @PostMapping("saveEditBqfzTree")
    @ApiOperation(value = "保存修改标签分组树节点",notes = "保存修改标签分组树节点")
    @Log(title = "保存修改标签分组树节点", describe = "保存修改标签分组树节点", type = BusinessType.ADD)
    public Result saveEditBqfzTree(@RequestBody ImPushOrg imPushOrg){
        JSONObject retJsonObject = pushObjectManageService.saveEditBqfzTree(imPushOrg);
        String retCode = retJsonObject.getString("retCode");
        if(retCode.equals("0")){
            return success("保存成功");
        }else{
            String retMsg = retJsonObject.getString("retMsg");
            return failure(retMsg);
        }

    }


    /**
     * Describe: 获取组织维度树节点人员信息
     */
    @GetMapping("zzWdtreePerson")
    public ResultTree zzWdtreePerson() {
        List<WgDtreeData> data = pushObjectManageService.zzWdtreePerson();
        return dataTree(data);
    }

    /**
     * Describe: 获取标签分组树节点人员信息
     */
    @GetMapping("bqfzTreePerson")
    public ResultTree bqfzTreePerson() {
        List<WgDtreeData> data = pushObjectManageService.bqfzTreePerson();
        return dataTree(data);
    }

    /**
     * Describe: 获取标签分组树节点人员信息
     */
    @GetMapping("bqfzTreePersonSel")
    public ResultTree bqfzTreePersonSel(String pushObjectId) {
        List<WgDtreeData> data = pushObjectManageService.bqfzTreePersonSel(pushObjectId);
        return dataTree(data);
    }

    /**
     * Describe: 绑定所有组织维度下的人员
     */
    @GetMapping("zzwdBindingAll")
    public JSONObject zzwdBindingAll() {
        return pushObjectManageService.zzwdBindingAll();
    }

    /**
     * 标签分组一键绑定
     */
    @GetMapping("bqfzYjbd")
    public JSONObject bqfzYjbd() {
        return pushObjectManageService.bqfzYjbd();
    }


}
