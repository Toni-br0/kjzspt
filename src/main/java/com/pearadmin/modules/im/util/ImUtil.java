package com.pearadmin.modules.im.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.sys.domain.SysDictData;
import com.pearadmin.modules.sys.service.impl.SysDictDataServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 创建日期：2025-04-30
 * 描述：IM 工具类
 **/

@Component
@Slf4j
public class ImUtil {

    @Resource
    private  SysDictDataServiceImpl sysDictDataService;

    private static String baseServiceUrl = "";
    private static String xAppId = "";
    private static String xAppKey = "";
    private static String appKey = "";

    private static String from = "";
    private static String secret = "";
    private static String userPhone = "";



    public void init(){
        List<SysDictData> sysDictDataList = sysDictDataService.selectByCode("im_push_config");
        if(sysDictDataList != null && sysDictDataList.size() > 0){
            String dataLabel ="";
            for(SysDictData sysDictData : sysDictDataList){
                dataLabel = sysDictData.getDataLabel();
                if(dataLabel.equals("im_push_url")){
                    baseServiceUrl = sysDictData.getDataValue();
                }else if(dataLabel.equals("im_dcoos_x_app_id")){
                    xAppId = sysDictData.getDataValue();
                }else if(dataLabel.equals("im_dcoos_x_app_key")){
                    xAppKey = sysDictData.getDataValue();
                }else if(dataLabel.equals("im_push_app_key")){
                    appKey = sysDictData.getDataValue();
                }else if(dataLabel.equals("im_push_from")){
                    from = sysDictData.getDataValue();
                }else if(dataLabel.equals("im_push_secret")){
                    secret = sysDictData.getDataValue();
                }else if(dataLabel.equals("im_user_phone")){
                    userPhone = sysDictData.getDataValue();
                }
            }
        }
    }

    private String getAccessToken(){
        if(baseServiceUrl.equals("")){
            init();
        }
        JSONObject data = new JSONObject();
        data.put("appKey",appKey);
        data.put("secret",secret);
        String accessToken = JSON.parseObject(JSON.parseObject(httpRequest("/getToken",data,null,null)).getString("data")).getString("access_token");
        return accessToken;

    }


    /**
     * 绑定推送人员
     * @param userId
     * @return
     */
    public String relBind(String userId){
        if(baseServiceUrl.equals("")){
            init();
        }
        JSONObject data = new JSONObject();
        data.put("appKey",appKey);
        data.put("userId",userId);
        String service = "/relBind";
        return httpRequest(service,data,null, null);
    }

    /**
     * 发送消息
     * @param msg
     * @param targetType
     * @param target
     * @return
     */
    public String sendImMsg(String msg,String targetType,String target){

        if(baseServiceUrl.equals("")){
            init();
        }

        String accessToken = getAccessToken();
        if(StringUtil.isNotEmpty(accessToken)){
            JSONObject data = new JSONObject();
            JSONObject bizData = new JSONObject();
            bizData.put("target_type",targetType);
            bizData.put("target",target);
            bizData.put("from",from);
            bizData.put("type","common");
            bizData.put("msg",msg);
            data.put("access_token",accessToken);
            data.put("appKey",appKey);
            data.put("data",bizData);
            String service = "/pushMsg";
            return httpRequest(service,null,data,null);
        }

        return null;
    }

    /**
     * 发送文件
     * @param file
     * @param targetType
     * @param target
     * @param fileType
     * @return
     */
    public String sendImFile(MultipartFile file,String targetType,String target,String fileType){
        if(baseServiceUrl.equals("")){
            init();
        }
        String accessToken = getAccessToken();
        if(StringUtil.isNotEmpty(accessToken)){
            JSONObject data = new JSONObject();
            JSONObject bizData = new JSONObject();
            bizData.put("target_type",targetType);
            bizData.put("target",target);
            bizData.put("from",from);
            bizData.put("fileType",fileType);
            bizData.put("user",userPhone);
            data.put("access_token",accessToken);
            data.put("appKey",appKey);
            data.put("data",bizData);
            String service = "/pushFile";
            log.info("+++++++++++++++++++++发送IM文件参数：{}",data.toJSONString());
            return httpRequest(service,null,data,file);
        }

        return null;
    }


