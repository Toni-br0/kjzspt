package com.pearadmin.modules.im.service;

import com.alibaba.fastjson.JSONObject;
import com.pearadmin.common.aop.annotation.Log;
import com.pearadmin.common.aop.annotation.Repeat;
import com.pearadmin.common.aop.enums.BusinessType;
import com.pearadmin.common.web.domain.response.Result;
import com.pearadmin.modules.im.domain.ImPushRoleManage;
import com.pearadmin.modules.im.domain.ImPushRolePersonManage;
import com.pearadmin.modules.wgppt.domain.WgDtreeData;
import io.swagger.annotations.ApiOperation;
import org.apache.xmlbeans.impl.xb.xsdschema.Public;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/**
 *
 * 创建日期：2026-04-03
 * 推送角色管理
 **/

public interface IImPushRoleManageService {

    /**
     * Describe: 获取角色树
     * @return
     */
    public List<ImPushRoleManage>  getRoleTree();

    /**
     * 保存角色信息
     * @param imPushRoleManage
     * @return
     */
    public JSONObject saveRole(ImPushRoleManage imPushRoleManage);


    /**
     * Describe: 角色树节点删除
     * Param: id
     * Return: 文件
     */
    public Object roleDel(String roleId);

    /**
     * 根据主键ID获取相关信息
     * @param roleId
     * @return
     */
    public ImPushRoleManage getImPushRoleManageById(String roleId);

    /**
     * 保存编辑角色信息
     * @param imPushRoleManage
     * @return
     */
    public JSONObject saveRoleEdit(ImPushRoleManage imPushRoleManage);

    /**
     * 获取推送角色对应的人员信息表
     * @param imPushRolePersonManage
     * @return
     */
    public List<ImPushRolePersonManage> getRolePersonTableList(ImPushRolePersonManage imPushRolePersonManage);

    /**
     * 保存角色人员信息
     * @param imPushRolePersonManage
     * @return
     */
     public JSONObject personSave(ImPushRolePersonManage imPushRolePersonManage);

    /**
     * 编辑保存角色人员信息
     * @param imPushRolePersonManage
     * @return
     */
    public JSONObject personEditSave(ImPushRolePersonManage imPushRolePersonManage);

    /**
     * Describe: 角色下的推送对象单个删除
     * Param: id
     * Return: 文件
     */
    public boolean remove(String manageId);


    /**
     * 通过ID获取相关信息
     */
    public ImPushRolePersonManage getById(String manageId);


    /**
     * 角色推送对象批量导入模板下载
     * @return
     */
    public ResponseEntity<InputStreamResource> downloadModelFile();

    /**
     * 角色推送对象批量导入数据
     * @param file
     * @return
     */
    public boolean batchImportData(MultipartFile file,String roleId);

}
