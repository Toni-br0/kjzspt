package com.pearadmin.modules.im.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.context.UserContext;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.im.domain.ImFilePushManage;
import com.pearadmin.modules.im.domain.ImOperateLog;
import com.pearadmin.modules.im.domain.ImPushObjectManage;
import com.pearadmin.modules.im.mapper.ImFilePushManageMapper;
import com.pearadmin.modules.im.mapper.ImOperateLogMapper;
import com.pearadmin.modules.im.mapper.ImPushObjectManageMapper;
import com.pearadmin.modules.im.service.IFilePushManageService;
import com.pearadmin.modules.im.util.ImUtil;
import com.pearadmin.modules.ppt.domain.PptModelConfig;
import com.pearadmin.modules.search.util.FileToPdfUtils;
import com.pearadmin.modules.sys.domain.SysConfig;
import com.pearadmin.modules.sys.domain.SysUser;
import com.pearadmin.modules.sys.mapper.SysConfigMapper;
import com.pearadmin.modules.sys.service.impl.SysUserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * 创建日期：2025-04-25
 * IM文件推送管理
 **/

@Service
@Slf4j
public class FilePushManageServiceImpl implements IFilePushManageService {

    @Value("${im-file-topdf-path}")
    private String imFileTopdfPath;

    @Value("${ppt-target-path}")
    private String pptTargetPath;

    @Resource
    private ImFilePushManageMapper imFilePushManageMapper;

    @Resource
    private Environment env;

    @Resource
    private ImUtil imUtil;

    @Resource
    private ImOperateLogMapper imOperateLogMapper;

    @Resource
    private ImPushObjectManageMapper imPushObjectManageMapper;

    @Resource
    private SysUserServiceImpl sysUserService;

    @Resource
    private SysConfigMapper sysConfigMapper;

    /**
     * 获取IM文件推送管理列表
     * @param imFilePushManage
     * @return
     */
    @Override
    public List<ImFilePushManage> getImFilePushManageList(ImFilePushManage imFilePushManage) {
        QueryWrapper<ImFilePushManage> queryWrapper = new QueryWrapper<>();
        if(StringUtil.isNotEmpty(imFilePushManage.getModelName())){
            queryWrapper.like("model_name", imFilePushManage.getModelName());
        }
        if(StringUtil.isNotEmpty(imFilePushManage.getFileName())){
            queryWrapper.like("file_name", imFilePushManage.getFileName());
        }

        queryWrapper.orderByDesc("file_creat_time");
        List<ImFilePushManage> list = imFilePushManageMapper.selectList(queryWrapper);

        if(list !=null && list.size() >0){
            //推送对象是否带电话号码
            String pushPersonPhoneValue ="0";
            SysConfig sysConfig = sysConfigMapper.selectByCode("push_person_phone");
            if(sysConfig != null && sysConfig.getConfigValue().equals("1")){
                pushPersonPhoneValue ="1";
            }

            for(ImFilePushManage queryImFilePushManage : list){
            //拼接推送对象名称
            String pushObjectId = queryImFilePushManage.getPushObjectId();
            String[] pushObjectIdArr = pushObjectId.split(";");
            String pushObjectName = "";

            String pushPhone ="";
            for(int i=0;i<pushObjectIdArr.length;i++){
                String strPushObjectId = pushObjectIdArr[i];
                int iPushObjectId = Integer.parseInt(strPushObjectId);
                ImPushObjectManage imPushObjectManage = imPushObjectManageMapper.selectById(iPushObjectId);
                if(imPushObjectManage != null){
                    if(pushPersonPhoneValue.equals("1")){
                        pushPhone = "["+imPushObjectManage.getPushObjectId()+"]";
                    }
                    if(!pushObjectName.equals("")){
                        pushObjectName = pushObjectName+";"+imPushObjectManage.getPushObjectName()+pushPhone;
                    }else{
                        pushObjectName = imPushObjectManage.getPushObjectName()+pushPhone;
                    }

                }
            }

            queryImFilePushManage.setPushObjectName(pushObjectName);

            //拼接推送对象领导名称
            String pushObjectLeaderId = queryImFilePushManage.getPushObjectLeaderId();
            if(StringUtil.isNotEmpty(pushObjectLeaderId)){
                String[] pushObjectLeaderIdArr = pushObjectLeaderId.split(";");
                String pushObjectLeaderName = "";
                String pushLeaderPhone ="";
                for(int i=0;i<pushObjectLeaderIdArr.length;i++){
                    String strPushObjectLeaderId = pushObjectLeaderIdArr[i];
                    int iPushObjectLeaderId = Integer.parseInt(strPushObjectLeaderId);
                    ImPushObjectManage imPushObjectManage = imPushObjectManageMapper.selectById(iPushObjectLeaderId);
                    if(imPushObjectManage != null){

                        if(pushPersonPhoneValue.equals("1")){
                            pushLeaderPhone = "["+imPushObjectManage.getPushObjectId()+"]";
                        }

                        if(!pushObjectLeaderName.equals("")){
                            pushObjectLeaderName = pushObjectLeaderName+";"+imPushObjectManage.getPushObjectName()+pushLeaderPhone;
                        }else{
                            pushObjectLeaderName = imPushObjectManage.getPushObjectName()+pushLeaderPhone;
                        }

                    }
                }

                queryImFilePushManage.setPushObjectLeaderName(pushObjectLeaderName);
            }
         }
        }
        return list;
    }

    /**
     * 保存IM文件推送管理信息
     * @param imFilePushManage
     * @return
     */
    @Override
    public boolean save(ImFilePushManage imFilePushManage) {
        int result = 0;
        // 判断是否存在该ManageId的记录，如果存在则更新，不存在则插入新的记录
        if(imFilePushManage.getManageId() != null){
            //imFilePushManage.setFileCreatTime(LocalDateTime.now());

            ImFilePushManage queryImFilePushManage = imFilePushManageMapper.selectById(imFilePushManage.getManageId());
            if(queryImFilePushManage != null){

                queryImFilePushManage.setModelId(imFilePushManage.getModelId());
                queryImFilePushManage.setModelName(imFilePushManage.getModelName());

                queryImFilePushManage.setPushObjectId(imFilePushManage.getPushObjectId());
                queryImFilePushManage.setPushObjectName(imFilePushManage.getPushObjectName());
                queryImFilePushManage.setIsAutoPush(imFilePushManage.getIsAutoPush());
                queryImFilePushManage.setPushObjectLeaderId(imFilePushManage.getPushObjectLeaderId());
                queryImFilePushManage.setPustTxtMsg(imFilePushManage.getPustTxtMsg());
                queryImFilePushManage.setIsPushTxtMsg(imFilePushManage.getIsPushTxtMsg());

                result = imFilePushManageMapper.updateById(queryImFilePushManage);
            }

        }else{
            QueryWrapper<ImFilePushManage> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("model_id", imFilePushManage.getModelId());
            ImFilePushManage queryImFilePushManage = imFilePushManageMapper.selectOne(queryWrapper);
            if(queryImFilePushManage != null){
                queryImFilePushManage.setPushObjectId(imFilePushManage.getPushObjectId());
                queryImFilePushManage.setPushObjectName(imFilePushManage.getPushObjectName());
                queryImFilePushManage.setIsAutoPush(imFilePushManage.getIsAutoPush());
                queryImFilePushManage.setPushObjectLeaderId(imFilePushManage.getPushObjectLeaderId());
                queryImFilePushManage.setPustTxtMsg(imFilePushManage.getPustTxtMsg());
                queryImFilePushManage.setIsPushTxtMsg(imFilePushManage.getIsPushTxtMsg());
                //queryImFilePushManage.setFileCreatTime(LocalDateTime.now());
                result = imFilePushManageMapper.updateById(queryImFilePushManage);
            }else{
                //imFilePushManage.setFileCreatTime(LocalDateTime.now());
                result = imFilePushManageMapper.insert(imFilePushManage);
            }
        }

        if(result > 0){
            return true;
        }else{
            return false;
        }
    }


