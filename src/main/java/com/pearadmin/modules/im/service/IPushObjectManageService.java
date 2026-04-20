package com.pearadmin.modules.im.service;

import com.alibaba.fastjson.JSONObject;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.common.web.domain.response.Result;
import com.pearadmin.common.web.domain.response.module.ResultTree;
import com.pearadmin.modules.im.domain.ImPushObjectManage;
import com.pearadmin.modules.im.domain.ImPushOrg;
import com.pearadmin.modules.wgppt.domain.VWgzsUserLevelD;
import com.pearadmin.modules.wgppt.domain.WgDtreeData;
import io.swagger.annotations.ApiOperation;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 创建日期：2025-04-27
 * 推送对象管理
 **/

public interface IPushObjectManageService {

    /**
     * 获取标签分组推送对象列表
     * @param imPushObjectManage
     * @return
     */
    List<ImPushObjectManage> getPushObjectManageList(ImPushObjectManage imPushObjectManage);

    /**
     * 获取组织维度推送对象列表
     * @param imPushObjectManage
     * @return
     */
    List<VWgzsUserLevelD> getZzwdPushObjectManageList(ImPushObjectManage imPushObjectManage);


    /**
     * 保存推送对象
     * @param imPushObjectManage
     * @return
     */
    JSONObject save(ImPushObjectManage imPushObjectManage);

    /**
     * 根据ID查询推送对象信息
     * @param manageId
     * @return
     */
    ImPushObjectManage getById(int manageId);

    /**
     *  推送对象信息
     * @param manageId
     * @return
     */
    boolean remove(int manageId);

    /**
     * 获取推送对象管理下拉框
     * @return
     */
    Object getPushObjectManageSelect();

    /**
     * 根据当前登录人的部门获取推送对象管理下拉框
     * @return
     */
    Object getPushObjectByAreaSelect();

    /**
     * 获取推送对象管理下拉框（选中值）
     * @return
     */
    Object getPushObjectManageSelectSel(String manageId);

    /**
     * 获取推送对象管理领导下拉框（选中值）
     * @return
     */
    Object getPushObjectManageLeaderSelectSel(String manageId);

    /**
     * 标签分组一键绑定
     * @param param
     * @return
     */
    JSONObject binding(String param);

    /**
     * 组织维度一键绑定
     * @param param
     * @return
     */
    JSONObject zzwdBinding(String param);

    /**
     * 标签分组批量一键绑定
     * @param param
     * @return
     */
    JSONObject batchBinding(String param);

    /**
     * 组织维度批量一键绑定
     * @param param
     * @return
     */
    JSONObject zzwdBatchBinding(String param);

    /**
     * 获取日报推送对象管理下拉框（选中值）
     * @return
     */
    public Object getRbPushObjectManageSelectSel(String manageId);


    /**
     * 获取日报推送领导管理下拉框（选中值）
     * @return
     */
    public Object getRbPushLeaderManageSelectSel(String manageId);

    /**
     * 获取当前登录人在推送对象中的信息
     * @return
     * @throws IOException
     */

    public JSONObject getPushObjectByLogin();

    /**
     * 根据推送对象ID获取推送对象信息
     * @param objectId
     * @return
     */
    ImPushObjectManage getImPushObjectByObjectId(String objectId);


    /**
     * 批量导入
     * @return
     */
    public ResponseEntity<InputStreamResource> downloadModelFile();


    /**
     * 批量导入数据
     * @param file
     * Describe: 获取组织维度树
     * @return
     */
    public boolean batchImportData(MultipartFile file);


    /**
     * Describe: 获取组织维度树
     */
    public List<WgDtreeData> zzWdtree();

    /**
     * Describe: 获取标签分组树
     */
    public List<WgDtreeData> bqfzTree();

    /**
     * Describe: 保存新增标签分组树节点
     */
    public JSONObject saveAddBqfzTree(ImPushOrg imPushOrg);

    /**
     * 标签分组树节点删除
     */
    public JSONObject bqfzTreeDel(String orgId);

    /**
     * 根据ID查询标签分组节点信息
     * @param orgId
     * @return
     */
    public ImPushOrg getBqfzTreeNodeById(String orgId);


    /**
     * Describe: 保存修改标签分组树节点
     */
    public JSONObject saveEditBqfzTree(ImPushOrg imPushOrg);

    /**
     * Describe: 获取组织维度树节点人员信息
     */
    public List<WgDtreeData> zzWdtreePerson();

    /**
     * Describe: 获取标签分组树节点人员信息
     */
    public List<WgDtreeData> bqfzTreePerson();

    /**
     * Describe: 获取标签分组树节点人员信息
     */
    public List<WgDtreeData> bqfzTreePersonSel(String pushObjectId);

    /**
     * Describe: 绑定所有组织维度下的人员
     */
    public JSONObject zzwdBindingAll();

    /**
     * Describe: 标签分组一键绑定
     */
    public JSONObject bqfzYjbd();

}
