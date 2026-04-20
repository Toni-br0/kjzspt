package com.pearadmin.modules.knowBase.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.context.UserContext;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.knowBase.domain.KnowbaseDraftInfo;
import com.pearadmin.modules.knowBase.domain.KnowbaseKnowInfo;
import com.pearadmin.modules.knowBase.domain.KnowbaseUndercarrInfo;
import com.pearadmin.modules.knowBase.mapper.KnowbaseDraftInfoMapper;
import com.pearadmin.modules.knowBase.mapper.KnowbaseKnowInfoMapper;
import com.pearadmin.modules.knowBase.mapper.KnowbaseUndercarrInfoMapper;
import com.pearadmin.modules.knowBase.service.KnowManageService;
import com.pearadmin.modules.sys.domain.SysUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建日期：2025-06-23
 * 知识管理
 **/

@Slf4j
@Service
public class KnowManageServiceImpl implements KnowManageService {

    @Value("${know-attachment-path}")
    private String knowAttachmentPath;

    @Value("${know-cover-path}")
    private String knowCoverPath;

    @Value("${know-video-path}")
    private String knowVideoPath;


    @Resource
    private KnowbaseKnowInfoMapper knowbaseKnowInfoMapper;

    @Resource
    private KnowbaseDraftInfoMapper knowbaseDraftInfoMapper;

    @Resource
    private KnowbaseUndercarrInfoMapper knowbaseUndercarrInfoMapper;