    /**
     * Describe: 文件推送管理单个删除
     */
    @Override
    public boolean remove(int manageId) {
        int result = imFilePushManageMapper.deleteById(manageId);
        if(result > 0){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 根据id获取IM文件推送管理信息
     * @param manageId
     * @return
     */
    @Override
    public ImFilePushManage getById(int manageId) {
        return imFilePushManageMapper.selectById(manageId);
    }


    /**
     * 获取页面的模板下拉框
     * @return
     */
    @Override
    public Object getModelSelect() {
        //模板
        List<PptModelConfig> modelsList = imFilePushManageMapper.getModelList();
        List<JSONObject> retList = new ArrayList<>();

        if(modelsList !=null && modelsList.size() >0){
            String modelType ="";
            String disModelType ="";
            String pptName ="";
            for(PptModelConfig pptModelConfig : modelsList){
                pptName = pptModelConfig.getPptName()==null?"":pptModelConfig.getPptName().trim();
                modelType = pptModelConfig.getModelType();
                if(modelType.equals("1")){
                    disModelType = "月";
                }else if(modelType.equals("2")){
                    disModelType = "周";
                }else if(modelType.equals("3")){
                    disModelType = "日";
                }

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name","["+pptModelConfig.getModelLevel()+"_"+disModelType+"]-"+pptModelConfig.getModelName());
                jsonObject.put("value",pptModelConfig.getModelId());
                jsonObject.put("level",pptModelConfig.getModelLevel());
                jsonObject.put("modelType",modelType);
                jsonObject.put("pptName",pptName);
                jsonObject.put("modelName",pptModelConfig.getModelName());
                retList.add(jsonObject);

            }
        }

        return retList;
    }


    /**
     * 获取模板下拉树（选中值）
     * @param modelId
     * @return
     */
    @Override
    public Object getModelSelectSel(int modelId) {
        //模板
        List<PptModelConfig> modelsList = imFilePushManageMapper.getModelSelList(modelId);
        List<JSONObject> retList = new ArrayList<>();

        //String[] modelIds = modelId.split(",");

        if(modelsList !=null && modelsList.size() >0){
            for(PptModelConfig pptModelConfig : modelsList){
                int modelIdValue = pptModelConfig.getModelId();

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name",pptModelConfig.getModelName());
                jsonObject.put("value",pptModelConfig.getModelId());
                jsonObject.put("level",pptModelConfig.getModelLevel());

                if(modelId == modelIdValue){
                    jsonObject.put("selected",true);
                }

                /*if(modelIds.length >0){
                    for(String strModelId : modelIds){
                        //String eqStrModelId = "("+modelIdValue+")";
                        String eqStrModelId = modelIdValue+"";
                        if(strModelId.equals(eqStrModelId)){
                            jsonObject.put("selected",true);
                            break;
                        }
                    }
                }*/

                retList.add(jsonObject);
            }
        }

        return retList;
    }

    /**
     * 在线预览PPT文件
     * @param param
     * @param response
     */
    @Override
    public JSONObject fileView(String param, HttpServletResponse response) {
        JSONObject retJson  = new JSONObject();
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            int manageId = paramJson.getInteger("manageId");
            ImFilePushManage imFilePushManage = imFilePushManageMapper.selectById(manageId);
            if(imFilePushManage != null){

                String path = imFilePushManage.getFilePath();
                String fileName = imFilePushManage.getFileName();
                String pdfPath = imFileTopdfPath;
                FileToPdfUtils.officeToPdf(path,pdfPath);
                String port = env.getProperty("server.port");
                String serverIp = env.getProperty("server.server-ip");

                String pdfName = fileName.substring(0,fileName.lastIndexOf(".")+1)+"pdf";
                String retPdfPath =serverIp+":"+port+"/imFileTopdf/"+pdfName;

                retJson.put("retCode","0");
                retJson.put("retMsg",retPdfPath);
            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","未查询到文件信息！");
            }

        }catch (Exception e) {
            e.printStackTrace();
            retJson.put("retCode","500");
            retJson.put("retMsg","系统异常！");
        }

        return retJson;
    }


    /**
     * 文件下载
     * @param manageId
     * @return
     * @throws Exception
     */
    @Override
    public ResponseEntity<InputStreamResource> downloadFile(int manageId) throws Exception {
        log.info("进入下载方法... manageId: {}",manageId);
        String filePath = "";
        ImFilePushManage imFilePushManage = imFilePushManageMapper.selectById(manageId);
        if(imFilePushManage != null){
            filePath = imFilePushManage.getFilePath();
        }
        //读取文件
        //String filePath = searchUploadPath + fileName;


        /*FileSystemResource file = new FileSystemResource(filePath);

        String encodedFilename = new String(file.getFilename().getBytes("UTF-8"), "ISO-8859-1");

        log.info("进入下载方法... encodedFilename: {}",encodedFilename);

        //设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Content-Disposition", "attachment; filename=\"" + encodedFilename + "\"");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentLength(file.contentLength())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(new InputStreamResource(file.getInputStream()));*/

        FileSystemResource file = new FileSystemResource(filePath);
        String filename = file.getFilename();

        HttpHeaders headers = new HttpHeaders();
        // 确保不缓存
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        // 处理特殊字符的文件名
        String encodedFileName = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString());
        log.info("进入下载方法... encodedFilename: {}",encodedFileName);
        headers.add("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

        try {
            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentLength(file.contentLength())
                    .contentType(MediaType.parseMediaType("application/octet-stream"))
                    .body(new InputStreamResource(file.getInputStream()));
        }catch (Exception ex){
            ex.printStackTrace();
            return null;
        }

    }

    /**
     * 一键绑定
     * @param param
     * @return
     */
    @Override
    public JSONObject binding(String param) {
        JSONObject retJson  = new JSONObject();
        ImOperateLog imOperateLog = new ImOperateLog();
        ImFilePushManage imFilePushManage = null;
        int manageId =0;
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            manageId = paramJson.getInteger("manageId");
            imFilePushManage = imFilePushManageMapper.selectById(manageId);
            if(imFilePushManage != null){
                String pushObjectId = imFilePushManage.getPushObjectId();
                if(StringUtil.isNotEmpty(pushObjectId)){
                    //[18160546373,18999933998];
                    pushObjectId = pushObjectId.replace("[","").replace("]","");
                    pushObjectId = pushObjectId.replace(",",";");

                    String[] arrPushObjectId = pushObjectId.split(";");
                    if(arrPushObjectId != null && arrPushObjectId.length >0){
                        for(String strPushObjectId : arrPushObjectId){
                            //执行绑定操作
                            String bindingResult = imUtil.relBind(strPushObjectId);
                            log.info("++++++++++[{}]绑定结果：{}",strPushObjectId,bindingResult);
                            if(StringUtil.isNotEmpty(bindingResult)){
                                JSONObject bindingJson = JSONObject.parseObject(bindingResult);
                                String status = bindingJson.getString("status");
                                //绑定成功
                                if(StringUtil.isNotEmpty(bindingResult) && "success".equals(status)){

                                    retJson.put("retCode","0");
                                    retJson.put("retMsg","绑定成功！");
                                }else{
                                    retJson.put("retCode","-1");
                                    retJson.put("retMsg","["+strPushObjectId+"]绑定失败，请检查绑定对象是否存在！");
                                    break;
                                }
                            }else{
                                retJson.put("retCode","-1");
                                retJson.put("retMsg","IM绑定接口返回空");
                            }
                        }
                    }else{
                        retJson.put("retCode","-1");
                        retJson.put("retMsg","绑定失败，请检查是否设置绑定对象!");
                    }
                }else{
                    retJson.put("retCode","-1");
                    retJson.put("retMsg","绑定失败，请检查是否设置绑定对象!");
                }
            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","绑定失败，根据["+manageId+"]未查询到相关信息");
            }
        }catch (Exception e) {
            e.printStackTrace();
            retJson.put("retCode","500");
            retJson.put("retMsg","系统异常,请稍后再试！");
        }finally {
            if(imFilePushManage != null){
                imOperateLog.setOperateType("bind");
                imOperateLog.setModelId(imFilePushManage.getModelId());
                imOperateLog.setModelName(imFilePushManage.getModelName());
                imOperateLog.setFileName(imFilePushManage.getFileName());
                imOperateLog.setObjectId(imFilePushManage.getPushObjectId());
                imOperateLog.setObjectName(imFilePushManage.getPushObjectName());
                imOperateLog.setFileCreatTime(imFilePushManage.getFileCreatTime());
                imOperateLog.setOperateTime(LocalDateTime.now());
                imOperateLog.setFilePath(imFilePushManage.getFilePath());
                imOperateLog.setManageId(manageId);

                String retCode = retJson.getString("retCode");
                String retMsg = retJson.getString("retMsg");
                if(StringUtil.isNotEmpty(retMsg) && retMsg.length()>1000){
                    retMsg = retMsg.substring(0,1000);
                }

                imOperateLog.setIsSuccess(retCode);
                imOperateLog.setOperateResult(retMsg);

                //获取当前登录用户信息
                SysUser currentUser = UserContext.currentUser();
                String realName = currentUser.getRealName();
                imOperateLog.setOperatePerson(realName);

                imOperateLogMapper.insert(imOperateLog);
            }
        }
        return retJson;
    }

