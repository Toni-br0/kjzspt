package com.pearadmin.modules.knowBase.service;

import com.alibaba.fastjson.JSONObject;
import com.pearadmin.common.aop.annotation.Log;
import com.pearadmin.common.aop.enums.BusinessType;
import com.pearadmin.modules.knowBase.domain.KnowbaseDraftInfo;
import com.pearadmin.modules.knowBase.domain.KnowbaseKnowInfo;
import com.pearadmin.modules.knowBase.domain.KnowbaseUndercarrInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 创建日期：2025-06-23
 * 知识管理服务接口
 **/

public interface KnowManageService {

   /**
    * 发布知识
     * @param files
    * @param images
    * @param videos
    * @param knowInfo
    * @return
    */
   public JSONObject publishKnow(MultipartFile[] files, MultipartFile[] images, MultipartFile[] videos, KnowbaseKnowInfo knowInfo);

   /**
    * 保存草稿
    * @param files
    * @param images
    * @param videos
    * @param knowbaseDraftInfo
    * @return
    */
   public JSONObject saveDraft(MultipartFile[] files, MultipartFile[] images, MultipartFile[] videos, KnowbaseDraftInfo knowbaseDraftInfo);


   /**
    * 获取知识管理列表数据(已发布)
    * @param knowbaseKnowInfo
    * @return
    */
   public List<KnowbaseKnowInfo> getKnowDataList(KnowbaseKnowInfo knowbaseKnowInfo);


   /**
    * 获取知识管理列表数据(已下架)
    * @param knowbaseKnowInfo
    * @return
    */
   public List<KnowbaseUndercarrInfo> getUndercarrDataList(KnowbaseKnowInfo knowbaseKnowInfo);

   /**
    * 获取知识管理列表数据(草稿箱)
    * @param knowbaseKnowInfo
    * @return
    */
   public List<KnowbaseDraftInfo> getDraftDataList(KnowbaseKnowInfo knowbaseKnowInfo);

   /**
    * 知识下架
    */
   public JSONObject undercarr(int infoId);

   /**
    * 知识上架
    */
   public JSONObject uppercarr(int infoId,String queryType);

   /**
    * 单个删除草稿箱知识
    */
   public JSONObject draftRemove(int infoId);

   /**
    * 根据知识ID获取知识信息
    * @param infoId
    * @return
    */
   public Object toKnowEdit(int infoId,String queryType);

}