    public String httpRequest(String service, Map<String,Object> loginParams, Map<String,Object> bizParams, MultipartFile file){
        CloseableHttpClient httpClient  = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        String resp = "";
        try{

            HttpPost httpPost = new HttpPost(baseServiceUrl + service);
            httpPost.addHeader("X-APP-ID",xAppId);
            httpPost.addHeader("X-APP-KEY",xAppKey);
            if(file == null) {
                if (bizParams != null) {
                    List<NameValuePair> entityList = new ArrayList<>();
                    entityList.add(new BasicNameValuePair("paramStr", JSON.toJSONString(bizParams, SerializerFeature.BrowserCompatible)));
                    UrlEncodedFormEntity bizEntity = new UrlEncodedFormEntity(entityList);
                    httpPost.setEntity(bizEntity);
                }
                if (loginParams != null) {
                    List<NameValuePair> param = new ArrayList<>();
                    for (Map.Entry<String, Object> node : loginParams.entrySet()) {
                        param.add(new BasicNameValuePair(node.getKey(), node.getValue().toString()));
                    }
                    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(param, StandardCharsets.UTF_8);
                    httpPost.setEntity(entity);
                }
            } else {
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                if(bizParams != null) {
                    builder.addTextBody("paramStr", JSON.toJSONString(bizParams, SerializerFeature.BrowserCompatible));
                }
                if(file != null && file.getContentType() != null) {
                    builder.addBinaryBody("file", file.getInputStream(), ContentType.create(file.getContentType()), file.getOriginalFilename());
                }
                builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                builder.setCharset(StandardCharsets.UTF_8);
                HttpEntity fileEntity = builder.build();
                httpPost.setEntity(fileEntity);
            }
            response = httpClient.execute(httpPost);
            resp = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        }catch(Exception e){
            e.printStackTrace();
        }
        return resp;
    }

    /**
     * 推送文件给IM用户
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

                String sendResult = sendImFile(multipartFile,"users",target,fileType);
                //String sendResult ="{\"timestamp\":\"2026-03-21 23:53:39\",\"status\":\"success\",\"msg\":\"\",\"data\":{\"access_token\":\"71e4c1c24093418db9b50aa491560a6e\",\"data\":\"03214c5690b2f923461aa3c30b64d3fb6434\"}}";
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
            retJSONObject.put("addRetMsg","系统异常: "+e.toString());
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
     * 推送文件给IM群
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
                            String sendResult = sendImFile(multipartFile,"group",groupId,fileType);
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
            retJSONObject.put("addRetMsg","系统异常: "+e.toString());
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
     * 给IM用户推送消息
     * @param msg
     * @param target
     * @return
     */
    public JSONObject sendImUserMsg(String msg,String target){
        JSONObject retJsonObject = new JSONObject();
        try {

            String sendMsgResult = sendImMsg(msg,"users",target);
            if(StringUtil.isNotEmpty(sendMsgResult)){
                JSONObject sendMsgJson = JSONObject.parseObject(sendMsgResult);
                String sendMsgStatus = sendMsgJson.getString("status");
                //推送成功
                if(StringUtil.isNotEmpty(sendMsgStatus) && "success".equals(sendMsgStatus)){
                    retJsonObject.put("retCode","0");
                    retJsonObject.put("retMsg","给IM用户推送消息成功");
                }else{
                    retJsonObject.put("retCode","-1");
                    retJsonObject.put("retMsg","给IM用户推送消息失败："+sendMsgResult);
                }
            }else{
                retJsonObject.put("retCode","-1");
                retJsonObject.put("retMsg","给IM用户推送消息异常：接口返回空");
            }

        }catch (Exception e){
            e.printStackTrace();
            retJsonObject.put("retCode","-1");
            retJsonObject.put("retMsg","给IM用户推送消息异常："+e.toString());
        }

        return retJsonObject;
    }


    /**
     * 给IM群推送消息
     * @param msg
     * @param target
     * @return
     */
    public JSONObject sendImGroupMsg(String msg,String target){
        JSONObject retJsonObject = new JSONObject();
        try {

            List<String> grouplist = Arrays.stream(target.split(";"))
                    .map(String::toString)
                    .collect(Collectors.toList());

            if(grouplist != null && grouplist.size() >0){
                for(String groupId:grouplist){
                    String sendMsgResult = sendImMsg(msg,"group",groupId);
                    if(StringUtil.isNotEmpty(sendMsgResult)){
                        JSONObject sendMsgJson = JSONObject.parseObject(sendMsgResult);
                        String sendMsgStatus = sendMsgJson.getString("status");
                        //推送成功
                        if(StringUtil.isNotEmpty(sendMsgStatus) && "success".equals(sendMsgStatus)){
                            retJsonObject.put("retCode","0");
                            retJsonObject.put("retMsg","给IM群推送消息成功");
                        }else{
                            retJsonObject.put("retCode","-1");
                            retJsonObject.put("retMsg","给IM群推送消息失败："+sendMsgResult);
                            break;
                        }
                    }else{
                        retJsonObject.put("retCode","-1");
                        retJsonObject.put("retMsg","给IM群推送消息异常：接口返回空");
                        break;
                    }
                }
            }

        }catch (Exception e){
            e.printStackTrace();
            retJsonObject.put("retCode","-1");
            retJsonObject.put("retMsg","给IM群推送消息异常："+e.toString());
        }

        return retJsonObject;
    }


}