    /**
     * 批量一键绑定
     * @param param
     * @return
     */
    @Override
    public JSONObject batchBinding(String param) {
        JSONObject retJson  = new JSONObject();
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            String manageIds = paramJson.getString("manageIds");
            if(StringUtil.isNotEmpty(manageIds)){
                String[] arrManageIds = manageIds.split(",");
                if(arrManageIds != null && arrManageIds.length >0){
                    boolean isNext = false;
                    for(int i=0;i<arrManageIds.length && !isNext;i++){
                        String manageId = arrManageIds[i];
                        int iManageId = Integer.parseInt(manageId);
                        ImFilePushManage imFilePushManage = imFilePushManageMapper.selectById(iManageId);
                        if(imFilePushManage != null){
                            String pushObjectId = imFilePushManage.getPushObjectId();
                            if(StringUtil.isNotEmpty(pushObjectId)){
                                pushObjectId = pushObjectId.replace("[","").replace("]","");
                                pushObjectId = pushObjectId.replace(",",";");
                                String[] arrPushObjectId = pushObjectId.split(";");
                                if(arrPushObjectId != null && arrPushObjectId.length >0){
                                    for(String strPushObjectId : arrPushObjectId){
                                        //执行绑定操作
                                        String bindingResult = imUtil.relBind(strPushObjectId);
                                        log.info("++++++++++[{}]绑定结果：{}",strPushObjectId,bindingResult);
                                        if(StringUtil.isNotEmpty(bindingResult)){
                                            JSONObject bindingJson = JSONObject.parseObject(bindingResult);
                                            String status = bindingJson.getString("status");
                                            //绑定成功
                                            if(StringUtil.isNotEmpty(bindingResult) && "success".equals(status)){

                                                retJson.put("retCode","0");
                                                retJson.put("retMsg","绑定成功！");
                                            }else{
                                                retJson.put("retCode","-1");
                                                retJson.put("retMsg","["+strPushObjectId+"]绑定失败，请检查绑定对象是否存在！");
                                                isNext = true;
                                                break;
                                            }
                                        }else{
                                            retJson.put("retCode","-1");
                                            retJson.put("retMsg","绑定接口返回空");
                                            isNext = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }else{
                            retJson.put("retCode","-1");
                            retJson.put("retMsg","根据ID["+iManageId+"]未查询到相关信息！");
                            break;
                        }

                        //保存操作日志
                        if(imFilePushManage != null){
                            ImOperateLog imOperateLog = new ImOperateLog();
                            imOperateLog.setOperateType("bind");
                            imOperateLog.setModelId(imFilePushManage.getModelId());
                            imOperateLog.setModelName(imFilePushManage.getModelName());
                            imOperateLog.setFileName(imFilePushManage.getFileName());
                            imOperateLog.setObjectId(imFilePushManage.getPushObjectId());
                            imOperateLog.setObjectName(imFilePushManage.getPushObjectName());
                            imOperateLog.setFileCreatTime(imFilePushManage.getFileCreatTime());
                            imOperateLog.setOperateTime(LocalDateTime.now());
                            imOperateLog.setFilePath(imFilePushManage.getFilePath());
                            imOperateLog.setManageId(iManageId);

                            String retCode = retJson.getString("retCode");
                            String retMsg = retJson.getString("retMsg");
                            if(StringUtil.isNotEmpty(retMsg) && retMsg.length()>1000){
                                retMsg = retMsg.substring(0,1000);
                            }

                            imOperateLog.setIsSuccess(retCode);
                            imOperateLog.setOperateResult(retMsg);

                            //获取当前登录用户信息
                            SysUser currentUser = UserContext.currentUser();
                            String realName = currentUser.getRealName();
                            imOperateLog.setOperatePerson(realName);

                            imOperateLogMapper.insert(imOperateLog);
                        }
                    }
                }
            }

        }catch (Exception e) {
            e.printStackTrace();
            retJson.put("retCode","-1");
            retJson.put("retMsg","系统异常，请稍后再试！");
        }
        return retJson;
    }

    /**
     * 一键推送
     * @param param
     * @return
     */
    @Override
    public JSONObject push(String param) {

        JSONObject retJson  = new JSONObject();
        ImOperateLog imOperateLog = new ImOperateLog();
        ImFilePushManage imFilePushManage = null;
        FileInputStream inputStream = null;
        int manageId =0;
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            manageId = paramJson.getInteger("manageId");
            imFilePushManage = imFilePushManageMapper.selectById(manageId);
            if(imFilePushManage != null){
                String pushObjectId = imFilePushManage.getPushObjectId();
                String filePath = imFilePushManage.getFilePath();
                String fileName = imFilePushManage.getFileName();

                if(StringUtil.isNotEmpty(pushObjectId)){
                    //字符数组转 整型List
                    String[] pushObjectIdArr = pushObjectId.split(";");
                    List<Integer> pushObjectIdList = new ArrayList<>();
                    for(int i=0;i<pushObjectIdArr.length;i++){
                        String strPushObjectId = pushObjectIdArr[i];
                        pushObjectIdList.add(Integer.parseInt(strPushObjectId));
                    }
                    
                    String lastPushObjectId ="";

                    //根据推送对象管理id集合，查询推送对象集合
                    List<ImPushObjectManage> imPushObjectManageList = imPushObjectManageMapper.selectListByList(pushObjectIdList);
                    if(imPushObjectManageList !=null && imPushObjectManageList.size() > 0){
                        //拼接推送对象id
                        //用户对象id 格式为 [user1,user2,user3]
                        //群对象id 格式为 group1;group2;group3
                        //最终推送格式为 用户对象id + 群对象id
                        String userPushObjectId = "";
                        String groupPushObjectId = "";
                        int userIndex =0;
                        int groupIndex =0;
                        int index =0;
                        String pushObjectName ="";
                        for(ImPushObjectManage imPushObjectManage : imPushObjectManageList){
                            String pustObjectType = imPushObjectManage.getPushObjectType();
                            String queryPushObjectName = imPushObjectManage.getPushObjectName();
                            LocalDateTime bindTime = imPushObjectManage.getBindTime();

                            if(index ==0){
                                pushObjectName = pushObjectName + imPushObjectManage.getPushObjectName();
                            }else{
                                pushObjectName = pushObjectName +";"+ imPushObjectManage.getPushObjectName();
                            }

                            imFilePushManage.setPushObjectName(pushObjectName);
                            index++;

                            if(bindTime ==null){
                                retJson.put("retCode","-1");
                                retJson.put("retMsg","推送对象["+queryPushObjectName+"]未绑定");
                                return retJson;

                            }

                            if(pustObjectType.equals("users")){ //用户
                                if(userIndex ==0){
                                    userPushObjectId = userPushObjectId +imPushObjectManage.getPushObjectId();
                                }else{
                                    userPushObjectId = userPushObjectId +","+imPushObjectManage.getPushObjectId();
                                }

                                userIndex++;
                            }else if(pustObjectType.equals("group")){ //群
                                if(groupIndex ==0){
                                    groupPushObjectId = groupPushObjectId +imPushObjectManage.getPushObjectId();
                                }else{
                                    groupPushObjectId = groupPushObjectId +";"+imPushObjectManage.getPushObjectId();
                                }

                                groupIndex++;
                            }
                        }

                        if(!userPushObjectId.equals("")){
                            userPushObjectId = "["+userPushObjectId+"]";
                            lastPushObjectId = lastPushObjectId + userPushObjectId;
                        }

                        if(!groupPushObjectId.equals("")){
                            if(userPushObjectId.equals("")){
                                lastPushObjectId = lastPushObjectId + groupPushObjectId;
                            }else{
                                lastPushObjectId = lastPushObjectId +";"+ groupPushObjectId;
                            }
                        }

                    }


                    String[] arrPushObjectId = lastPushObjectId.split(";");
                    if(arrPushObjectId != null && arrPushObjectId.length >0){

                        File file = new File(filePath);
                        inputStream = new FileInputStream(file);
                        String originalFilename = file.getName();
                        String contentType = "application/octet-stream"; // 根据文件类型修改
                        MultipartFile multipartFile = new MockMultipartFile("file", originalFilename, contentType, inputStream);

                        //执行推送操作 sendImFile(MultipartFile file,String targetType,String target,String fileType)
                        String targetType = "";
                        String target = "";
                        String fileType ="";
                        if(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".bmp")
                                || fileName.endsWith(".gif") || fileName.endsWith(".svg") || fileName.endsWith(".png")){
                            fileType = "pushImg";
                        }else{
                            fileType = "pushFile";
                        }

                        for(String strPushObjectId : arrPushObjectId){
                            target = strPushObjectId;
                            //用户
                            if(strPushObjectId.indexOf("[") >=0 && strPushObjectId.indexOf("]") >=0){
                                targetType = "users";
                            }else{
                                targetType = "group";
                            }


                            String sendResult = imUtil.sendImFile(multipartFile,targetType,target,fileType);
                            log.info("++++++++++{}_推送结果：{}",fileName,sendResult);
                            if(StringUtil.isNotEmpty(sendResult)){
                                JSONObject bindingJson = JSONObject.parseObject(sendResult);
                                String status = bindingJson.getString("status");
                                //推送成功
                                if(StringUtil.isNotEmpty(status) && "success".equals(status)){
                                    imFilePushManage.setFilePushTime(LocalDateTime.now());
                                    imFilePushManageMapper.updateById(imFilePushManage);
                                    retJson.put("retCode","0");
                                    retJson.put("retMsg","推送成功！");
                                    retJson.put("addRetMsg","推送成功："+sendResult);
                                }else{
                                    retJson.put("retCode","-1");
                                    retJson.put("retMsg","推送失败!");
                                    retJson.put("addRetMsg","推送失败："+sendResult);
                                    break;
                                }
                            }else{
                                retJson.put("retCode","-1");
                                retJson.put("retMsg","推送接口返回空");
                                retJson.put("addRetMsg","推送接口返回空");
                                break;
                            }
                        }
                    }else{
                        retJson.put("retCode","-1");
                        retJson.put("retMsg","推送失败，请检查是否设置绑定对象");
                        retJson.put("addRetMsg","推送失败，请检查是否设置绑定对象");
                    }
                }else{
                    retJson.put("retCode","-1");
                    retJson.put("retMsg","推送失败，请检查是否设置绑定对象");
                    retJson.put("addRetMsg","推送失败，请检查是否设置绑定对象");
                }
            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","推送失败，根据["+manageId+"]未查询到相关信息");
                retJson.put("addRetMsg","推送失败，根据["+manageId+"]未查询到相关信息");
            }
        }catch (Exception e) {
            e.printStackTrace();
            retJson.put("retCode","500");
            retJson.put("retMsg","系统异常,请稍后再试！");
            retJson.put("addRetMsg","系统异常: "+e.getMessage());
        }finally {
            //保存操作日志
            if(imFilePushManage != null){
                saveImOperateLog(imFilePushManage,retJson,"push",manageId);
            }

            if(inputStream != null){
                try {
                    inputStream.close();
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return retJson;

    }


    /**
     * 一键推送至领导
     * @param param
     * @return
     */
    @Override
    public JSONObject pushLeader(String param) {

        JSONObject retJson  = new JSONObject();
        ImOperateLog imOperateLog = new ImOperateLog();
        ImFilePushManage imFilePushManage = null;
        FileInputStream inputStream = null;
        int manageId =0;
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            manageId = paramJson.getInteger("manageId");
            imFilePushManage = imFilePushManageMapper.selectById(manageId);
            if(imFilePushManage != null){
                String pushObjectId = imFilePushManage.getPushObjectLeaderId();
                String filePath = imFilePushManage.getFilePath();
                String fileName = imFilePushManage.getFileName();
                String isPushTxtMsg = imFilePushManage.getIsPushTxtMsg();
                String pustTxtMsg = imFilePushManage.getPustTxtMsg();

                if(StringUtil.isNotEmpty(pushObjectId)){
                    //字符数组转 整型List
                    String[] pushObjectIdArr = pushObjectId.split(";");
                    List<Integer> pushObjectIdList = new ArrayList<>();
                    for(int i=0;i<pushObjectIdArr.length;i++){
                        String strPushObjectId = pushObjectIdArr[i];
                        pushObjectIdList.add(Integer.parseInt(strPushObjectId));
                    }

                    String lastPushObjectId ="";

                    //根据推送对象管理id集合，查询推送对象集合
                    List<ImPushObjectManage> imPushObjectManageList = imPushObjectManageMapper.selectListByList(pushObjectIdList);
                    if(imPushObjectManageList !=null && imPushObjectManageList.size() > 0){
                        //拼接推送对象id
                        //用户对象id 格式为 [user1,user2,user3]
                        //群对象id 格式为 group1;group2;group3
                        //最终推送格式为 用户对象id + 群对象id
                        String userPushObjectId = "";
                        String groupPushObjectId = "";
                        int userIndex =0;
                        int groupIndex =0;
                        int index =0;
                        String pushObjectName ="";
                        for(ImPushObjectManage imPushObjectManage : imPushObjectManageList){
                            String pustObjectType = imPushObjectManage.getPushObjectType();
                            String queryPushObjectName = imPushObjectManage.getPushObjectName();
                            LocalDateTime bindTime = imPushObjectManage.getBindTime();


                            if(index ==0){
                                pushObjectName = pushObjectName + imPushObjectManage.getPushObjectName();
                            }else{
                                pushObjectName = pushObjectName +";"+ imPushObjectManage.getPushObjectName();
                            }

                            imFilePushManage.setPushObjectName(pushObjectName);
                            index++;

                            if(bindTime ==null){
                                retJson.put("retCode","-1");
                                retJson.put("retMsg","推送对象["+queryPushObjectName+"]未绑定");
                                return retJson;

                            }

                            if(pustObjectType.equals("users")){ //用户
                                if(userIndex ==0){
                                    userPushObjectId = userPushObjectId +imPushObjectManage.getPushObjectId();
                                }else{
                                    userPushObjectId = userPushObjectId +","+imPushObjectManage.getPushObjectId();
                                }

                                userIndex++;
                            }else if(pustObjectType.equals("group")){ //群
                                if(groupIndex ==0){
                                    groupPushObjectId = groupPushObjectId +imPushObjectManage.getPushObjectId();
                                }else{
                                    groupPushObjectId = groupPushObjectId +";"+imPushObjectManage.getPushObjectId();
                                }

                                groupIndex++;
                            }
                        }

                        if(!userPushObjectId.equals("")){
                            userPushObjectId = "["+userPushObjectId+"]";
                            lastPushObjectId = lastPushObjectId + userPushObjectId;
                        }

                        if(!groupPushObjectId.equals("")){
                            if(userPushObjectId.equals("")){
                                lastPushObjectId = lastPushObjectId + groupPushObjectId;
                            }else{
                                lastPushObjectId = lastPushObjectId +";"+ groupPushObjectId;
                            }
                        }

                    }


                    String[] arrPushObjectId = lastPushObjectId.split(";");
                    if(arrPushObjectId != null && arrPushObjectId.length >0){

                        File file = new File(filePath);
                        inputStream = new FileInputStream(file);
                        String originalFilename = file.getName();
                        String contentType = "application/octet-stream"; // 根据文件类型修改
                        MultipartFile multipartFile = new MockMultipartFile("file", originalFilename, contentType, inputStream);

                        //执行推送操作 sendImFile(MultipartFile file,String targetType,String target,String fileType)
                        String targetType = "";
                        String target = "";
                        String fileType ="";
                        if(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".bmp")
                                || fileName.endsWith(".gif") || fileName.endsWith(".svg") || fileName.endsWith(".png")){
                            fileType = "pushImg";
                        }else{
                            fileType = "pushFile";
                        }

                        for(String strPushObjectId : arrPushObjectId){
                            target = strPushObjectId;
                            //用户
                            if(strPushObjectId.indexOf("[") >=0 && strPushObjectId.indexOf("]") >=0){
                                targetType = "users";
                            }else{
                                targetType = "group";
                            }

                            String sendResult = imUtil.sendImFile(multipartFile,targetType,target,fileType);
                            log.info("++++++++++{}_文件推送结果：{}",fileName,sendResult);
                            if(StringUtil.isNotEmpty(sendResult)){
                                JSONObject bindingJson = JSONObject.parseObject(sendResult);
                                String status = bindingJson.getString("status");
                                //推送成功
                                if(StringUtil.isNotEmpty(status) && "success".equals(status)){

                                    //判断是否要推送文字信息
                                    if(isPushTxtMsg.equals("1") && StringUtil.isNotEmpty(pustTxtMsg)){

                                        //休眠3秒
                                        try {
                                            log.info("++++++++++{}_3秒后开始推送文本消息",fileName);
                                            Thread.sleep(3000);
                                        }catch (InterruptedException ex) {
                                            Thread.currentThread().interrupt(); // 重新设置中断状态
                                        }

                                       String sendMsgResult = imUtil.sendImMsg(pustTxtMsg,targetType,target);
                                       log.info("++++++++++{}_文字信息推送结果：{}",fileName,sendMsgResult);
                                       if(StringUtil.isNotEmpty(sendMsgResult)){
                                           JSONObject sendMsgJson = JSONObject.parseObject(sendMsgResult);
                                           String sendMsgStatus = sendMsgJson.getString("status");
                                           //推送成功
                                           if(StringUtil.isNotEmpty(sendMsgStatus) && "success".equals(sendMsgStatus)){
                                               imFilePushManage.setFilePushTime(LocalDateTime.now());
                                               imFilePushManageMapper.updateById(imFilePushManage);

                                               retJson.put("retCode","0");
                                               retJson.put("retMsg","推送成功！");
                                               retJson.put("addRetMsg","推送成功："+sendResult+" -- "+sendMsgResult);
                                           }else{
                                               retJson.put("retCode","-1");
                                               retJson.put("retMsg","推送失败！");
                                               retJson.put("addRetMsg","推送失败："+sendResult + " -- "+sendMsgResult);
                                           }
                                       }

                                    }else{
                                        imFilePushManage.setFilePushTime(LocalDateTime.now());
                                        imFilePushManageMapper.updateById(imFilePushManage);

                                        retJson.put("retCode","0");
                                        retJson.put("retMsg","推送成功！");
                                        retJson.put("addRetMsg","推送成功："+sendResult);
                                    }

                                }else{
                                    retJson.put("retCode","-1");
                                    retJson.put("retMsg","推送失败!");
                                    retJson.put("addRetMsg","推送失败："+sendResult);
                                    break;
                                }
                            }else{
                                retJson.put("retCode","-1");
                                retJson.put("retMsg","推送接口返回空");
                                retJson.put("addRetMsg","推送接口返回空");
                                break;
                            }
                        }
                    }else{
                        retJson.put("retCode","-1");
                        retJson.put("retMsg","推送失败，请检查是否设置推送对象");
                        retJson.put("addRetMsg","推送失败，请检查是否设置推送对象");
                    }
                }else{
                    retJson.put("retCode","-1");
                    retJson.put("retMsg","推送失败，请检查是否设置推送对象");
                    retJson.put("addRetMsg","推送失败，请检查是否设置推送对象");
                }
            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","推送失败，根据["+manageId+"]未查询到相关信息");
                retJson.put("addRetMsg","推送失败，根据["+manageId+"]未查询到相关信息");
            }
        }catch (Exception e) {
            e.printStackTrace();
            retJson.put("retCode","500");
            retJson.put("retMsg","系统异常,请稍后再试！");
            retJson.put("addRetMsg","系统异常: "+e.getMessage());
        }finally {
            //保存操作日志
            if(imFilePushManage != null){
                saveImOperateLog(imFilePushManage,retJson,"pushLeader",manageId);
            }

            if(inputStream != null){
                try {
                    inputStream.close();
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return retJson;

    }


    /**
     * 批量推送
     * @param param
     * @return
     */
    @Override
    public JSONObject batchPush(String param) {
        JSONObject retJson  = new JSONObject();
        FileInputStream inputStream =null;
        ImFilePushManage imFilePushManage = null;
        int iManageId = 0;
        try{
            JSONObject paramJson = JSONObject.parseObject(param);
            String manageIds = paramJson.getString("manageIds");
            if(StringUtil.isNotEmpty(manageIds)){
                String[] arrManageIds = manageIds.split(",");

                boolean isNext = true;
                for(int i=0;i<arrManageIds.length && isNext;i++){
                    String strManageId = arrManageIds[i];
                    iManageId = Integer.parseInt(strManageId);
                    imFilePushManage = imFilePushManageMapper.selectById(iManageId);
                    if(imFilePushManage != null){
                        String filePath = imFilePushManage.getFilePath();
                        File file = new File(filePath);
                        inputStream = new FileInputStream(file);
                        String originalFilename = file.getName();
                        String contentType = "application/octet-stream"; // 根据文件类型修改
                        MultipartFile multipartFile = new MockMultipartFile("file", originalFilename, contentType, inputStream);
                        inputStream.close();

                        //执行推送操作 sendImFile(MultipartFile file,String targetType,String target,String fileType)
                        String targetType = "";
                        String target = "";
                        String fileType ="";
                        String fileName = imFilePushManage.getFileName();
                        if(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".bmp")
                                || fileName.endsWith(".gif") || fileName.endsWith(".svg") || fileName.endsWith(".png")){
                            fileType = "pushImg";
                        }else{
                            fileType = "pushFile";
                        }

                        String pushObjectId = imFilePushManage.getPushObjectId();

                        //字符数组转 整型List
                        String[] pushObjectIdArr = pushObjectId.split(";");
                        List<Integer> pushObjectIdList = new ArrayList<>();
                        for(int k=0;k<pushObjectIdArr.length;k++){
                            String strPushObjectId = pushObjectIdArr[k];
                            pushObjectIdList.add(Integer.parseInt(strPushObjectId));
                        }

                        String lastPushObjectId ="";

                        //根据推送对象管理id集合，查询推送对象集合
                        List<ImPushObjectManage> imPushObjectManageList = imPushObjectManageMapper.selectListByList(pushObjectIdList);
                        if(imPushObjectManageList !=null && imPushObjectManageList.size() > 0){
                            //拼接推送对象id
                            //用户对象id 格式为 [user1,user2,user3]
                            //群对象id 格式为 group1;group2;group3
                            //最终推送格式为 用户对象id + 群对象id
                            String userPushObjectId = "";
                            String groupPushObjectId = "";
                            int userIndex =0;
                            int groupIndex =0;
                            int index =0;
                            String pushObjectName ="";
                            for(ImPushObjectManage imPushObjectManage : imPushObjectManageList){
                                String pustObjectType = imPushObjectManage.getPushObjectType();
                                String queryPushObjectName = imPushObjectManage.getPushObjectName();
                                LocalDateTime bindTime = imPushObjectManage.getBindTime();

                                if(index ==0){
                                    pushObjectName = pushObjectName + imPushObjectManage.getPushObjectName();
                                }else{
                                    pushObjectName = pushObjectName +";"+ imPushObjectManage.getPushObjectName();
                                }

                                imFilePushManage.setPushObjectName(pushObjectName);
                                index++;

                                if(bindTime ==null){
                                    retJson.put("retCode","-1");
                                    retJson.put("retMsg","推送对象["+queryPushObjectName+"]未绑定");
                                    return retJson;

                                }

                                if(pustObjectType.equals("users")){ //用户
                                    if(userIndex ==0){
                                        userPushObjectId = userPushObjectId +imPushObjectManage.getPushObjectId();
                                    }else{
                                        userPushObjectId = userPushObjectId +","+imPushObjectManage.getPushObjectId();
                                    }

                                    userIndex++;
                                }else if(pustObjectType.equals("group")){ //群
                                    if(groupIndex ==0){
                                        groupPushObjectId = groupPushObjectId +imPushObjectManage.getPushObjectId();
                                    }else{
                                        groupPushObjectId = groupPushObjectId +";"+imPushObjectManage.getPushObjectId();
                                    }

                                    groupIndex++;
                                }

                            }

                            if(!userPushObjectId.equals("")){
                                userPushObjectId = "["+userPushObjectId+"]";
                                lastPushObjectId = lastPushObjectId + userPushObjectId;
                            }

                            if(!groupPushObjectId.equals("")){
                                if(userPushObjectId.equals("")){
                                    lastPushObjectId = lastPushObjectId + groupPushObjectId;
                                }else{
                                    lastPushObjectId = lastPushObjectId +";"+ groupPushObjectId;
                                }
                            }
                        }

                        String[] arrPushObjectId = lastPushObjectId.split(";");
                        for(String strPushObjectId : arrPushObjectId){
                            target = strPushObjectId;
                            //用户
                            if(strPushObjectId.indexOf("[") >=0 && strPushObjectId.indexOf("]") >=0){
                                targetType = "users";
                            }else{
                                targetType = "group";
                            }

                            String sendResult = imUtil.sendImFile(multipartFile,targetType,target,fileType);
                            log.info("++++++++++{}_推送结果：{}",fileName,sendResult);
                            if(StringUtil.isNotEmpty(sendResult)){
                                JSONObject bindingJson = JSONObject.parseObject(sendResult);
                                String status = bindingJson.getString("status");
                                if(StringUtil.isNotEmpty(status) && "success".equals(status)){
                                    imFilePushManage.setFilePushTime(LocalDateTime.now());
                                    imFilePushManageMapper.updateById(imFilePushManage);

                                    retJson.put("retCode","0");
                                    retJson.put("retMsg","推送成功！");
                                    retJson.put("addRetMsg","推送成功:"+sendResult);
                                }else{
                                    retJson.put("retCode","-1");
                                    retJson.put("retMsg","推送失败!");
                                    retJson.put("addRetMsg","推送失败:"+sendResult);
                                    isNext = false;
                                    break;
                                }
                            }else{
                                retJson.put("retCode","-1");
                                retJson.put("retMsg","推送接口返回空");
                                retJson.put("addRetMsg","推送接口返回空");
                                isNext = false;
                                break;
                            }
                        }
                    }

                    //保存操作日志
                    if(imFilePushManage != null){
                        saveImOperateLog(imFilePushManage,retJson,"push",iManageId);
                    }

                    //休眠3秒
                    try {
                        log.info("++++++++++{}_3秒后开始执行下一个推送逻辑");
                        Thread.sleep(3000);
                    }catch (InterruptedException ex) {
                        Thread.currentThread().interrupt(); // 重新设置中断状态
                    }

                }
            }
        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","500");
            retJson.put("retMsg","系统异常,请稍后再试！");
            retJson.put("addRetMsg","系统异常:"+e.getMessage());
            //保存操作日志
            if(imFilePushManage != null){
                saveImOperateLog(imFilePushManage,retJson,"push",iManageId);
            }
        }finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        return retJson;
    }


    /**
     * 批量推送至领导
     * @param param
     * @return
     */
    @Override
    public JSONObject batchPushLeader(String param) {
        JSONObject retJson  = new JSONObject();
        FileInputStream inputStream =null;
        ImFilePushManage imFilePushManage = null;
        int iManageId = 0;
        try{
            JSONObject paramJson = JSONObject.parseObject(param);
            String manageIds = paramJson.getString("manageIds");
            if(StringUtil.isNotEmpty(manageIds)){
                String[] arrManageIds = manageIds.split(",");

                boolean isNext = true;
                for(int i=0;i<arrManageIds.length && isNext;i++){
                    String strManageId = arrManageIds[i];
                    iManageId = Integer.parseInt(strManageId);
                    imFilePushManage = imFilePushManageMapper.selectById(iManageId);
                    if(imFilePushManage != null){

                        String filePath = imFilePushManage.getFilePath();
                        File file = new File(filePath);
                        inputStream = new FileInputStream(file);
                        String originalFilename = file.getName();
                        String contentType = "application/octet-stream"; // 根据文件类型修改
                        MultipartFile multipartFile = new MockMultipartFile("file", originalFilename, contentType, inputStream);
                        inputStream.close();

                        String isPushTxtMsg = imFilePushManage.getIsPushTxtMsg();
                        String pushTxtMsg = imFilePushManage.getPustTxtMsg();

                        //执行推送操作 sendImFile(MultipartFile file,String targetType,String target,String fileType)
                        String targetType = "";
                        String target = "";
                        String fileType ="";
                        String fileName = imFilePushManage.getFileName();
                        if(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".bmp")
                                || fileName.endsWith(".gif") || fileName.endsWith(".svg") || fileName.endsWith(".png")){
                            fileType = "pushImg";
                        }else{
                            fileType = "pushFile";
                        }

                        String pushObjectId = imFilePushManage.getPushObjectLeaderId();

                        //字符数组转 整型List
                        String[] pushObjectIdArr = pushObjectId.split(";");
                        List<Integer> pushObjectIdList = new ArrayList<>();
                        for(int k=0;k<pushObjectIdArr.length;k++){
                            String strPushObjectId = pushObjectIdArr[k];
                            pushObjectIdList.add(Integer.parseInt(strPushObjectId));
                        }

                        String lastPushObjectId ="";

                        //根据推送对象管理id集合，查询推送对象集合
                        List<ImPushObjectManage> imPushObjectManageList = imPushObjectManageMapper.selectListByList(pushObjectIdList);
                        if(imPushObjectManageList !=null && imPushObjectManageList.size() > 0){
                            //拼接推送对象id
                            //用户对象id 格式为 [user1,user2,user3]
                            //群对象id 格式为 group1;group2;group3
                            //最终推送格式为 用户对象id + 群对象id
                            String userPushObjectId = "";
                            String groupPushObjectId = "";
                            int userIndex =0;
                            int groupIndex =0;
                            int index =0;
                            String pushObjectName ="";
                            for(ImPushObjectManage imPushObjectManage : imPushObjectManageList){
                                String pustObjectType = imPushObjectManage.getPushObjectType();
                                String queryPushObjectName = imPushObjectManage.getPushObjectName();
                                LocalDateTime bindTime = imPushObjectManage.getBindTime();

                                if(index ==0){
                                    pushObjectName = pushObjectName + imPushObjectManage.getPushObjectName();
                                }else{
                                    pushObjectName = pushObjectName +";"+ imPushObjectManage.getPushObjectName();
                                }

                                imFilePushManage.setPushObjectName(pushObjectName);
                                index++;

                                if(bindTime ==null){
                                    retJson.put("retCode","-1");
                                    retJson.put("retMsg","推送对象["+queryPushObjectName+"]未绑定");
                                    return retJson;

                                }

                                if(pustObjectType.equals("users")){ //用户
                                    if(userIndex ==0){
                                        userPushObjectId = userPushObjectId +imPushObjectManage.getPushObjectId();
                                    }else{
                                        userPushObjectId = userPushObjectId +","+imPushObjectManage.getPushObjectId();
                                    }

                                    userIndex++;
                                }else if(pustObjectType.equals("group")){ //群
                                    if(groupIndex ==0){
                                        groupPushObjectId = groupPushObjectId +imPushObjectManage.getPushObjectId();
                                    }else{
                                        groupPushObjectId = groupPushObjectId +";"+imPushObjectManage.getPushObjectId();
                                    }

                                    groupIndex++;
                                }

                            }

                            if(!userPushObjectId.equals("")){
                                userPushObjectId = "["+userPushObjectId+"]";
                                lastPushObjectId = lastPushObjectId + userPushObjectId;
                            }

                            if(!groupPushObjectId.equals("")){
                                if(userPushObjectId.equals("")){
                                    lastPushObjectId = lastPushObjectId + groupPushObjectId;
                                }else{
                                    lastPushObjectId = lastPushObjectId +";"+ groupPushObjectId;
                                }
                            }
                        }

                        String[] arrPushObjectId = lastPushObjectId.split(";");
                        for(String strPushObjectId : arrPushObjectId){
                            target = strPushObjectId;
                            //用户
                            if(strPushObjectId.indexOf("[") >=0 && strPushObjectId.indexOf("]") >=0){
                                targetType = "users";
                            }else{
                                targetType = "group";
                            }

                            String sendResult = imUtil.sendImFile(multipartFile,targetType,target,fileType);
                            log.info("++++++++++{}_文件推送结果：{}",fileName,sendResult);
                            if(StringUtil.isNotEmpty(sendResult)){
                                JSONObject bindingJson = JSONObject.parseObject(sendResult);
                                String status = bindingJson.getString("status");

                                if(StringUtil.isNotEmpty(status) && "success".equals(status)){ //推送成功
                                    imFilePushManage.setFilePushTime(LocalDateTime.now());
                                    imFilePushManageMapper.updateById(imFilePushManage);

                                    //推送成功后，推送文本消息
                                    if(isPushTxtMsg.equals("1") && StringUtil.isNotEmpty(pushTxtMsg)){
                                        //休眠3秒
                                        try {
                                            log.info("++++++++++{}_3秒后开始推送文本消息",fileName);
                                            Thread.sleep(3000);
                                        }catch (InterruptedException ex) {
                                            Thread.currentThread().interrupt(); // 重新设置中断状态
                                        }

                                        String sendMsgResult = imUtil.sendImMsg(pushTxtMsg,targetType,target);
                                        log.info("++++++++++{}_文字信息推送结果：{}",fileName,sendMsgResult);
                                        if(StringUtil.isNotEmpty(sendMsgResult)){
                                            JSONObject sendMsgJson = JSONObject.parseObject(sendMsgResult);
                                            String sendMsgStatus = sendMsgJson.getString("status");
                                            //推送成功
                                            if(StringUtil.isNotEmpty(sendMsgStatus) && "success".equals(sendMsgStatus)){
                                                imFilePushManage.setFilePushTime(LocalDateTime.now());
                                                imFilePushManageMapper.updateById(imFilePushManage);

                                                retJson.put("retCode","0");
                                                retJson.put("retMsg","推送成功！");
                                                retJson.put("addRetMsg","推送成功："+sendResult+" -- "+sendMsgResult);
                                            }else{
                                                retJson.put("retCode","-1");
                                                retJson.put("retMsg","推送失败！");
                                                retJson.put("addRetMsg","推送失败："+sendResult + " -- "+sendMsgResult);
                                                isNext = false;
                                                break;
                                            }
                                        }
                                    }else{
                                        imFilePushManage.setFilePushTime(LocalDateTime.now());
                                        imFilePushManageMapper.updateById(imFilePushManage);

                                        retJson.put("retCode","0");
                                        retJson.put("retMsg","推送成功！");
                                        retJson.put("addRetMsg","推送成功："+sendResult);
                                    }

                                }else{
                                    retJson.put("retCode","-1");
                                    retJson.put("retMsg","推送失败!");
                                    retJson.put("addRetMsg","推送失败:"+sendResult);
                                    isNext = false;
                                    break;
                                }
                            }else{
                                retJson.put("retCode","-1");
                                retJson.put("retMsg","推送接口返回空");
                                retJson.put("addRetMsg","推送接口返回空");
                                isNext = false;
                                break;
                            }
                        }
                    }

                    //保存操作日志
                    if(imFilePushManage != null){
                        saveImOperateLog(imFilePushManage,retJson,"pushLeader",iManageId);
                    }

                    //休眠3秒
                    try {
                        log.info("++++++++++{}_3秒后开始执行下一个推送至领导逻辑");
                        Thread.sleep(3000);
                    }catch (InterruptedException ex) {
                        Thread.currentThread().interrupt(); // 重新设置中断状态
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","500");
            retJson.put("retMsg","系统异常,请稍后再试！");
            retJson.put("addRetMsg","系统异常:"+e.getMessage());
            //保存操作日志
            if(imFilePushManage != null){
                saveImOperateLog(imFilePushManage,retJson,"pushLeader",iManageId);
            }
        }finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        return retJson;
    }

    /**
     * 文件上传
     * @param file
     * @param manageId
     * @return
     */
    @Override
    public boolean upload(MultipartFile file, int manageId) {
        boolean result = false;

        if (file.isEmpty()) {
            return false;
        }

        ImFilePushManage imFilePushManage = null;
        try {
             imFilePushManage = imFilePushManageMapper.selectById(manageId);
             if(imFilePushManage != null){
                 //获取文件名
                 String fileName = file.getOriginalFilename();
                 log.info("上传的文件名为：" + fileName);

                 // 文件路径
                 String filePath = pptTargetPath+fileName;

                 File dest = new File(filePath);

                 // 检测是否存在目录
                 if (!dest.getParentFile().exists()) {
                     dest.getParentFile().mkdirs();
                 }

                 file.transferTo(dest);

                 imFilePushManage.setFileName(fileName);
                 imFilePushManage.setFilePath(filePath);
                 imFilePushManage.setFileCreatTime(LocalDateTime.now());
                 imFilePushManageMapper.updateById(imFilePushManage);

                 result = true;

             }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            //保存操作日志
            if(imFilePushManage != null){
                ImOperateLog imOperateLog = new ImOperateLog();
                imOperateLog.setOperateType("upload");
                imOperateLog.setModelId(imFilePushManage.getModelId());
                imOperateLog.setModelName(imFilePushManage.getModelName());
                imOperateLog.setFileName(imFilePushManage.getFileName());
                imOperateLog.setObjectId(imFilePushManage.getPushObjectId());

                String objectIds = imFilePushManage.getPushObjectId();
                String[] objectIdArr = objectIds.split(";");

                ImPushObjectManage imPushObjectManage = null;
                String objectName = "";
                for(int i=0;i<objectIdArr.length;i++){
                    String strObjectId = objectIdArr[i];
                    int iPushObjectId = Integer.parseInt(strObjectId);
                    imPushObjectManage = imPushObjectManageMapper.selectById(iPushObjectId);
                    if(imPushObjectManage != null){
                        if(i==0){
                            objectName = objectName+imPushObjectManage.getPushObjectName();
                        }else{
                            objectName = objectName+";"+imPushObjectManage.getPushObjectName();
                        }
                    }
                }

                imOperateLog.setObjectName(objectName);
                imOperateLog.setFileCreatTime(imFilePushManage.getFileCreatTime());
                imOperateLog.setOperateTime(LocalDateTime.now());
                imOperateLog.setFilePath(imFilePushManage.getFilePath());
                imOperateLog.setManageId(imFilePushManage.getManageId());

                String retCode = "";
                String retMsg = "";
                if(result){
                    retCode = "0";
                    retMsg = "上传成功！";
                }else{
                    retCode = "-1";
                    retMsg = "上传失败！";
                }

                imOperateLog.setIsSuccess(retCode);
                imOperateLog.setOperateResult(retMsg);

                //获取当前登录用户信息
                SysUser currentUser = UserContext.currentUser();
                String realName = currentUser.getRealName();
                imOperateLog.setOperatePerson(realName);

                imOperateLogMapper.insert(imOperateLog);
            }
        }

        return result;
    }

    /**
     * 保存IM操作日志
     * @param imFilePushManage
     * @param retJson
     * @param iManageId
     */
    private void saveImOperateLog(ImFilePushManage imFilePushManage,JSONObject retJson,String operateType,int iManageId){
        //保存操作日志
        if(imFilePushManage != null){
            ImOperateLog imOperateLog = new ImOperateLog();
            imOperateLog.setOperateType(operateType);
            imOperateLog.setModelId(imFilePushManage.getModelId());
            imOperateLog.setModelName(imFilePushManage.getModelName());
            imOperateLog.setFileName(imFilePushManage.getFileName());
            imOperateLog.setObjectId(imFilePushManage.getPushObjectId());
            imOperateLog.setObjectName(imFilePushManage.getPushObjectName());
            imOperateLog.setFileCreatTime(imFilePushManage.getFileCreatTime());
            imOperateLog.setOperateTime(LocalDateTime.now());
            imOperateLog.setFilePath(imFilePushManage.getFilePath());
            imOperateLog.setManageId(iManageId);

            String retCode = retJson.getString("retCode");
            String retMsg = retJson.getString("addRetMsg");
            if(StringUtil.isNotEmpty(retMsg) && retMsg.length()>1000){
                retMsg = retMsg.substring(0,1000);
            }

            imOperateLog.setIsSuccess(retCode);
            imOperateLog.setOperateResult(retMsg);

            //获取当前登录用户信息
            SysUser currentUser = UserContext.currentUser();
            if(currentUser ==null){
                //取管理员信息
                currentUser = sysUserService.getById("1309861917694623744");
            }
            String realName = currentUser.getRealName();
            imOperateLog.setOperatePerson(realName);

            imOperateLogMapper.insert(imOperateLog);
        }
    }

}
