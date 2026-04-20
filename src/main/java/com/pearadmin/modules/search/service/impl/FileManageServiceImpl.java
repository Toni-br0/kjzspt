/*
package com.pearadmin.modules.search.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.context.UserContext;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.search.config.FileRepository;
import com.pearadmin.modules.search.domain.FileInfo;
import com.pearadmin.modules.search.domain.SearchFileClassify;
import com.pearadmin.modules.search.domain.SearchFileManage;
import com.pearadmin.modules.search.mapper.SearchFileClassifyMapper;
import com.pearadmin.modules.search.mapper.SearchFileManageMapper;
import com.pearadmin.modules.search.service.IFileManageService;
import com.pearadmin.modules.search.util.FileUtils;
import com.pearadmin.modules.sys.domain.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

*/
/**
 * 创建日期：2024-12-06
 * 智能搜索文件管理
 **//*



@Slf4j
@Service
@RequiredArgsConstructor
public class FileManageServiceImpl implements IFileManageService {

    @Value("${search-upload-path}")
    private String searchUploadPath;

    private final SearchFileManageMapper searchFileManageMapper;

    private final FileRepository fileRepository;

    private final Tesseract tesseract;

    private final SearchFileClassifyMapper searchFileClassifyMapper;



*/
/**
     * 获取智能搜索文件管理列表数据
     * @param searchFileManage
     * @return*//*



    @Override
    public List<SearchFileManage> fileDataList(SearchFileManage searchFileManage) {
        QueryWrapper<SearchFileManage>  queryWrapper = new QueryWrapper<>();
        if(StringUtil.isNotEmpty(searchFileManage.getFileName())){
            queryWrapper.like("file_name",searchFileManage.getFileName());
        }

        if(StringUtil.isNotEmpty(searchFileManage.getCreateUserName())){
            queryWrapper.like("create_user_name",searchFileManage.getCreateUserName());
        }

        queryWrapper.orderByDesc("create_time");
        List<SearchFileManage> list = searchFileManageMapper.selectList(queryWrapper);
        return list;
    }

*/
/**
     * 智能搜索文件管理文件上传
     * @param file
     * @return*//*



    @Override
    public JSONObject uploadFile(MultipartFile file,String fileClassifyCode,String fileClassifyName) {
        JSONObject returnJson = new JSONObject();
        returnJson.put("retCode", "200");
        returnJson.put("retMsg", "文件上传成功");

        String fileName = "";
        String filePath = "";
        String fileSize = "";
        String fileType = "";
        String createUserId = "";
        String createUserName = "";

        try {

            //当前登录人信息
            SysUser currentUser = UserContext.currentUser();
            createUserId = currentUser.getUserId();
            createUserName = currentUser.getRealName();
            // 获取文件名
            fileName = file.getOriginalFilename();
            // 获取文件的后缀名
            fileType = fileName.substring(fileName.lastIndexOf(".")+1);
            // 获取文件大小
            double dFileSize = file.getSize();
            BigDecimal value = new BigDecimal(dFileSize);
            BigDecimal divisor = new BigDecimal(1024);
            // 除法运算，并设置保留两位小数，以及舍入模式
            fileSize = value.divide(divisor, 1, RoundingMode.HALF_UP).toString()+"KB";

            // 文件存储路径
            //filePath = searchUploadPath+fileClassifyCode+"/" + fileName;
            filePath = searchUploadPath + fileName;

            //获取服务器操作系统类型，用于判断文件路径分隔符
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("windows")) {
                //filePath = searchUploadPath+fileClassifyCode+"\\" + fileName;
                filePath = searchUploadPath + fileName;
            }

            // 保存文件信息到数据库
            QueryWrapper<SearchFileManage> fileQu = new QueryWrapper<>();
            fileQu.eq("file_name", fileName);
            fileQu.last("limit 1");
            SearchFileManage searchFileManage = searchFileManageMapper.selectOne(fileQu);
            if(searchFileManage != null){
                //更新
                searchFileManage.setFilePath(filePath);
                searchFileManage.setFileSize(fileSize);
                searchFileManage.setFileType(fileType);
                searchFileManage.setCreateTime(LocalDateTime.now());
                searchFileManage.setCreateUserId(createUserId);
                searchFileManage.setCreateUserName(createUserName);
                searchFileManage.setFileClassifyCode(fileClassifyCode);
                searchFileManage.setFileClassifyName(fileClassifyName);

                //根据ID删除ES上的文件
                fileRepository.deleteById(searchFileManage.getFileId());
                //保存文件到ES
                boolean saveResult = saveEsFile(file,searchFileManage);
                if(saveResult){
                   int upCount = searchFileManageMapper.updateById(searchFileManage);
                   if(upCount ==0){
                       returnJson.put("retCode", "204");
                       returnJson.put("retMsg", "更新文件信息到数据库失败");
                   }else{
                       // 将文件保存到指定目录
                       File dest = new File(filePath);

                       // 检测是否存在目录
                       if (!dest.getParentFile().exists()) {
                           dest.getParentFile().mkdirs();
                       }

                       file.transferTo(dest);
                   }
                }else{
                    returnJson.put("retCode", "202");
                    returnJson.put("retMsg", "文件上传至ES失败");
                }

            }else{
                //新增
                searchFileManage = new SearchFileManage();
                searchFileManage.setFileId(IdUtil.getSnowflake(1, 1).nextIdStr());
                searchFileManage.setFileName(fileName);
                searchFileManage.setFilePath(filePath);
                searchFileManage.setFileSize(fileSize);
                searchFileManage.setFileType(fileType);
                searchFileManage.setCreateTime(LocalDateTime.now());
                searchFileManage.setCreateUserId(createUserId);
                searchFileManage.setCreateUserName(createUserName);
                searchFileManage.setFileClassifyCode(fileClassifyCode);
                searchFileManage.setFileClassifyName(fileClassifyName);

                //保存文件到ES
                boolean saveResult = saveEsFile(file,searchFileManage);
                if(saveResult){
                    //保存文件信息到数据库
                   int addCount = searchFileManageMapper.insert(searchFileManage);
                   if(addCount == 0){
                       returnJson.put("retCode", "203");
                       returnJson.put("retMsg", "新增文件信息到数据库失败");
                   }else{
                       // 将文件保存到指定目录
                       File dest = new File(filePath);

                       // 检测是否存在目录
                       if (!dest.getParentFile().exists()) {
                           dest.getParentFile().mkdirs();
                       }

                       file.transferTo(dest);
                   }
                }else{
                    returnJson.put("retCode", "201");
                    returnJson.put("retMsg", "文件上传至ES失败");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            returnJson.put("retCode", "500");
            returnJson.put("retMsg", e.getMessage());
        }finally {

        }
        return returnJson;
    }


*/
/**
     * 保存文件到ES*//*



    public boolean saveEsFile(MultipartFile multFile,SearchFileManage searchFileManage) throws Exception {
        boolean isResult = true;
        if (Objects.isNull(multFile)) {
            isResult = false;
            throw new IOException("文件不存在");
        }

        InputStream inputStream = multFile.getInputStream();

        try {

            // 读取文件内容，上传到es，方便后续的检索
            String fileContent = FileUtils.readFileContent(inputStream, searchFileManage.getFileType(), tesseract);
            log.info("++++++++++++++文件内容: {}",fileContent);
            if(fileContent == null || fileContent.trim().length() ==0){
                return false;
            }
            FileInfo file = new FileInfo();
            file.setFileId(searchFileManage.getFileId());
            file.setFileContent(fileContent);
            file.setFileName(searchFileManage.getFileName());
            file.setFilePath(searchFileManage.getFilePath());
            file.setFileType(searchFileManage.getFileType());
            file.setFileCategory(searchFileManage.getFileType());
            file.setCreateBy(searchFileManage.getCreateUserName());
            file.setCreateTime(new Date());
            file.setFileClassifyName(searchFileManage.getFileClassifyName());
            file.setFileSize(searchFileManage.getFileSize());
            fileRepository.save(file);

        }catch (Exception e) {
            e.printStackTrace();
            isResult = false;
            throw e;
        }finally {
            inputStream.close();
        }
        return isResult;
    }

*/
/**
     *  智能搜索文件管理文件删除
     * @param fileId
     * @return*//*



    @Override
    public boolean fileRemove(String fileId) {
        boolean retResult = false;
        try {
            //根据ID删除ES上的文件
            fileRepository.deleteById(fileId);
            //删除数据库上的文件信息
            int delCount = searchFileManageMapper.deleteById(fileId);
            if(delCount > 0){
                retResult = true;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return retResult;
    }

*/
/**
     * 文件下载
     * @param fileId
     * @return
     * @throws Exception*//*



    @Override
    public ResponseEntity<InputStreamResource> downloadFile(String fileId) throws Exception {
        log.info("进入下载方法... fileId: {}",fileId);
        String fileName = "";
        String filePath = "";
        SearchFileManage searchFileManage = searchFileManageMapper.selectById(fileId);
        if(searchFileManage != null){
            fileName = searchFileManage.getFileName();
            filePath = searchFileManage.getFilePath();
        }
        //读取文件
        //String filePath = searchUploadPath + fileName;


        FileSystemResource file = new FileSystemResource(filePath);

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
                .body(new InputStreamResource(file.getInputStream()));
    }

*/
/**
     * 获取文件分类树
     * @return*//*



    @Override
    public Object treeload() {
        JSONObject retArr = new JSONObject();
        QueryWrapper<SearchFileClassify> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", "0");
        List<SearchFileClassify> list = searchFileClassifyMapper.selectList(queryWrapper);
        if(StringUtil.isNotEmpty(list)){
            String title = "";
            String value ="";
            String classifyId = "";

            for(SearchFileClassify searchFileClassify:list){
                title = searchFileClassify.getClassifyName();
                value = searchFileClassify.getClassifyCode();
                classifyId = searchFileClassify.getClassifyId();

                retArr.put("title",title);
                retArr.put("id",value);
                retArr.put("spread",true);
                retArr.put("children",getChildTreeNode(classifyId));

            }
        }

        String jsonStr = "["+retArr.toJSONString()+"]";
        return jsonStr;
    }

    private JSONArray getChildTreeNode(String classifyId){
        JSONArray retArr = new JSONArray();
        QueryWrapper<SearchFileClassify> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", classifyId);
        List<SearchFileClassify> list = searchFileClassifyMapper.selectList(queryWrapper);
        if(list != null && list.size() >0){
            String title = "";
            String value ="";
            String subClassifyId = "";

            for(SearchFileClassify searchFileClassify:list){
                JSONObject subJson = new JSONObject();
                title = searchFileClassify.getClassifyName();
                value = searchFileClassify.getClassifyCode();
                subClassifyId = searchFileClassify.getClassifyId();

                subJson.put("title",title);
                subJson.put("id",value);
                subJson.put("spread",true);
                subJson.put("children",getChildTreeNode(subClassifyId)) ;
                retArr.add(subJson);
            }
        }

        return retArr;

    }


}
*/
