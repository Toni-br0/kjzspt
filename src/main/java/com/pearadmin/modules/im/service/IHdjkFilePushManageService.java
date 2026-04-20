package com.pearadmin.modules.im.service;

import com.alibaba.fastjson.JSONObject;
import com.pearadmin.modules.im.domain.ImHdjkPushManage;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 创建日期：2025-04-25
 * IM日报文件推送管理
 **/

public interface IHdjkFilePushManageService {

    /**
     * 获取IM文件推送管理列表
     * @param imRbFilePushManage
     * @return
     */
    List<ImHdjkPushManage> getImRbFilePushManageList(ImHdjkPushManage imRbFilePushManage);

    /**
     * 保存IM文件推送管理信息
     * @param imRbFilePushManage
     * @return
     */
    boolean save(ImHdjkPushManage imRbFilePushManage);

    /**
     * Describe: 文件推送管理单个删除
     */
     boolean remove(int manageId);

    /**
     * 根据id获取IM文件推送管理信息
     * @param manageId
     * @return
     */
    ImHdjkPushManage getById(int manageId);

    /**
     * 获取页面的模板下拉框
     * @return
     */
    Object getModelSelect();

    /**
     * 获取模板下拉树（选中值）
     * @return
     */
    Object getModelSelectSel(int modelId);

    /**
     * 在线预览PPT文件
     * @param param
     * @param response
     */
    JSONObject fileView(String param, HttpServletResponse response);

    /**
     * 文件下载
     * @param manageId
     * @return
     * @throws Exception
     */
    ResponseEntity<InputStreamResource> downloadFile(int manageId) throws Exception;


    /**
     * 一键推送
     * @param param
     */
    public JSONObject push(String param);

    /**
     * 一键推送
     * @param param
     */
    public JSONObject pushNew(String param);

    /**
     * 一键推送至领导
     * @param param
     */
    public JSONObject pushLeader(String param);

    /**
     * 一键推送至领导
     * @param param
     */
    public JSONObject pushLeaderNew(String param);


    /**
     * 批量推送
     * @param param
     * @return
     */
    public JSONObject batchPush(String param);

    /**
     * 批量推送
     * @param param
     * @return
     */
    public JSONObject batchPushNew(String param);

    /**
     * 批量推送至领导
     * @param param
     * @return
     */
    public JSONObject batchLeaderPush(String param);

    /**
     * 批量推送至领导
     * @param param
     * @return
     */
    public JSONObject batchLeaderPushNew(String param);


    /**
     * 文件上传
     * @param file
     * @return
     */
    public boolean upload(MultipartFile file, int manageId);

    /**
     * 校验文件生成时间
     * @param param
     */
    public JSONObject checkFileCreateTime(String param);

}
