package com.pearadmin.modules.im.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.context.UserContext;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.im.domain.ImOperateLog;
import com.pearadmin.modules.im.domain.ImPushObjectManage;
import com.pearadmin.modules.im.domain.ImHdjkPushManage;
import com.pearadmin.modules.im.mapper.ImOperateLogMapper;
import com.pearadmin.modules.im.mapper.ImPushObjectManageMapper;
import com.pearadmin.modules.im.mapper.ImHdjkPushManageMapper;
import com.pearadmin.modules.im.service.IHdjkFilePushManageService;
import com.pearadmin.modules.im.util.ImUtil;
import com.pearadmin.modules.hdjk.domain.HdjkModelManage;
import com.pearadmin.modules.search.util.FileToPdfUtils;
import com.pearadmin.modules.sys.domain.SysUser;
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
import springfox.documentation.spring.web.json.Json;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 创建日期：2025-04-25
 * IM文件推送管理
 **/

@Service
@Slf4j
public class HdjkFilePushManageServiceImpl implements IHdjkFilePushManageService {

    @Value("${im-file-topdf-path}")
    private String imFileTopdfPath;

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
    private ImHdjkPushManageMapper imRbFilePushManageMapper;

    /**
     * 获取IM文件推送管理列表
     * @param imRbFilePushManage
     * @return
     */
    @Override
    public List<ImHdjkPushManage> getImRbFilePushManageList(ImHdjkPushManage imRbFilePushManage) {
        /*QueryWrapper<ImHdjkPushManage> queryWrapper = new QueryWrapper<>();
        if(StringUtil.isNotEmpty(imRbFilePushManage.getModelName())){
            queryWrapper.like("model_name", imRbFilePushManage.getModelName());
        }
        if(StringUtil.isNotEmpty(imRbFilePushManage.getFileName())){
            queryWrapper.like("file_name", imRbFilePushManage.getFileName());
        }

        queryWrapper.orderByDesc("file_creat_time");
        List<ImHdjkPushManage> list = imRbFilePushManageMapper.selectList(queryWrapper);*/

        List<ImHdjkPushManage> list = imRbFilePushManageMapper.getHdjkPushManageList(imRbFilePushManage.getModelName(),imRbFilePushManage.getSendFileName(),imRbFilePushManage.getRunState());

        if(list !=null && list.size() >0){
            for(ImHdjkPushManage queryImRbFilePushManage : list){

                //查询对象
                String pushObjectId = queryImRbFilePushManage.getPushObjectId();
                if(StringUtil.isNotEmpty(pushObjectId)){
                    String pushObjectName = "";
                    List<Integer> pushObjectIdList = Arrays.stream(pushObjectId.split(";"))
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());

                    List<ImPushObjectManage> imPushObjectManageList = imPushObjectManageMapper.selectListByList(pushObjectIdList);
                    if(imPushObjectManageList != null && imPushObjectManageList.size() >0){
                        for(ImPushObjectManage imPushObjectManage: imPushObjectManageList){
                            if(!pushObjectName.equals("")){
                                pushObjectName = pushObjectName+";"+imPushObjectManage.getPushObjectName();
                            }else{
                                pushObjectName = imPushObjectManage.getPushObjectName();
                            }
                        }
                    }
                    queryImRbFilePushManage.setPushObjectName(pushObjectName);
                }

                //是否推送领导为是时，查询领导
                String is_push_leader = queryImRbFilePushManage.getIsPushLeader() ==null?"":queryImRbFilePushManage.getIsPushLeader();
                if(is_push_leader.equals("1")){
                    String pushLeaderId = queryImRbFilePushManage.getPushLeaderId();
                    if(StringUtil.isNotEmpty(pushLeaderId)){
                        String pushObjectName = "";
                        List<Integer> pushObjectIdList = Arrays.stream(pushLeaderId.split(";"))
                                .map(Integer::parseInt)
                                .collect(Collectors.toList());

                        List<ImPushObjectManage> imPushObjectManageList = imPushObjectManageMapper.selectListByList(pushObjectIdList);
                        if(imPushObjectManageList != null && imPushObjectManageList.size() >0){
                            for(ImPushObjectManage imPushObjectManage: imPushObjectManageList){
                                if(!pushObjectName.equals("")){
                                    pushObjectName = pushObjectName+";"+imPushObjectManage.getPushObjectName();
                                }else{
                                    pushObjectName = imPushObjectManage.getPushObjectName();
                                }
                            }
                        }

                        queryImRbFilePushManage.setPushLeaderName(pushObjectName);

                    }
                }
            }
        }
        return list;
    }

    /**
     * 保存IM文件推送管理信息
     * @param imRbFilePushManage
     * @return
     */
    @Override
    public boolean save(ImHdjkPushManage imRbFilePushManage) {
        int result = 0;
        // 判断是否存在该ManageId的记录，如果存在则更新，不存在则插入新的记录
        if(imRbFilePushManage.getManageId() != null){
            //imFilePushManage.setFileCreatTime(LocalDateTime.now());

            ImHdjkPushManage queryImRbFilePushManage = imRbFilePushManageMapper.selectById(imRbFilePushManage.getManageId());
            if(queryImRbFilePushManage != null){

                queryImRbFilePushManage.setModelId(imRbFilePushManage.getModelId());
                queryImRbFilePushManage.setModelName(imRbFilePushManage.getModelName());

                queryImRbFilePushManage.setPushObjectId(imRbFilePushManage.getPushObjectId());
                queryImRbFilePushManage.setPushObjectName(imRbFilePushManage.getPushObjectName());
                queryImRbFilePushManage.setIsAutoPush(imRbFilePushManage.getIsAutoPush());
                queryImRbFilePushManage.setIsPushImg(imRbFilePushManage.getIsPushImg());

                queryImRbFilePushManage.setPushLeaderId(imRbFilePushManage.getPushLeaderId());
                queryImRbFilePushManage.setPushObjectPeriod(imRbFilePushManage.getPushObjectPeriod());
                queryImRbFilePushManage.setSendObjectWeek(imRbFilePushManage.getSendObjectWeek());
                queryImRbFilePushManage.setSendObjectDay(imRbFilePushManage.getSendObjectDay());
                queryImRbFilePushManage.setExecState(imRbFilePushManage.getExecState());
                queryImRbFilePushManage.setIsPushLeader(imRbFilePushManage.getIsPushLeader());
                queryImRbFilePushManage.setSendObjectTime(imRbFilePushManage.getSendObjectTime());
                queryImRbFilePushManage.setIsWeekendPush(imRbFilePushManage.getIsWeekendPush());
                queryImRbFilePushManage.setRunState(imRbFilePushManage.getRunState());

                result = imRbFilePushManageMapper.updateById(queryImRbFilePushManage);
            }

        }else{
            QueryWrapper<ImHdjkPushManage> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("model_id", imRbFilePushManage.getModelId());
            ImHdjkPushManage queryImRbFilePushManage = imRbFilePushManageMapper.selectOne(queryWrapper);
            if(queryImRbFilePushManage != null){
                queryImRbFilePushManage.setPushObjectId(imRbFilePushManage.getPushObjectId());
                queryImRbFilePushManage.setPushObjectName(imRbFilePushManage.getPushObjectName());
                queryImRbFilePushManage.setIsAutoPush(imRbFilePushManage.getIsAutoPush());
                queryImRbFilePushManage.setIsPushImg(imRbFilePushManage.getIsPushImg());

                queryImRbFilePushManage.setPushLeaderId(imRbFilePushManage.getPushLeaderId());
                queryImRbFilePushManage.setPushObjectPeriod(imRbFilePushManage.getPushObjectPeriod());
                queryImRbFilePushManage.setSendObjectWeek(imRbFilePushManage.getSendObjectWeek());
                queryImRbFilePushManage.setSendObjectDay(imRbFilePushManage.getSendObjectDay());
                queryImRbFilePushManage.setExecState(imRbFilePushManage.getExecState());
                queryImRbFilePushManage.setIsPushLeader(imRbFilePushManage.getIsPushLeader());
                queryImRbFilePushManage.setSendObjectTime(imRbFilePushManage.getSendObjectTime());
                queryImRbFilePushManage.setIsWeekendPush(imRbFilePushManage.getIsWeekendPush());
                queryImRbFilePushManage.setRunState(imRbFilePushManage.getRunState());

                result = imRbFilePushManageMapper.updateById(queryImRbFilePushManage);
            }else{
                //imFilePushManage.setFileCreatTime(LocalDateTime.now());
                result = imRbFilePushManageMapper.insert(imRbFilePushManage);
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

        int result = imRbFilePushManageMapper.deleteById(manageId);
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
    public ImHdjkPushManage getById(int manageId) {
        return imRbFilePushManageMapper.selectById(manageId);
    }


    /**
     * 获取页面的模板下拉框
     * @return
     */
    @Override
    public Object getModelSelect() {
        //模板
        List<HdjkModelManage> modelsList = imRbFilePushManageMapper.getModelList();
        List<JSONObject> retList = new ArrayList<>();

        if(modelsList !=null && modelsList.size() >0){
            String modelType ="";
            String disModelType ="";
            String modelName ="";
            for(HdjkModelManage rbhdjkModelManage : modelsList){
                modelName = rbhdjkModelManage.getModelName()==null?"":rbhdjkModelManage.getModelName().trim();
                modelType = rbhdjkModelManage.getModelType();
                if(modelType.equals("1")){
                    disModelType = "日报";
                }

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name","["+rbhdjkModelManage.getModelArea()+"_"+disModelType+"]-"+rbhdjkModelManage.getModelName());
                jsonObject.put("value",rbhdjkModelManage.getModelId());
                jsonObject.put("level",rbhdjkModelManage.getModelArea());
                jsonObject.put("modelType",modelType);
                jsonObject.put("pptName", modelName);
                jsonObject.put("modelName",rbhdjkModelManage.getModelName());
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
        List<HdjkModelManage> modelsList = imRbFilePushManageMapper.getModelSelList(modelId);
        List<JSONObject> retList = new ArrayList<>();

        //String[] modelIds = modelId.split(",");

        if(modelsList !=null && modelsList.size() >0){
            for(HdjkModelManage rbhdjkModelManage : modelsList){
                int modelIdValue = rbhdjkModelManage.getModelId();

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name",rbhdjkModelManage.getModelName());
                jsonObject.put("value",rbhdjkModelManage.getModelId());
                jsonObject.put("level",rbhdjkModelManage.getModelArea());

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
            ImHdjkPushManage imRbFilePushManage = imRbFilePushManageMapper.selectById(manageId);
            if(imRbFilePushManage != null){

                String path = imRbFilePushManage.getFilePath();
                //String fileName = imRbFilePushManage.getFileName();
                String pdfPath = imFileTopdfPath;
                String pdfName = FileToPdfUtils.officeToPdfFileName(path,pdfPath);
                String port = env.getProperty("server.port");
                String serverIp = env.getProperty("server.server-ip");

                //String pdfName = fileName.substring(0,fileName.lastIndexOf(".")+1)+"pdf";
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
        ImHdjkPushManage imRbFilePushManage = imRbFilePushManageMapper.selectById(manageId);
        if(imRbFilePushManage != null){
            filePath = imRbFilePushManage.getFilePath();
        }
        //读取文件
        //String filePath = searchUploadPath + fileName;


        FileSystemResource file = new FileSystemResource(filePath);

        /*String encodedFilename = new String(file.getFilename().getBytes("UTF-8"), "ISO-8859-1");

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

        HttpHeaders headers = new HttpHeaders();
        // 确保不缓存
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        // 处理特殊字符的文件名
        String encodedFileName = URLEncoder.encode(file.getFilename(), StandardCharsets.UTF_8.toString());
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
     * 一键推送
     * @param param
     * @return
     */
    @Override
    public JSONObject push(String param) {

        JSONObject retJson  = new JSONObject();
        ImHdjkPushManage imRbFilePushManage = null;
        FileInputStream inputStream = null;
        int manageId =0;
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            manageId = paramJson.getInteger("manageId");
            imRbFilePushManage = imRbFilePushManageMapper.selectById(manageId);
            if(imRbFilePushManage != null){
                String pushObjectId = imRbFilePushManage.getPushObjectId();
                String filePath = imRbFilePushManage.getFilePath();
                String imgPath = imRbFilePushManage.getImgPath();
                String isPushImg = imRbFilePushManage.getIsPushImg()==null?"":imRbFilePushManage.getIsPushImg().trim();

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

                            imRbFilePushManage.setPushObjectName(pushObjectName);
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

                        //推送文件
                        if(filePath != null && filePath.trim().length() >0){
                            File file = new File(filePath);
                            inputStream = new FileInputStream(file);
                            String originalFilename = file.getName();
                            String contentType = "application/octet-stream"; // 根据文件类型修改
                            MultipartFile multipartFile = new MockMultipartFile("file", originalFilename, contentType, inputStream);
                            inputStream.close();

                            //执行推送操作 sendImFile(MultipartFile file,String targetType,String target,String fileType)
                            String targetType = "";
                            String target = "";
                            String fileType ="pushFile";

                            for(String strPushObjectId : arrPushObjectId){
                                target = strPushObjectId;
                                //用户
                                if(strPushObjectId.indexOf("[") >=0 && strPushObjectId.indexOf("]") >=0){
                                    targetType = "users";
                                }else{
                                    targetType = "group";
                                }

                                String sendResult = imUtil.sendImFile(multipartFile,targetType,target,fileType);
                                log.info("++++++++++{}_文件推送结果：{}",originalFilename,sendResult);
                                if(StringUtil.isNotEmpty(sendResult)){
                                    JSONObject bindingJson = JSONObject.parseObject(sendResult);
                                    String status = bindingJson.getString("status");
                                    //推送成功 推送图片为 1
                                    if(StringUtil.isNotEmpty(status) && "success".equals(status)){
                                        /*imRbFilePushManage.setFilePushTime(LocalDateTime.now());
                                        imRbFilePushManageMapper.updateById(imRbFilePushManage);
                                        retJson.put("retCode","0");
                                        retJson.put("retMsg","推送成功！");
                                        retJson.put("addRetMsg","推送成功: "+sendResult);*/
                                        //推送图片
                                        if(isPushImg.equals("1")){
                                            //推送图片
                                            if(imgPath != null && imgPath.trim().length() >0){
                                                //Thread.sleep(2000);
                                                File imgFile = new File(imgPath);
                                                inputStream = new FileInputStream(imgFile);
                                                String imgFilename = imgFile.getName();
                                                String imgContentType = "application/octet-stream"; // 根据文件类型修改
                                                MultipartFile imgMultipartFile = new MockMultipartFile("file", imgFilename, imgContentType, inputStream);
                                                inputStream.close();

                                                //执行推送操作 sendImFile(MultipartFile file,String targetType,String target,String fileType)
                                                fileType ="pushImg";

                                                String sendImgResult = imUtil.sendImFile(imgMultipartFile,targetType,target,fileType);
                                                log.info("++++++++++{}_图片推送结果：{}",imgFilename,sendImgResult);
                                                if(StringUtil.isNotEmpty(sendImgResult)){
                                                    JSONObject sendImgJson = JSONObject.parseObject(sendImgResult);
                                                    String imgStatus = sendImgJson.getString("status");
                                                    //推送成功
                                                    if(StringUtil.isNotEmpty(imgStatus) && "success".equals(imgStatus)){
                                                        imRbFilePushManage.setFilePushTime(LocalDateTime.now());
                                                        imRbFilePushManageMapper.updateById(imRbFilePushManage);
                                                        retJson.put("retCode","0");
                                                        retJson.put("retMsg","推送成功！");
                                                        retJson.put("addRetMsg","推送成功："+sendResult+" -- "+sendImgResult);
                                                    }else{
                                                        retJson.put("retCode","-1");
                                                        retJson.put("retMsg","图片推送失败!");
                                                        retJson.put("addRetMsg","图片推送失败："+sendImgResult);
                                                        return retJson;
                                                    }
                                                }else{
                                                    retJson.put("retCode","-1");
                                                    retJson.put("retMsg","图片推送接口返回空");
                                                    retJson.put("addRetMsg","图片推送接口返回空");
                                                    return retJson;
                                                }
                                            }
                                        }else{
                                            imRbFilePushManage.setFilePushTime(LocalDateTime.now());
                                            imRbFilePushManageMapper.updateById(imRbFilePushManage);

                                            retJson.put("retCode","0");
                                            retJson.put("retMsg","推送成功！");
                                            retJson.put("addRetMsg","推送成功："+sendResult);
                                        }

                                    }else{
                                        retJson.put("retCode","-1");
                                        retJson.put("retMsg","文件推送失败!");
                                        retJson.put("addRetMsg","文件推送失败："+sendResult);
                                        return retJson;
                                    }
                                }else{
                                    retJson.put("retCode","-1");
                                    retJson.put("retMsg","文件推送接口返回空");
                                    retJson.put("addRetMsg","文件推送接口返回空");
                                    return retJson;
                                }
                            }
                        }

                    }else{
                        retJson.put("retCode","-1");
                        retJson.put("retMsg","推送失败，请检查是否设置绑定对象!");
                        retJson.put("addRetMsg","推送失败，请检查是否设置绑定对象!");
                    }
                }else{
                    retJson.put("retCode","-1");
                    retJson.put("retMsg","推送失败，请检查是否设置绑定对象!");
                    retJson.put("addRetMsg","推送失败，请检查是否设置绑定对象!");
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
            if(imRbFilePushManage != null){
                //保存操作日志
                saveImOperateLog(imRbFilePushManage,retJson,"push",manageId);
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
     * 一键推送
     * @param param
     * @return
     */
    @Override
    public JSONObject pushNew(String param){
        JSONObject retJsonObject = new JSONObject();
        ImHdjkPushManage imHdjkPushManage = null;
        int manageId =0;
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            manageId = paramJson.getInteger("manageId");
            imHdjkPushManage = imRbFilePushManageMapper.selectById(manageId);
            if(imHdjkPushManage != null){
                String pushObjectId = imHdjkPushManage.getPushObjectId();
                String filePath = imHdjkPushManage.getFilePath();
                String imgPath = imHdjkPushManage.getImgPath();
                String isPushImg = imHdjkPushManage.getIsPushImg()==null?"":imHdjkPushManage.getIsPushImg().trim();

                if(StringUtil.isNotEmpty(pushObjectId)){
                    List<Integer> pushObjectIdList = Arrays.stream(pushObjectId.split(";"))
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());

                    //根据推送对象管理id集合，查询推送对象集合
                    List<ImPushObjectManage> imPushObjectManageList = imPushObjectManageMapper.selectListByList(pushObjectIdList);
                    if(imPushObjectManageList != null && imPushObjectManageList.size() >0){
                        //获取推送对象信息
                        Map<String,String> pushObjectManageMap = getPushObjectMap(imPushObjectManageList);
                        if(pushObjectManageMap != null && pushObjectManageMap.size() >0){

                            String pushNames = pushObjectManageMap.get("pushNames")==null?"":pushObjectManageMap.get("pushNames");
                            imHdjkPushManage.setPushObjectName(pushNames);

                            //给用户推送IM文件
                            String pushUserId = pushObjectManageMap.get("userPushId");
                            if(StringUtil.isNotEmpty(pushUserId)){
                                //调用接口,推送Im文件给用户
                                JSONObject pushUserFileJson = pushUserFileToIm(filePath,pushUserId,"pushFile");
                                String pushUserFileCode = pushUserFileJson.getString("retCode");
                                if(pushUserFileCode.equals("0")){ //推送文件成功
                                    if(isPushImg.equals("1")){ //需要推送图片
                                        JSONObject pushUserImgJson = pushUserFileToIm(imgPath,pushUserId,"pushImg");
                                        String pushUserImgCode = pushUserImgJson.getString("retCode");
                                        if(pushUserImgCode.equals("0")){ //推送图片成功
                                            retJsonObject.put("retCode","0");
                                            retJsonObject.put("retMsg","推送成功");
                                            retJsonObject.put("addRetMsg",pushUserImgJson.get("addRetMsg"));

                                            imHdjkPushManage.setFilePushTime(LocalDateTime.now());
                                            imRbFilePushManageMapper.updateById(imHdjkPushManage);
                                        }else{//推送图片失败
                                            retJsonObject.put("retCode","-1");
                                            retJsonObject.put("retMsg","推送失败");
                                            retJsonObject.put("addRetMsg",pushUserImgJson.get("addRetMsg"));
                                        }
                                    }else{//不需要推送图片
                                        retJsonObject.put("retCode","0");
                                        retJsonObject.put("retMsg","推送成功");
                                        retJsonObject.put("addRetMsg",pushUserFileJson.get("addRetMsg"));

                                        imHdjkPushManage.setFilePushTime(LocalDateTime.now());
                                        imRbFilePushManageMapper.updateById(imHdjkPushManage);
                                    }
                                }else{ //推送文件失败
                                    retJsonObject.put("retCode","-1");
                                    retJsonObject.put("retMsg","推送失败");
                                    retJsonObject.put("addRetMsg",pushUserFileJson.get("addRetMsg"));
                                }
                            }

                            //给群推送IM文件
                            String groupPushId = pushObjectManageMap.get("groupPushId");
                            if(StringUtil.isNotEmpty(groupPushId)){
                                //推送Im文件给群
                                JSONObject pushGroupFileJson = pushGroupFileToIm(filePath,groupPushId,"pushFile");
                                String pushGroupFileCode = pushGroupFileJson.getString("retCode");
                                if(pushGroupFileCode.equals("0")){ //推送文件成功
                                    if(isPushImg.equals("1")){ //需要推送图片
                                        JSONObject pushGroupImgJson = pushGroupFileToIm(imgPath,groupPushId,"pushImg");
                                        String pushGroupImgCode = pushGroupImgJson.getString("retCode");
                                        if(pushGroupImgCode.equals("0")){ //推送图片成功
                                            retJsonObject.put("retCode","0");
                                            retJsonObject.put("retMsg","推送成功");
                                            retJsonObject.put("addRetMsg",pushGroupImgJson.get("addRetMsg"));

                                            imHdjkPushManage.setFilePushTime(LocalDateTime.now());
                                            imRbFilePushManageMapper.updateById(imHdjkPushManage);
                                        }else{//推送图片失败
                                            retJsonObject.put("retCode","-1");
                                            retJsonObject.put("retMsg","推送失败");
                                            retJsonObject.put("addRetMsg",pushGroupImgJson.get("addRetMsg"));
                                        }
                                    }else{//不需要推送图片
                                        retJsonObject.put("retCode","0");
                                        retJsonObject.put("retMsg","推送成功");
                                        retJsonObject.put("addRetMsg",pushGroupFileJson.get("addRetMsg"));

                                        imHdjkPushManage.setFilePushTime(LocalDateTime.now());
                                        imRbFilePushManageMapper.updateById(imHdjkPushManage);
                                    }
                                }else{ //推送文件失败
                                    retJsonObject.put("retCode","-1");
                                    retJsonObject.put("retMsg","推送失败");
                                    retJsonObject.put("addRetMsg",pushGroupFileJson.get("addRetMsg"));
                                }
                            }

                        }
                    }else{
                        retJsonObject.put("retCode","-1");
                        retJsonObject.put("retMsg","未查询到推送对象信息");
                        retJsonObject.put("addRetMsg","推送失败：未查询到推送对象信息");
                    }

                }else{
                    retJsonObject.put("retCode","-1");
                    retJsonObject.put("retMsg","推送对象为空");
                    retJsonObject.put("addRetMsg","推送对象为空");
                }

            }else{
                retJsonObject.put("retCode","-1");
                retJsonObject.put("retMsg","未查询到相关信息");
                retJsonObject.put("addRetMsg","未查询到相关信息");
            }

        }catch (Exception e){
            e.printStackTrace();
            retJsonObject.put("retCode","500");
            retJsonObject.put("retMsg","系统异常,请稍后再试！");
            retJsonObject.put("addRetMsg","系统异常: "+e.getMessage());
        }finally {
            if(imHdjkPushManage != null){
                //保存操作日志
                saveImOperateLog(imHdjkPushManage,retJsonObject,"push",manageId);
            }
        }
        return retJsonObject;
    }

    /**
     * 一键推送至领导
     * @param param
     * @return
     */
    @Override
    public JSONObject pushLeader(String param) {

        JSONObject retJson  = new JSONObject();
        ImHdjkPushManage imRbFilePushManage = null;
        FileInputStream inputStream = null;
        int manageId =0;
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            manageId = paramJson.getInteger("manageId");
            imRbFilePushManage = imRbFilePushManageMapper.selectById(manageId);
            if(imRbFilePushManage != null){
                String pushLeaderId = imRbFilePushManage.getPushLeaderId();
                String filePath = imRbFilePushManage.getFilePath();
                String imgPath = imRbFilePushManage.getImgPath();
                String isPushImg = imRbFilePushManage.getIsPushImg()==null?"":imRbFilePushManage.getIsPushImg().trim();

                if(StringUtil.isNotEmpty(pushLeaderId)){
                    //字符数组转 整型List
                    String[] pushObjectIdArr = pushLeaderId.split(";");
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

                            imRbFilePushManage.setPushObjectName(pushObjectName);
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

                        //推送文件
                        if(filePath != null && filePath.trim().length() >0){
                            File file = new File(filePath);
                            inputStream = new FileInputStream(file);
                            String originalFilename = file.getName();
                            String contentType = "application/octet-stream"; // 根据文件类型修改
                            MultipartFile multipartFile = new MockMultipartFile("file", originalFilename, contentType, inputStream);
                            inputStream.close();

                            //执行推送操作 sendImFile(MultipartFile file,String targetType,String target,String fileType)
                            String targetType = "";
                            String target = "";
                            String fileType ="pushFile";

                            for(String strPushObjectId : arrPushObjectId){
                                target = strPushObjectId;
                                //用户
                                if(strPushObjectId.indexOf("[") >=0 && strPushObjectId.indexOf("]") >=0){
                                    targetType = "users";
                                }else{
                                    targetType = "group";
                                }

                                String sendResult = imUtil.sendImFile(multipartFile,targetType,target,fileType);
                                log.info("++++++++++{}_推送至领导文件推送结果：{}",originalFilename,sendResult);
                                if(StringUtil.isNotEmpty(sendResult)){
                                    JSONObject bindingJson = JSONObject.parseObject(sendResult);
                                    String status = bindingJson.getString("status");
                                    //推送成功 推送图片为 1
                                    if(StringUtil.isNotEmpty(status) && "success".equals(status)){
                                        /*imRbFilePushManage.setFilePushTime(LocalDateTime.now());
                                        imRbFilePushManageMapper.updateById(imRbFilePushManage);
                                        retJson.put("retCode","0");
                                        retJson.put("retMsg","推送成功！");
                                        retJson.put("addRetMsg","推送成功: "+sendResult);*/
                                        //推送图片
                                        if(isPushImg.equals("1")){
                                            //推送图片
                                            if(imgPath != null && imgPath.trim().length() >0){
                                                //Thread.sleep(2000);
                                                File imgFile = new File(imgPath);
                                                inputStream = new FileInputStream(imgFile);
                                                String imgFilename = imgFile.getName();
                                                String imgContentType = "application/octet-stream"; // 根据文件类型修改
                                                MultipartFile imgMultipartFile = new MockMultipartFile("file", imgFilename, imgContentType, inputStream);
                                                inputStream.close();

                                                //执行推送操作 sendImFile(MultipartFile file,String targetType,String target,String fileType)
                                                fileType ="pushImg";

                                                String sendImgResult = imUtil.sendImFile(imgMultipartFile,targetType,target,fileType);
                                                log.info("++++++++++{}_推送至领导图片推送结果：{}",imgFilename,sendImgResult);
                                                if(StringUtil.isNotEmpty(sendImgResult)){
                                                    JSONObject sendImgJson = JSONObject.parseObject(sendImgResult);
                                                    String imgStatus = sendImgJson.getString("status");
                                                    //推送成功
                                                    if(StringUtil.isNotEmpty(imgStatus) && "success".equals(imgStatus)){
                                                        imRbFilePushManage.setFilePushTime(LocalDateTime.now());
                                                        imRbFilePushManageMapper.updateById(imRbFilePushManage);
                                                        retJson.put("retCode","0");
                                                        retJson.put("retMsg","推送成功！");
                                                        retJson.put("addRetMsg","推送成功："+sendResult+" -- "+sendImgResult);
                                                    }else{
                                                        retJson.put("retCode","-1");
                                                        retJson.put("retMsg","图片推送失败!");
                                                        retJson.put("addRetMsg","图片推送失败："+sendImgResult);
                                                        return retJson;
                                                    }
                                                }else{
                                                    retJson.put("retCode","-1");
                                                    retJson.put("retMsg","图片推送接口返回空");
                                                    retJson.put("addRetMsg","图片推送接口返回空");
                                                    return retJson;
                                                }
                                            }
                                        }else{
                                            imRbFilePushManage.setFilePushTime(LocalDateTime.now());
                                            imRbFilePushManageMapper.updateById(imRbFilePushManage);

                                            retJson.put("retCode","0");
                                            retJson.put("retMsg","推送成功！");
                                            retJson.put("addRetMsg","推送成功："+sendResult);
                                        }

                                    }else{
                                        retJson.put("retCode","-1");
                                        retJson.put("retMsg","文件推送失败!");
                                        retJson.put("addRetMsg","文件推送失败："+sendResult);
                                        return retJson;
                                    }
                                }else{
                                    retJson.put("retCode","-1");
                                    retJson.put("retMsg","文件推送接口返回空");
                                    retJson.put("addRetMsg","文件推送接口返回空");
                                    return retJson;
                                }
                            }
                        }

                    }else{
                        retJson.put("retCode","-1");
                        retJson.put("retMsg","推送失败，请检查是否设置绑定对象!");
                        retJson.put("addRetMsg","推送失败，请检查是否设置绑定对象!");
                    }
                }else{
                    retJson.put("retCode","-1");
                    retJson.put("retMsg","推送失败，请检查是否设置绑定对象!");
                    retJson.put("addRetMsg","推送失败，请检查是否设置绑定对象!");
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
            if(imRbFilePushManage != null){
                //保存操作日志
                saveImOperateLog(imRbFilePushManage,retJson,"pushLeader",manageId);
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
    public JSONObject pushLeaderNew(String param){
        JSONObject retJsonObject = new JSONObject();
        ImHdjkPushManage imHdjkPushManage = null;
        int manageId =0;
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            manageId = paramJson.getInteger("manageId");
            imHdjkPushManage = imRbFilePushManageMapper.selectById(manageId);
            if(imHdjkPushManage != null){
                String pushLeaderId = imHdjkPushManage.getPushLeaderId();
                String filePath = imHdjkPushManage.getFilePath();
                String imgPath = imHdjkPushManage.getImgPath();
                String isPushImg = imHdjkPushManage.getIsPushImg()==null?"":imHdjkPushManage.getIsPushImg().trim();

                if(StringUtil.isNotEmpty(pushLeaderId)){
                    List<Integer> pushObjectIdList = Arrays.stream(pushLeaderId.split(";"))
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());

                    //根据推送对象管理id集合，查询推送对象集合
                    List<ImPushObjectManage> imPushObjectManageList = imPushObjectManageMapper.selectListByList(pushObjectIdList);
                    if(imPushObjectManageList != null && imPushObjectManageList.size() >0){
                        //获取推送对象信息
                        Map<String,String> pushObjectManageMap = getPushObjectMap(imPushObjectManageList);
                        if(pushObjectManageMap != null && pushObjectManageMap.size() >0){

                            String pushNames = pushObjectManageMap.get("pushNames")==null?"":pushObjectManageMap.get("pushNames");
                            imHdjkPushManage.setPushObjectName(pushNames);

                            //给用户推送IM文件
                            String pushUserId = pushObjectManageMap.get("userPushId");
                            if(StringUtil.isNotEmpty(pushUserId)){
                                //推送Im文件给用户
                                JSONObject pushUserFileJson = pushUserFileToIm(filePath,pushUserId,"pushFile");
                                String pushUserFileCode = pushUserFileJson.getString("retCode");
                                if(pushUserFileCode.equals("0")){ //推送文件成功
                                    if(isPushImg.equals("1")){ //需要推送图片
                                        JSONObject pushUserImgJson = pushUserFileToIm(imgPath,pushUserId,"pushImg");
                                        String pushUserImgCode = pushUserImgJson.getString("retCode");
                                        if(pushUserImgCode.equals("0")){ //推送图片成功
                                            retJsonObject.put("retCode","0");
                                            retJsonObject.put("retMsg","推送成功");
                                            retJsonObject.put("addRetMsg",pushUserImgJson.get("addRetMsg"));

                                            imHdjkPushManage.setFilePushLeaderTime(LocalDateTime.now());
                                            imRbFilePushManageMapper.updateById(imHdjkPushManage);
                                        }else{//推送图片失败
                                            retJsonObject.put("retCode","-1");
                                            retJsonObject.put("retMsg","推送失败");
                                            retJsonObject.put("addRetMsg",pushUserImgJson.get("addRetMsg"));
                                        }
                                    }else{//不需要推送图片
                                        retJsonObject.put("retCode","0");
                                        retJsonObject.put("retMsg","推送成功");
                                        retJsonObject.put("addRetMsg",pushUserFileJson.get("addRetMsg"));

                                        imHdjkPushManage.setFilePushLeaderTime(LocalDateTime.now());
                                        imRbFilePushManageMapper.updateById(imHdjkPushManage);
                                    }
                                }else{ //推送文件失败
                                    retJsonObject.put("retCode","-1");
                                    retJsonObject.put("retMsg","推送失败");

                                    retJsonObject.put("addRetMsg",pushUserFileJson.get("addRetMsg"));
                                }
                            }

                            //给群推送IM文件
                            String groupPushId = pushObjectManageMap.get("groupPushId");
                            if(StringUtil.isNotEmpty(groupPushId)){
                                //推送Im文件给群
                                JSONObject pushGroupFileJson = pushGroupFileToIm(filePath,groupPushId,"pushFile");
                                String pushGroupFileCode = pushGroupFileJson.getString("retCode");
                                if(pushGroupFileCode.equals("0")){ //推送文件成功
                                    if(isPushImg.equals("1")){ //需要推送图片
                                        JSONObject pushGroupImgJson = pushGroupFileToIm(imgPath,groupPushId,"pushImg");
                                        String pushGroupImgCode = pushGroupImgJson.getString("retCode");
                                        if(pushGroupImgCode.equals("0")){ //推送图片成功
                                            retJsonObject.put("retCode","0");
                                            retJsonObject.put("retMsg","推送成功");
                                            retJsonObject.put("addRetMsg",pushGroupImgJson.get("addRetMsg"));

                                            imHdjkPushManage.setFilePushLeaderTime(LocalDateTime.now());
                                            imRbFilePushManageMapper.updateById(imHdjkPushManage);
                                        }else{//推送图片失败
                                            retJsonObject.put("retCode","-1");
                                            retJsonObject.put("retMsg","推送失败");
                                            retJsonObject.put("addRetMsg",pushGroupImgJson.get("addRetMsg"));
                                        }
                                    }else{//不需要推送图片
                                        retJsonObject.put("retCode","0");
                                        retJsonObject.put("retMsg","推送成功");
                                        retJsonObject.put("addRetMsg",pushGroupFileJson.get("addRetMsg"));

                                        imHdjkPushManage.setFilePushLeaderTime(LocalDateTime.now());
                                        imRbFilePushManageMapper.updateById(imHdjkPushManage);
                                    }
                                }else{ //推送文件失败
                                    retJsonObject.put("retCode","-1");
                                    retJsonObject.put("retMsg","推送失败");
                                    retJsonObject.put("addRetMsg",pushGroupFileJson.get("addRetMsg"));
                                }
                            }

                        }
                    }else{
                        retJsonObject.put("retCode","-1");
                        retJsonObject.put("retMsg","未查询到推送对象信息");
                        retJsonObject.put("addRetMsg","未查询到推送对象信息");
                    }

                }else{
                    retJsonObject.put("retCode","-1");
                    retJsonObject.put("retMsg","推送对象为空");
                    retJsonObject.put("addRetMsg","推送对象为空");
                }

            }else{
                retJsonObject.put("retCode","-1");
                retJsonObject.put("retMsg","未查询到相关信息");
                retJsonObject.put("addRetMsg","未查询到相关信息");
            }

        }catch (Exception e){
            e.printStackTrace();
            retJsonObject.put("retCode","500");
            retJsonObject.put("retMsg","系统异常,请稍后再试！");
            retJsonObject.put("addRetMsg","系统异常: "+e.getMessage());
        }finally {
            if(imHdjkPushManage != null){
                //保存操作日志
                saveImOperateLog(imHdjkPushManage,retJsonObject,"pushLeader",manageId);
            }
        }
        return retJsonObject;
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
        ImHdjkPushManage imRbFilePushManage = null;
        int iManageId = 0;
        try{
            JSONObject paramJson = JSONObject.parseObject(param);
            String manageIds = paramJson.getString("manageIds");
            if(StringUtil.isNotEmpty(manageIds)){
                String[] arrManageIds = manageIds.split(",");

                boolean isNext = true;
                for(int j=0;j<arrManageIds.length && isNext;j++){
                    String strManageId = arrManageIds[j];
                    iManageId = Integer.parseInt(strManageId);
                    imRbFilePushManage = imRbFilePushManageMapper.selectById(iManageId);
                    if(imRbFilePushManage != null){
                        String pushObjectId = imRbFilePushManage.getPushObjectId();
                        String filePath = imRbFilePushManage.getFilePath();
                        String imgPath = imRbFilePushManage.getImgPath();
                        String isPushImg = imRbFilePushManage.getIsPushImg()==null?"":imRbFilePushManage.getIsPushImg().trim();

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

                                    imRbFilePushManage.setPushObjectName(pushObjectName);
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

                                //推送文件
                                if(filePath != null && filePath.trim().length() >0){
                                    File file = new File(filePath);
                                    inputStream = new FileInputStream(file);
                                    String originalFilename = file.getName();
                                    String contentType = "application/octet-stream"; // 根据文件类型修改
                                    MultipartFile multipartFile = new MockMultipartFile("file", originalFilename, contentType, inputStream);
                                    inputStream.close();

                                    //执行推送操作 sendImFile(MultipartFile file,String targetType,String target,String fileType)
                                    String targetType = "";
                                    String target = "";
                                    String fileType ="pushFile";

                                    for(String strPushObjectId : arrPushObjectId){
                                        target = strPushObjectId;
                                        //用户
                                        if(strPushObjectId.indexOf("[") >=0 && strPushObjectId.indexOf("]") >=0){
                                            targetType = "users";
                                        }else{
                                            targetType = "group";
                                        }

                                        String sendResult = imUtil.sendImFile(multipartFile,targetType,target,fileType);
                                        log.info("++++++++++{}_文件推送结果：{}",originalFilename,sendResult);
                                        if(StringUtil.isNotEmpty(sendResult)){
                                            JSONObject bindingJson = JSONObject.parseObject(sendResult);
                                            String status = bindingJson.getString("status");
                                            //推送成功 推送图片为 1
                                            if(StringUtil.isNotEmpty(status) && "success".equals(status)){

                                                //推送图片
                                                if(isPushImg.equals("1")){
                                                    //推送图片
                                                    if(imgPath != null && imgPath.trim().length() >0){
                                                        //Thread.sleep(2000);
                                                        File imgFile = new File(imgPath);
                                                        inputStream = new FileInputStream(imgFile);
                                                        String imgFilename = imgFile.getName();
                                                        String imgContentType = "application/octet-stream"; // 根据文件类型修改
                                                        MultipartFile imgMultipartFile = new MockMultipartFile("file", imgFilename, imgContentType, inputStream);
                                                        inputStream.close();

                                                        //执行推送操作 sendImFile(MultipartFile file,String targetType,String target,String fileType)
                                                        fileType ="pushImg";

                                                        String sendImgResult = imUtil.sendImFile(imgMultipartFile,targetType,target,fileType);
                                                        log.info("++++++++++{}_图片推送结果：{}",imgFilename,sendImgResult);
                                                        if(StringUtil.isNotEmpty(sendImgResult)){
                                                            JSONObject sendImgJson = JSONObject.parseObject(sendImgResult);
                                                            String imgStatus = sendImgJson.getString("status");
                                                            //推送成功
                                                            if(StringUtil.isNotEmpty(imgStatus) && "success".equals(imgStatus)){
                                                                imRbFilePushManage.setFilePushTime(LocalDateTime.now());
                                                                imRbFilePushManageMapper.updateById(imRbFilePushManage);
                                                                retJson.put("retCode","0");
                                                                retJson.put("retMsg","推送成功！");
                                                                retJson.put("addRetMsg","推送成功："+sendResult+" -- "+sendImgResult);
                                                            }else{
                                                                retJson.put("retCode","-1");
                                                                retJson.put("retMsg","图片推送失败!");
                                                                retJson.put("addRetMsg","图片推送失败："+sendImgResult);
                                                                isNext = false;
                                                                break;
                                                            }
                                                        }else{
                                                            retJson.put("retCode","-1");
                                                            retJson.put("retMsg","图片推送接口返回空");
                                                            retJson.put("addRetMsg","图片推送接口返回空");
                                                            isNext = false;
                                                            break;
                                                        }
                                                    }
                                                }else{
                                                    imRbFilePushManage.setFilePushTime(LocalDateTime.now());
                                                    imRbFilePushManageMapper.updateById(imRbFilePushManage);

                                                    retJson.put("retCode","0");
                                                    retJson.put("retMsg","推送成功！");
                                                    retJson.put("addRetMsg","推送成功："+sendResult);
                                                }

                                            }else{
                                                retJson.put("retCode","-1");
                                                retJson.put("retMsg","文件推送失败!");
                                                retJson.put("addRetMsg","文件推送失败："+sendResult);
                                                isNext = false;
                                                break;

                                            }
                                        }else{
                                            retJson.put("retCode","-1");
                                            retJson.put("retMsg","文件推送接口返回空");
                                            retJson.put("addRetMsg","文件推送接口返回空");
                                            isNext = false;
                                            break;

                                        }
                                    }
                                }

                            }else{
                                retJson.put("retCode","-1");
                                retJson.put("retMsg","推送失败，请检查是否设置绑定对象!");
                                retJson.put("addRetMsg","推送失败，请检查是否设置绑定对象!");
                            }
                        }else{
                            retJson.put("retCode","-1");
                            retJson.put("retMsg","推送失败，请检查是否设置绑定对象!");
                            retJson.put("addRetMsg","推送失败，请检查是否设置绑定对象!");
                        }
                    }

                    //保存操作日志
                    if(imRbFilePushManage != null){
                        saveImOperateLog(imRbFilePushManage,retJson,"push",iManageId);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","500");
            retJson.put("retMsg","系统异常,请稍后再试！");
            retJson.put("addRetMsg","系统异常: "+e.getMessage());
            //保存操作日志
            if(imRbFilePushManage != null){
                saveImOperateLog(imRbFilePushManage,retJson,"push",iManageId);
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
     * 批量推送
     * @param param
     * @return
     */
    @Override
    public JSONObject batchPushNew(String param){
        JSONObject retJsonObject = new JSONObject();
        ImHdjkPushManage imHdjkPushManage = null;
        int iManageId = 0;
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            String manageIds = paramJson.getString("manageIds");
            String[] arrManageIds = manageIds.split(",");

            boolean isNext = true;
            for(int j=0;j<arrManageIds.length && isNext;j++){
                String strManageId = arrManageIds[j];
                iManageId = Integer.parseInt(strManageId);
                imHdjkPushManage = imRbFilePushManageMapper.selectById(iManageId);
                if(imHdjkPushManage != null){
                    String pushObjectId = imHdjkPushManage.getPushObjectId();
                    String filePath = imHdjkPushManage.getFilePath();
                    String imgPath = imHdjkPushManage.getImgPath();
                    String isPushImg = imHdjkPushManage.getIsPushImg()==null?"":imHdjkPushManage.getIsPushImg().trim();

                    if(StringUtil.isNotEmpty(pushObjectId)){
                        List<Integer> pushObjectIdList = Arrays.stream(pushObjectId.split(";"))
                                .map(Integer::parseInt)
                                .collect(Collectors.toList());

                        //根据推送对象管理id集合，查询推送对象集合
                        List<ImPushObjectManage> imPushObjectManageList = imPushObjectManageMapper.selectListByList(pushObjectIdList);
                        if(imPushObjectManageList != null && imPushObjectManageList.size() >0){
                            //获取推送对象信息
                            Map<String,String> pushObjectManageMap = getPushObjectMap(imPushObjectManageList);
                            if(pushObjectManageMap != null && pushObjectManageMap.size() >0){

                                String pushNames = pushObjectManageMap.get("pushNames")==null?"":pushObjectManageMap.get("pushNames");
                                imHdjkPushManage.setPushObjectName(pushNames);

                                //给用户推送IM文件
                                String pushUserId = pushObjectManageMap.get("userPushId");
                                if(StringUtil.isNotEmpty(pushUserId)){
                                    //推送Im文件给用户
                                    JSONObject pushUserFileJson = pushUserFileToIm(filePath,pushUserId,"pushFile");
                                    String pushUserFileCode = pushUserFileJson.getString("retCode");
                                    if(pushUserFileCode.equals("0")){ //推送文件成功
                                        if(isPushImg.equals("1")){ //需要推送图片
                                            JSONObject pushUserImgJson = pushUserFileToIm(imgPath,pushUserId,"pushImg");
                                            String pushUserImgCode = pushUserImgJson.getString("retCode");
                                            if(pushUserImgCode.equals("0")){ //推送图片成功
                                                retJsonObject.put("retCode","0");
                                                retJsonObject.put("retMsg","推送成功");
                                                retJsonObject.put("addRetMsg",pushUserImgJson.get("addRetMsg"));

                                                imHdjkPushManage.setFilePushTime(LocalDateTime.now());
                                                imRbFilePushManageMapper.updateById(imHdjkPushManage);
                                            }else{//推送图片失败
                                                retJsonObject.put("retCode","-1");
                                                retJsonObject.put("retMsg","推送失败");
                                                retJsonObject.put("addRetMsg",pushUserImgJson.get("addRetMsg"));

                                                isNext = false;
                                                break;
                                            }
                                        }else{//不需要推送图片
                                            retJsonObject.put("retCode","0");
                                            retJsonObject.put("retMsg","推送成功");
                                            retJsonObject.put("addRetMsg",pushUserFileJson.get("addRetMsg"));

                                            imHdjkPushManage.setFilePushTime(LocalDateTime.now());
                                            imRbFilePushManageMapper.updateById(imHdjkPushManage);
                                        }
                                    }else{ //推送文件失败
                                        retJsonObject.put("retCode","-1");
                                        retJsonObject.put("retMsg","推送失败");
                                        retJsonObject.put("addRetMsg",pushUserFileJson.get("addRetMsg"));

                                        isNext = false;
                                        break;
                                    }
                                }

                                //给群推送IM文件
                                String groupPushId = pushObjectManageMap.get("groupPushId");
                                if(StringUtil.isNotEmpty(groupPushId)){
                                    //推送Im文件给群
                                    JSONObject pushGroupFileJson = pushGroupFileToIm(filePath,groupPushId,"pushFile");
                                    String pushGroupFileCode = pushGroupFileJson.getString("retCode");
                                    if(pushGroupFileCode.equals("0")){ //推送文件成功
                                        if(isPushImg.equals("1")){ //需要推送图片
                                            JSONObject pushGroupImgJson = pushGroupFileToIm(imgPath,groupPushId,"pushImg");
                                            String pushGroupImgCode = pushGroupImgJson.getString("retCode");
                                            if(pushGroupImgCode.equals("0")){ //推送图片成功
                                                retJsonObject.put("retCode","0");
                                                retJsonObject.put("retMsg","推送成功");
                                                retJsonObject.put("addRetMsg",pushGroupImgJson.get("addRetMsg"));

                                                imHdjkPushManage.setFilePushTime(LocalDateTime.now());
                                                imRbFilePushManageMapper.updateById(imHdjkPushManage);
                                            }else{//推送图片失败
                                                retJsonObject.put("retCode","-1");
                                                retJsonObject.put("retMsg","推送失败");
                                                retJsonObject.put("addRetMsg",pushGroupImgJson.get("addRetMsg"));

                                                isNext = false;
                                                break;
                                            }
                                        }else{//不需要推送图片
                                            retJsonObject.put("retCode","0");
                                            retJsonObject.put("retMsg","推送成功");
                                            retJsonObject.put("addRetMsg",pushGroupFileJson.get("addRetMsg"));

                                            imHdjkPushManage.setFilePushTime(LocalDateTime.now());
                                            imRbFilePushManageMapper.updateById(imHdjkPushManage);
                                        }
                                    }else{ //推送文件失败
                                        retJsonObject.put("retCode","-1");
                                        retJsonObject.put("retMsg","推送失败");
                                        retJsonObject.put("addRetMsg",pushGroupFileJson.get("addRetMsg"));

                                        isNext = false;
                                        break;
                                    }
                                }

                            }
                        }else{
                            retJsonObject.put("retCode","-1");
                            retJsonObject.put("retMsg","未查询到推送对象信息");
                            retJsonObject.put("addRetMsg","未查询到推送对象信息");
                        }

                    }else{
                        retJsonObject.put("retCode","-1");
                        retJsonObject.put("retMsg","推送对象为空");
                        retJsonObject.put("addRetMsg","推送对象为空");
                    }

                }else{
                    retJsonObject.put("retCode","-1");
                    retJsonObject.put("retMsg","未查询到相关信息");
                    retJsonObject.put("addRetMsg","未查询到相关信息");
                }

                //保存操作日志
                if(imHdjkPushManage != null){
                    saveImOperateLog(imHdjkPushManage,retJsonObject,"push",iManageId);
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return retJsonObject;
    }

    /**
     * 批量推送至领导
     * @param param
     * @return
     */
    @Override
    public JSONObject batchLeaderPush(String param) {
        JSONObject retJson  = new JSONObject();
        FileInputStream inputStream =null;
        ImHdjkPushManage imRbFilePushManage = null;
        int iManageId = 0;
        try{
            JSONObject paramJson = JSONObject.parseObject(param);
            String manageIds = paramJson.getString("manageIds");
            if(StringUtil.isNotEmpty(manageIds)){
                String[] arrManageIds = manageIds.split(",");

                boolean isNext = true;
                for(int j=0;j<arrManageIds.length && isNext;j++){
                    String strManageId = arrManageIds[j];
                    iManageId = Integer.parseInt(strManageId);
                    imRbFilePushManage = imRbFilePushManageMapper.selectById(iManageId);
                    if(imRbFilePushManage != null){
                        String pushObjectId = imRbFilePushManage.getPushLeaderId();
                        String filePath = imRbFilePushManage.getFilePath();
                        String imgPath = imRbFilePushManage.getImgPath();
                        String isPushImg = imRbFilePushManage.getIsPushImg()==null?"":imRbFilePushManage.getIsPushImg().trim();

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

                                    imRbFilePushManage.setPushObjectName(pushObjectName);
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

                                //推送文件
                                if(filePath != null && filePath.trim().length() >0){
                                    File file = new File(filePath);
                                    inputStream = new FileInputStream(file);
                                    String originalFilename = file.getName();
                                    String contentType = "application/octet-stream"; // 根据文件类型修改
                                    MultipartFile multipartFile = new MockMultipartFile("file", originalFilename, contentType, inputStream);
                                    inputStream.close();

                                    //执行推送操作 sendImFile(MultipartFile file,String targetType,String target,String fileType)
                                    String targetType = "";
                                    String target = "";
                                    String fileType ="pushFile";

                                    for(String strPushObjectId : arrPushObjectId){
                                        target = strPushObjectId;
                                        //用户
                                        if(strPushObjectId.indexOf("[") >=0 && strPushObjectId.indexOf("]") >=0){
                                            targetType = "users";
                                        }else{
                                            targetType = "group";
                                        }

                                        String sendResult = imUtil.sendImFile(multipartFile,targetType,target,fileType);
                                        log.info("++++++++++{}_文件推送结果：{}",originalFilename,sendResult);
                                        if(StringUtil.isNotEmpty(sendResult)){
                                            JSONObject bindingJson = JSONObject.parseObject(sendResult);
                                            String status = bindingJson.getString("status");
                                            //推送成功 推送图片为 1
                                            if(StringUtil.isNotEmpty(status) && "success".equals(status)){

                                                //推送图片
                                                if(isPushImg.equals("1")){
                                                    //推送图片
                                                    if(imgPath != null && imgPath.trim().length() >0){
                                                        //Thread.sleep(2000);
                                                        File imgFile = new File(imgPath);
                                                        inputStream = new FileInputStream(imgFile);
                                                        String imgFilename = imgFile.getName();
                                                        String imgContentType = "application/octet-stream"; // 根据文件类型修改
                                                        MultipartFile imgMultipartFile = new MockMultipartFile("file", imgFilename, imgContentType, inputStream);
                                                        inputStream.close();

                                                        //执行推送操作 sendImFile(MultipartFile file,String targetType,String target,String fileType)
                                                        fileType ="pushImg";

                                                        String sendImgResult = imUtil.sendImFile(imgMultipartFile,targetType,target,fileType);
                                                        log.info("++++++++++{}_图片推送结果：{}",imgFilename,sendImgResult);
                                                        if(StringUtil.isNotEmpty(sendImgResult)){
                                                            JSONObject sendImgJson = JSONObject.parseObject(sendImgResult);
                                                            String imgStatus = sendImgJson.getString("status");
                                                            //推送成功
                                                            if(StringUtil.isNotEmpty(imgStatus) && "success".equals(imgStatus)){
                                                                imRbFilePushManage.setFilePushTime(LocalDateTime.now());
                                                                imRbFilePushManageMapper.updateById(imRbFilePushManage);
                                                                retJson.put("retCode","0");
                                                                retJson.put("retMsg","推送成功！");
                                                                retJson.put("addRetMsg","推送成功："+sendResult+" -- "+sendImgResult);
                                                            }else{
                                                                retJson.put("retCode","-1");
                                                                retJson.put("retMsg","图片推送失败!");
                                                                retJson.put("addRetMsg","图片推送失败："+sendImgResult);
                                                                isNext = false;
                                                                break;
                                                            }
                                                        }else{
                                                            retJson.put("retCode","-1");
                                                            retJson.put("retMsg","图片推送接口返回空");
                                                            retJson.put("addRetMsg","图片推送接口返回空");
                                                            isNext = false;
                                                            break;
                                                        }
                                                    }
                                                }else{
                                                    imRbFilePushManage.setFilePushTime(LocalDateTime.now());
                                                    imRbFilePushManageMapper.updateById(imRbFilePushManage);

                                                    retJson.put("retCode","0");
                                                    retJson.put("retMsg","推送成功！");
                                                    retJson.put("addRetMsg","推送成功："+sendResult);
                                                }

                                            }else{
                                                retJson.put("retCode","-1");
                                                retJson.put("retMsg","文件推送失败!");
                                                retJson.put("addRetMsg","文件推送失败："+sendResult);
                                                isNext = false;
                                                break;

                                            }
                                        }else{
                                            retJson.put("retCode","-1");
                                            retJson.put("retMsg","文件推送接口返回空");
                                            retJson.put("addRetMsg","文件推送接口返回空");
                                            isNext = false;
                                            break;

                                        }
                                    }
                                }

                            }else{
                                retJson.put("retCode","-1");
                                retJson.put("retMsg","推送失败，请检查是否设置绑定对象!");
                                retJson.put("addRetMsg","推送失败，请检查是否设置绑定对象!");
                            }
                        }else{
                            retJson.put("retCode","-1");
                            retJson.put("retMsg","推送失败，请检查是否设置绑定对象!");
                            retJson.put("addRetMsg","推送失败，请检查是否设置绑定对象!");
                        }
                    }

                    //保存操作日志
                    if(imRbFilePushManage != null){
                        saveImOperateLog(imRbFilePushManage,retJson,"pushLeader",iManageId);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","500");
            retJson.put("retMsg","系统异常,请稍后再试！");
            retJson.put("addRetMsg","系统异常: "+e.getMessage());
            //保存操作日志
            if(imRbFilePushManage != null){
                saveImOperateLog(imRbFilePushManage,retJson,"push",iManageId);
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
    public JSONObject batchLeaderPushNew(String param){
        JSONObject retJsonObject = new JSONObject();
        ImHdjkPushManage imHdjkPushManage = null;
        int iManageId = 0;
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            String manageIds = paramJson.getString("manageIds");
            String[] arrManageIds = manageIds.split(",");

            boolean isNext = true;
            for(int j=0;j<arrManageIds.length && isNext;j++){
                String strManageId = arrManageIds[j];
                iManageId = Integer.parseInt(strManageId);
                imHdjkPushManage = imRbFilePushManageMapper.selectById(iManageId);
                if(imHdjkPushManage != null){
                    String pushLeaderId = imHdjkPushManage.getPushLeaderId();
                    String filePath = imHdjkPushManage.getFilePath();
                    String imgPath = imHdjkPushManage.getImgPath();
                    String isPushImg = imHdjkPushManage.getIsPushImg()==null?"":imHdjkPushManage.getIsPushImg().trim();

                    if(StringUtil.isNotEmpty(pushLeaderId)){
                        List<Integer> pushObjectIdList = Arrays.stream(pushLeaderId.split(";"))
                                .map(Integer::parseInt)
                                .collect(Collectors.toList());

                        //根据推送对象管理id集合，查询推送对象集合
                        List<ImPushObjectManage> imPushObjectManageList = imPushObjectManageMapper.selectListByList(pushObjectIdList);
                        if(imPushObjectManageList != null && imPushObjectManageList.size() >0){
                            //获取推送对象信息
                            Map<String,String> pushObjectManageMap = getPushObjectMap(imPushObjectManageList);
                            if(pushObjectManageMap != null && pushObjectManageMap.size() >0){

                                String pushNames = pushObjectManageMap.get("pushNames")==null?"":pushObjectManageMap.get("pushNames");
                                imHdjkPushManage.setPushObjectName(pushNames);

                                //给用户推送IM文件
                                String pushUserId = pushObjectManageMap.get("userPushId");
                                if(StringUtil.isNotEmpty(pushUserId)){
                                    //推送Im文件给用户
                                    JSONObject pushUserFileJson = pushUserFileToIm(filePath,pushUserId,"pushFile");
                                    String pushUserFileCode = pushUserFileJson.getString("retCode");
                                    if(pushUserFileCode.equals("0")){ //推送文件成功
                                        if(isPushImg.equals("1")){ //需要推送图片
                                            JSONObject pushUserImgJson = pushUserFileToIm(imgPath,pushUserId,"pushImg");
                                            String pushUserImgCode = pushUserImgJson.getString("retCode");
                                            if(pushUserImgCode.equals("0")){ //推送图片成功
                                                retJsonObject.put("retCode","0");
                                                retJsonObject.put("retMsg","推送成功");
                                                retJsonObject.put("addRetMsg",pushUserImgJson.get("addRetMsg"));

                                                imHdjkPushManage.setFilePushTime(LocalDateTime.now());
                                                imRbFilePushManageMapper.updateById(imHdjkPushManage);
                                            }else{//推送图片失败
                                                retJsonObject.put("retCode","-1");
                                                retJsonObject.put("retMsg","推送失败");
                                                retJsonObject.put("addRetMsg",pushUserImgJson.get("addRetMsg"));

                                                isNext = false;
                                                break;
                                            }
                                        }else{//不需要推送图片
                                            retJsonObject.put("retCode","0");
                                            retJsonObject.put("retMsg","推送成功");
                                            retJsonObject.put("addRetMsg",pushUserFileJson.get("addRetMsg"));

                                            imHdjkPushManage.setFilePushTime(LocalDateTime.now());
                                            imRbFilePushManageMapper.updateById(imHdjkPushManage);
                                        }
                                    }else{ //推送文件失败
                                        retJsonObject.put("retCode","-1");
                                        retJsonObject.put("retMsg","推送失败");
                                        retJsonObject.put("addRetMsg",pushUserFileJson.get("addRetMsg"));

                                        isNext = false;
                                        break;
                                    }
                                }

                                //给群推送IM文件
                                String groupPushId = pushObjectManageMap.get("groupPushId");
                                if(StringUtil.isNotEmpty(groupPushId)){
                                    //推送Im文件给群
                                    JSONObject pushGroupFileJson = pushGroupFileToIm(filePath,groupPushId,"pushFile");
                                    String pushGroupFileCode = pushGroupFileJson.getString("retCode");
                                    if(pushGroupFileCode.equals("0")){ //推送文件成功
                                        if(isPushImg.equals("1")){ //需要推送图片
                                            JSONObject pushGroupImgJson = pushGroupFileToIm(imgPath,groupPushId,"pushImg");
                                            String pushGroupImgCode = pushGroupImgJson.getString("retCode");
                                            if(pushGroupImgCode.equals("0")){ //推送图片成功
                                                retJsonObject.put("retCode","0");
                                                retJsonObject.put("retMsg","推送成功");
                                                retJsonObject.put("addRetMsg",pushGroupImgJson.get("addRetMsg"));

                                                imHdjkPushManage.setFilePushTime(LocalDateTime.now());
                                                imRbFilePushManageMapper.updateById(imHdjkPushManage);
                                            }else{//推送图片失败
                                                retJsonObject.put("retCode","-1");
                                                retJsonObject.put("retMsg","推送失败");
                                                retJsonObject.put("addRetMsg",pushGroupImgJson.get("addRetMsg"));

                                                isNext = false;
                                                break;
                                            }
                                        }else{//不需要推送图片
                                            retJsonObject.put("retCode","0");
                                            retJsonObject.put("retMsg","推送成功");
                                            retJsonObject.put("addRetMsg",pushGroupFileJson.get("addRetMsg"));

                                            imHdjkPushManage.setFilePushTime(LocalDateTime.now());
                                            imRbFilePushManageMapper.updateById(imHdjkPushManage);
                                        }
                                    }else{ //推送文件失败
                                        retJsonObject.put("retCode","-1");
                                        retJsonObject.put("retMsg","推送失败");
                                        retJsonObject.put("addRetMsg",pushGroupFileJson.get("addRetMsg"));

                                        isNext = false;
                                        break;
                                    }
                                }

                            }
                        }else{
                            retJsonObject.put("retCode","-1");
                            retJsonObject.put("retMsg","未查询到推送对象信息");
                            retJsonObject.put("addRetMsg","未查询到推送对象信息");
                        }

                    }else{
                        retJsonObject.put("retCode","-1");
                        retJsonObject.put("retMsg","推送对象为空");
                        retJsonObject.put("addRetMsg","推送对象为空");
                    }

                }else{
                    retJsonObject.put("retCode","-1");
                    retJsonObject.put("retMsg","未查询到相关信息");
                    retJsonObject.put("addRetMsg","未查询到相关信息");
                }

                //保存操作日志
                if(imHdjkPushManage != null){
                    saveImOperateLog(imHdjkPushManage,retJsonObject,"pushLeader",iManageId);
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return retJsonObject;
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

        ImHdjkPushManage imRbFilePushManage = null;
        try {
            imRbFilePushManage = imRbFilePushManageMapper.selectById(manageId);
             if(imRbFilePushManage != null){
                 //获取文件名
                 //String fileName = file.getOriginalFilename();
                 String fileName = imRbFilePushManage.getFileName();
                 log.info("上传的文件名为：" + fileName);

                 // 文件路径
                 String filePath = imRbFilePushManage.getFilePath();

                 File dest = new File(filePath);

                 // 检测是否存在目录
                 if (!dest.getParentFile().exists()) {
                     dest.getParentFile().mkdirs();
                 }

                 file.transferTo(dest);

                 imRbFilePushManage.setFileName(fileName);
                 imRbFilePushManage.setFilePath(filePath);
                 imRbFilePushManage.setFileCreatTime(LocalDateTime.now());
                 imRbFilePushManageMapper.updateById(imRbFilePushManage);

                 result = true;

             }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            //保存操作日志
            if(imRbFilePushManage != null){
                ImOperateLog imOperateLog = new ImOperateLog();
                imOperateLog.setOperateType("upload");
                imOperateLog.setModelId(imRbFilePushManage.getModelId());
                imOperateLog.setModelName(imRbFilePushManage.getModelName());
                imOperateLog.setFileName(imRbFilePushManage.getFileName());
                imOperateLog.setObjectId(imRbFilePushManage.getPushObjectId());

                String objectIds = imRbFilePushManage.getPushObjectId();
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
                imOperateLog.setFileCreatTime(imRbFilePushManage.getFileCreatTime());
                imOperateLog.setOperateTime(LocalDateTime.now());
                imOperateLog.setFilePath(imRbFilePushManage.getFilePath());
                imOperateLog.setManageId(imRbFilePushManage.getManageId());

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
     * 校验文件生成时间
     * @param param
     * @return
     */
    @Override
    public JSONObject checkFileCreateTime(String param) {
        JSONObject retJsonObject = new JSONObject();
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            String manageIds = paramJson.getString("manageIds");
            String isPushLeader = paramJson.getString("isPushLeader");
            String isCheckLeader = paramJson.getString("isCheckLeader");
            if(StringUtil.isNotEmpty(manageIds)){
                List<Integer> list = Arrays.stream(manageIds.split(","))
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                List<ImHdjkPushManage> imHdjkPushManageList = imRbFilePushManageMapper.selectBatchIds(list);
                if(imHdjkPushManageList != null && imHdjkPushManageList.size() >0){
                    for(ImHdjkPushManage imHdjkPushManage : imHdjkPushManageList){
                       LocalDateTime fileCreateTime = imHdjkPushManage.getFileCreatTime();
                       if(fileCreateTime == null){
                           retJsonObject.put("retCode","-1");
                           retJsonObject.put("retMsg","文件未生成");
                          break;
                       }

                        if(isPushLeader.equals("1")){
                            String queryIsPushLeader = imHdjkPushManage.getIsPushLeader();
                            if(StringUtil.isNotEmpty(queryIsPushLeader) && queryIsPushLeader.equals("0")){
                                retJsonObject.put("retCode","-1");
                                retJsonObject.put("retMsg","不能推送至领导");
                                break;
                            }
                        }

                       if(isCheckLeader.equals("1")){
                            String pushLeaderId = imHdjkPushManage.getPushLeaderId();
                            if(StringUtil.isEmpty(pushLeaderId)){
                                retJsonObject.put("retCode","-1");
                                retJsonObject.put("retMsg","推送领导不能为空");
                                break;
                            }
                       }

                        retJsonObject.put("retCode","0");
                        retJsonObject.put("retMsg","成功");
                    }

                }else{
                    retJsonObject.put("retCode","-1");
                    retJsonObject.put("retMsg","未查询到相关信息");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return retJsonObject;
    }


    /**
     * 保存IM操作日志
     * @param imHdjkPushManage
     * @param retJson
     * @param manageId
     */
    private void saveImOperateLog(ImHdjkPushManage imHdjkPushManage,JSONObject retJson,String operateType,int manageId){
        //保存操作日志
        if(imHdjkPushManage != null){
            ImOperateLog imOperateLog = new ImOperateLog();
            imOperateLog.setOperateType(operateType);
            imOperateLog.setModelId(imHdjkPushManage.getModelId());
            imOperateLog.setModelName(imHdjkPushManage.getModelName());
            imOperateLog.setFileName(imHdjkPushManage.getFileName());
            imOperateLog.setObjectId(imHdjkPushManage.getPushObjectId());
            imOperateLog.setObjectName(imHdjkPushManage.getPushObjectName());
            imOperateLog.setFileCreatTime(imHdjkPushManage.getFileCreatTime());
            imOperateLog.setOperateTime(LocalDateTime.now());
            imOperateLog.setFilePath(imHdjkPushManage.getFilePath());
            imOperateLog.setManageId(manageId);

            String retCode = retJson.getString("retCode");
            String retMsg = retJson.getString("addRetMsg");

            if(StringUtil.isEmpty(retMsg)){
                retMsg = retJson.getString("retMsg");
            }
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

    /**
     * 根据推送对象的集合，获取推送对象Map
     * @param imPushObjectManageList
     * @return
     */
    public Map<String,String> getPushObjectMap(List<ImPushObjectManage> imPushObjectManageList){
        Map<String,String> retMap = new HashMap<>();
        try {
            if(imPushObjectManageList != null && imPushObjectManageList.size() >0){
                List<String> userList = new ArrayList<>();
                List<String> groupList = new ArrayList<>();
                String pushNames ="";
                for(ImPushObjectManage imPushObjectManage:imPushObjectManageList){
                    String pushObjectType = imPushObjectManage.getPushObjectType();
                    String pushObjectId = imPushObjectManage.getPushObjectId();
                    if(StringUtil.isNotEmpty(pushObjectType) && pushObjectType.equals("users")){ //用户
                        userList.add(pushObjectId);
                    }else if(StringUtil.isNotEmpty(pushObjectType) && pushObjectType.equals("group")){ //群
                        groupList.add(pushObjectId);
                    }

                    if(pushNames.equals("")){
                        pushNames = imPushObjectManage.getPushObjectName();
                    }else{
                        pushNames = pushNames + ";"+imPushObjectManage.getPushObjectName();
                    }
                }

                if(userList.size() >0){
                    String userPushId ="";
                    for(String userId : userList){
                        if(userPushId.equals("")){
                            userPushId = userId;
                        }else{
                            userPushId = userPushId+","+userId;
                        }
                    }

                    userPushId = "["+userPushId+"]";
                    retMap.put("userPushId",userPushId);
                }

                if(groupList.size() >0){
                    String groupPushId ="";
                    for(String groupId : groupList){
                        if(groupPushId.equals("")){
                            groupPushId = groupId;
                        }else{
                            groupPushId = groupPushId+";" +groupId;
                        }
                    }

                    retMap.put("groupPushId",groupPushId);

                }

                retMap.put("pushNames",pushNames);

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return retMap;
    }

    /**
     * 推送用户文件给IM
     * @return
     */
    public JSONObject pushUserFileToIm(String filePath,String target,String fileType){
        JSONObject retJSONObject = new JSONObject();
        FileInputStream inputStream = null;
        try {
            if(StringUtil.isNotEmpty(filePath)){
                File file = new File(filePath);
                inputStream = new FileInputStream(file);
                String originalFilename = file.getName();
                String contentType = "application/octet-stream"; // 根据文件类型修改
                MultipartFile multipartFile = new MockMultipartFile("file", originalFilename, contentType, inputStream);

                String sendResult = imUtil.sendImFile(multipartFile,"users",target,fileType);
                log.info("++++++++++{}_用户文件推送IM结果：{}",originalFilename,sendResult);
                if(StringUtil.isNotEmpty(sendResult)){
                    JSONObject bindingJson = JSONObject.parseObject(sendResult);
                    String status = bindingJson.getString("status");
                    if(StringUtil.isNotEmpty(status) && "success".equals(status)){
                        retJSONObject.put("retCode","0");
                        retJSONObject.put("retMsg","推送成功！");
                        retJSONObject.put("addRetMsg","推送成功："+sendResult);
                    }else{
                        retJSONObject.put("retCode","-1");
                        retJSONObject.put("retMsg","推送失败");
                        retJSONObject.put("addRetMsg","推送失败："+sendResult);
                    }

                }else{
                    retJSONObject.put("retCode","-1");
                    retJSONObject.put("retMsg","文件推送接口返回空");
                    retJSONObject.put("addRetMsg","文件推送接口返回空");
                }

            }else{
                retJSONObject.put("retCode","-1");
                retJSONObject.put("retMsg","推送文件路径为空");
                retJSONObject.put("addRetMsg","推送文件路径为空");
            }
        }catch (Exception e){
            e.printStackTrace();
            retJSONObject.put("retCode","-1");
            retJSONObject.put("retMsg","系统异常，请联系管理员");
            retJSONObject.put("addRetMsg","系统异常，请联系管理员");
        }finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return retJSONObject;
    }

    /**
     * 推送群文件给IM
     * @return
     */
    public JSONObject pushGroupFileToIm(String filePath,String target,String fileType){
        JSONObject retJSONObject = new JSONObject();
        FileInputStream inputStream = null;
        try {
            if(StringUtil.isNotEmpty(filePath)){
                File file = new File(filePath);
                inputStream = new FileInputStream(file);
                String originalFilename = file.getName();
                String contentType = "application/octet-stream"; // 根据文件类型修改
                MultipartFile multipartFile = new MockMultipartFile("file", originalFilename, contentType, inputStream);

                if(StringUtil.isNotEmpty(target)){
                    List<String> grouplist = Arrays.stream(target.split(";"))
                            .map(String::toString)
                            .collect(Collectors.toList());

                    if(grouplist != null && grouplist.size() >0){
                        for(String groupId:grouplist){
                            String sendResult = imUtil.sendImFile(multipartFile,"group",groupId,fileType);
                            log.info("++++++++++{}_群文件推送IM结果：{}",originalFilename,sendResult);
                            if(StringUtil.isNotEmpty(sendResult)){
                                JSONObject bindingJson = JSONObject.parseObject(sendResult);
                                String status = bindingJson.getString("status");
                                if(StringUtil.isNotEmpty(status) && "success".equals(status)){
                                    retJSONObject.put("retCode","0");
                                    retJSONObject.put("retMsg","推送成功！");
                                    retJSONObject.put("addRetMsg","推送成功："+sendResult);
                                }else{
                                    retJSONObject.put("retCode","-1");
                                    retJSONObject.put("retMsg","推送失败");
                                    retJSONObject.put("addRetMsg","推送失败："+sendResult);
                                    break;
                                }

                            }else{
                                retJSONObject.put("retCode","-1");
                                retJSONObject.put("retMsg","文件推送接口返回空");
                                retJSONObject.put("addRetMsg","文件推送接口返回空");
                                break;
                            }
                        }
                    }
                }
            }else{
                retJSONObject.put("retCode","-1");
                retJSONObject.put("retMsg","推送文件路径为空");
                retJSONObject.put("addRetMsg","推送文件路径为空");
            }
        }catch (Exception e){
            e.printStackTrace();
            retJSONObject.put("retCode","-1");
            retJSONObject.put("retMsg","系统异常，请联系管理员");
            retJSONObject.put("addRetMsg","系统异常，请联系管理员");
        }finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return retJSONObject;
    }



}