    /**
     * 发布知识
     * @param files
     * @param images
     * @param videos
     * @param knowInfo
     * @return
     */
    @Override
    public JSONObject publishKnow(MultipartFile[] files, MultipartFile[] images, MultipartFile[] videos, KnowbaseKnowInfo knowInfo){
        JSONObject retJson = new JSONObject();
        try {
            if(files != null && files.length > 0){
                String filePath = knowAttachmentPath;
                saveFile(files,filePath,"file",knowInfo);
            }

            if(images != null && images.length > 0){
                String filePath = knowCoverPath;
                saveFile(images,filePath,"image",knowInfo);
            }

            if(videos != null && videos.length > 0){
                String filePath = knowVideoPath;
                saveFile(videos,filePath,"video",knowInfo);
            }

            //当前登录人信息
            SysUser currentUser = UserContext.currentUser();


            knowInfo.setCreateTime(LocalDateTime.now());
            knowInfo.setCreateUserId(currentUser.getUserId());
            knowInfo.setCreateUserName(currentUser.getRealName());

            int addResult = knowbaseKnowInfoMapper.insert(knowInfo);
            if(addResult >0){
                retJson.put("retCode","0");
                retJson.put("retMsg","发布成功");
            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","发布失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
            retJson.put("retCode","500");
            retJson.put("retMsg","发布失败，系统异常！");
        }

        return retJson;
    }

    /**
     * 保存草稿
     * @param files
     * @param images
     * @param videos
     * @param knowbaseDraftInfo
     * @return
     */
    @Override
    public JSONObject saveDraft(MultipartFile[] files, MultipartFile[] images, MultipartFile[] videos, KnowbaseDraftInfo knowbaseDraftInfo){
        JSONObject retJson = new JSONObject();
        try {
            if(files != null && files.length > 0){
                String filePath = knowAttachmentPath;
                saveDraftFile(files,filePath,"file",knowbaseDraftInfo);
            }

            if(images != null && images.length > 0){
                String filePath = knowCoverPath;
                saveDraftFile(images,filePath,"image",knowbaseDraftInfo);
            }

            if(videos != null && videos.length > 0){
                String filePath = knowVideoPath;
                saveDraftFile(videos,filePath,"video",knowbaseDraftInfo);
            }

            //当前登录人信息
            SysUser currentUser = UserContext.currentUser();


            knowbaseDraftInfo.setCreateTime(LocalDateTime.now());
            knowbaseDraftInfo.setCreateUserId(currentUser.getUserId());
            knowbaseDraftInfo.setCreateUserName(currentUser.getRealName());

            int addResult = knowbaseDraftInfoMapper.insert(knowbaseDraftInfo);
            if(addResult >0){
                retJson.put("retCode","0");
                retJson.put("retMsg","保存草稿成功");
            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","保存草稿失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
            retJson.put("retCode","500");
            retJson.put("retMsg","保存草稿失败，系统异常！");
        }

        return retJson;
    }


    /**
     * 保存文件
     * @param files
     * @param filePath
     * @param type
     * @param knowInfo
     * @throws Exception
     */
    private void saveFile(MultipartFile[] files, String filePath, String type, KnowbaseKnowInfo knowInfo) throws Exception{
        try {
            String fileAllPath ="";
            for(MultipartFile file : files){

                Path uploadPath = Paths.get(filePath);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                // 使用原始文件名（会覆盖同名文件）
                String fileName = file.getOriginalFilename();
                Path addFilePath = uploadPath.resolve(fileName);

                // 保存文件（使用REPLACE_EXISTING选项覆盖）
                Files.write(addFilePath, file.getBytes(), StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

                if(fileAllPath.equals("")){
                    fileAllPath = filePath+fileName;
                }else{
                    fileAllPath = fileAllPath+";"+filePath+fileName;
                }

            }

            if(type.equals("file")){
                knowInfo.setFjUrl(fileAllPath);
            }else if(type.equals("image")){
                knowInfo.setFmUrl(fileAllPath);
            }else{
                knowInfo.setSpUrl(fileAllPath);
            }

        }catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }


    /**
     * 保存文件
     * @param files
     * @param filePath
     * @param type
     * @param knowbaseDraftInfo
     * @throws Exception
     */
    private void saveDraftFile(MultipartFile[] files, String filePath, String type, KnowbaseDraftInfo knowbaseDraftInfo) throws Exception{
        try {
            String fileAllPath ="";
            for(MultipartFile file : files){

                Path uploadPath = Paths.get(filePath);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                // 使用原始文件名（会覆盖同名文件）
                String fileName = file.getOriginalFilename();
                Path addFilePath = uploadPath.resolve(fileName);

                // 保存文件（使用REPLACE_EXISTING选项覆盖）
                Files.write(addFilePath, file.getBytes(), StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

                if(fileAllPath.equals("")){
                    fileAllPath = filePath+fileName;
                }else{
                    fileAllPath = fileAllPath+";"+filePath+fileName;
                }

            }

            if(type.equals("file")){
                knowbaseDraftInfo.setFjUrl(fileAllPath);
            }else if(type.equals("image")){
                knowbaseDraftInfo.setFmUrl(fileAllPath);
            }else{
                knowbaseDraftInfo.setSpUrl(fileAllPath);
            }

        }catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }


    /**
     * 获取知识管理列表数据(已发布)
     * @param knowbaseKnowInfo
     * @return
     */
    @Override
    public List<KnowbaseKnowInfo> getKnowDataList(KnowbaseKnowInfo knowbaseKnowInfo){
        //当前登录人信息
        SysUser currentUser = UserContext.currentUser();
        knowbaseKnowInfo.setCreateUserId(currentUser.getUserId());

        List<KnowbaseKnowInfo> list = knowbaseKnowInfoMapper.selKnowInfoList(knowbaseKnowInfo);
        return list;
    }

    /**
     * 获取知识管理列表数据(已下架)
     * @param knowbaseKnowInfo
     * @return
     */
    public List<KnowbaseUndercarrInfo> getUndercarrDataList(KnowbaseKnowInfo knowbaseKnowInfo){
        //当前登录人信息
        SysUser currentUser = UserContext.currentUser();
        knowbaseKnowInfo.setCreateUserId(currentUser.getUserId());

        List<KnowbaseUndercarrInfo> list = knowbaseKnowInfoMapper.selUndercarrInfoList(knowbaseKnowInfo);
        return list;
    }

    /**
     * 获取知识管理列表数据(草稿箱)
     * @param knowbaseKnowInfo
     * @return
     */
    @Override
    public List<KnowbaseDraftInfo> getDraftDataList(KnowbaseKnowInfo knowbaseKnowInfo){
        //当前登录人信息
        SysUser currentUser = UserContext.currentUser();
        knowbaseKnowInfo.setCreateUserId(currentUser.getUserId());
        List<KnowbaseDraftInfo> list = knowbaseKnowInfoMapper.selDraftInfoList(knowbaseKnowInfo);
        return list;
    }


    /**
     * 知识下架
     */
    @Override
    @Transactional
    public JSONObject undercarr(int infoId){
        JSONObject retJson = new JSONObject();
        try {
               KnowbaseKnowInfo knowbaseKnowInfo = knowbaseKnowInfoMapper.selectById(infoId);
               if(knowbaseKnowInfo != null){
                   KnowbaseUndercarrInfo knowbaseUndercarrInfo = new KnowbaseUndercarrInfo();
                   BeanUtils.copyProperties(knowbaseKnowInfo,knowbaseUndercarrInfo);
                   knowbaseUndercarrInfo.setInfoId(null);

                   int addCount = knowbaseUndercarrInfoMapper.insert(knowbaseUndercarrInfo);
                   if(addCount >0){
                      int delCount = knowbaseKnowInfoMapper.deleteById(infoId);
                      if(delCount >0){
                          retJson.put("retCode","0");
                          retJson.put("retMsg","知识下架成功");
                      }else{
                          retJson.put("retCode","-1");
                          retJson.put("retMsg","知识下架失败");
                          // 手动回滚
                          TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                      }
                   }else{
                       retJson.put("retCode","-1");
                       retJson.put("retMsg","知识下架失败");
                       // 手动回滚
                       TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                   }

               }else{
                   retJson.put("retCode","-1");
                   retJson.put("retMsg","知识下架失败");
               }
        }catch (Exception e) {
            e.printStackTrace();
            retJson.put("retCode","500");
            retJson.put("retMsg","知识下架失败，系统异常！");
            // 手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
        return retJson;
    }

    /**
     * 知识上架
     */
    @Override
    @Transactional
    public JSONObject uppercarr(int infoId,String queryType){
        JSONObject retJson = new JSONObject();
        try {
            KnowbaseKnowInfo knowbaseKnowInfo = new KnowbaseKnowInfo();
            if(queryType.equals("yxj")){
                KnowbaseUndercarrInfo knowbaseUndercarrInfo = knowbaseUndercarrInfoMapper.selectById(infoId);
                if(knowbaseUndercarrInfo !=null){
                    BeanUtils.copyProperties(knowbaseUndercarrInfo,knowbaseKnowInfo);
                }
            }else if(queryType.equals("cgx")){
                KnowbaseDraftInfo knowbaseDraftInfo = knowbaseDraftInfoMapper.selectById(infoId);
                if(knowbaseDraftInfo !=null){
                    BeanUtils.copyProperties(knowbaseDraftInfo,knowbaseKnowInfo);
                }
            }

            if(knowbaseKnowInfo != null && StringUtil.isNotEmpty(knowbaseKnowInfo.getKnowTitle())){
                knowbaseKnowInfo.setInfoId(null);
                int addCount = knowbaseKnowInfoMapper.insert(knowbaseKnowInfo);
                if(addCount >0){
                    int delCount = 0;
                    if(queryType.equals("yxj")){
                        delCount = knowbaseUndercarrInfoMapper.deleteById(infoId);
                    }else if(queryType.equals("cgx")){
                        delCount = knowbaseDraftInfoMapper.deleteById(infoId);
                    }

                    if(delCount >0){
                        retJson.put("retCode","0");
                        retJson.put("retMsg","知识上架成功");
                    }else{
                        retJson.put("retCode","-1");
                        retJson.put("retMsg","知识上架失败");
                        // 手动回滚
                        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    }
                }else{
                    retJson.put("retCode","-1");
                    retJson.put("retMsg","知识上架失败");
                    // 手动回滚
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                }

            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","知识上架失败");
            }
        }catch (Exception e) {
            e.printStackTrace();
            retJson.put("retCode","500");
            retJson.put("retMsg","知识上架失败，系统异常！");
            // 手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
        return retJson;
    }

    /**
     * 单个删除草稿箱知识
     */
    @Override
    public JSONObject draftRemove(int infoId){
        JSONObject retJson = new JSONObject();
        int delCount = knowbaseDraftInfoMapper.deleteById(infoId);
        if(delCount >0){
            retJson.put("retCode","0");
            retJson.put("retMsg","删除成功");
        }else{
            retJson.put("retCode","-1");
            retJson.put("retMsg","删除失败");
        }
        return retJson;
    }


    /**
     * 根据知识ID获取知识信息
     * @param infoId
     * @return
     */
    @Override
    public Object toKnowEdit(int infoId,String queryType){
        Object retObj = null;
        if(queryType.equals("yxj")){
            retObj = knowbaseUndercarrInfoMapper.selectById(infoId);
        }else if(queryType.equals("cgx")){
            retObj = knowbaseDraftInfoMapper.selectById(infoId);
        }else {
            retObj = knowbaseKnowInfoMapper.selectById(infoId);
        }
        return  retObj;
    }


}
