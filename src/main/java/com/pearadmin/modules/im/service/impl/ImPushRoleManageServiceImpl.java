package com.pearadmin.modules.im.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.im.domain.ImPushRoleManage;
import com.pearadmin.modules.im.domain.ImPushRolePersonManage;
import com.pearadmin.modules.im.domain.ImPushRolePersonManageImp;
import com.pearadmin.modules.im.mapper.ImPushRoleManageMapper;
import com.pearadmin.modules.im.mapper.ImPushRolePersonManageMapper;
import com.pearadmin.modules.im.service.IImPushRoleManageService;
import com.pearadmin.modules.wgppt.domain.WgDtreeData;
import com.pearadmin.modules.wgppt.domain.WgpptPushObjectManage;
import com.pearadmin.modules.wgppt.domain.WgpptPushObjectManageImp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * 创建日期：2026-04-03
 * 推送角色管理
 **/

@Slf4j
@Service
public class ImPushRoleManageServiceImpl implements IImPushRoleManageService {

    @Value("${im-push-role-import-model-path}")
    private String imPushRoleImportModelPath;

    @Resource
    private ImPushRoleManageMapper imPushRoleManageMapper;

    @Resource
    private ImPushRolePersonManageMapper imPushRolePersonManageMapper;

    /**
     * Describe: 获取角色树
     * @return
     */
    @Override
    public List<ImPushRoleManage> getRoleTree() {
        List<ImPushRoleManage> list = new ArrayList<>();
        try {
            ImPushRoleManage rootImPushRoleManage = new ImPushRoleManage();
            rootImPushRoleManage.setRoleId("1");
            rootImPushRoleManage.setRoleName("推送角色");
            rootImPushRoleManage.setParentId("0");
            rootImPushRoleManage.setRoleLevel("js");

            JSONObject rootDataJson = new JSONObject();
            rootDataJson.put("roleLevel","js");
            rootImPushRoleManage.setBasicData(rootDataJson);
            list.add(rootImPushRoleManage);

            QueryWrapper<ImPushRoleManage> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByDesc("create_time");

            List<ImPushRoleManage> queryList = imPushRoleManageMapper.selectList(queryWrapper);
            if(queryList != null && queryList.size() >0){
                JSONObject dataJson = new JSONObject();
                for(ImPushRoleManage imPushRoleManage: queryList){
                    imPushRoleManage.setParentId("1");
                    dataJson.put("roleLevel",imPushRoleManage.getRoleLevel());
                    imPushRoleManage.setBasicData(dataJson);

                    list.add(imPushRoleManage);
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 保存角色信息
     * @param imPushRoleManage
     * @return
     */
    @Override
    public JSONObject saveRole(ImPushRoleManage imPushRoleManage) {
        JSONObject retJson = new JSONObject();
        try {
            QueryWrapper<ImPushRoleManage> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("role_code",imPushRoleManage.getRoleCode());
            queryWrapper.last("limit 1");
            ImPushRoleManage queryImPushRoleManage= imPushRoleManageMapper.selectOne(queryWrapper);
            if(queryImPushRoleManage != null){
                retJson.put("retCode","-1");
                retJson.put("retMsg","角色编码已存在");
                return retJson;
            }

            imPushRoleManage.setRoleId(IdUtil.simpleUUID());
            int addResult = imPushRoleManageMapper.insert(imPushRoleManage);
            if(addResult >0){
                retJson.put("retCode","0");
                retJson.put("retMsg","新增成功");
            }else{
                retJson.put("retCode","0");
                retJson.put("retMsg","新增失败");
            }

        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","0");
            retJson.put("retMsg","新增失败:"+e.getMessage());
        }
        return retJson;
    }


    /**
     * 角色树节点删除
     * @param roleId
     * @return
     */
    @Override
    public Object roleDel(String roleId) {
        JSONObject retJson = new JSONObject();
        try {
              if(roleId.equals("1")){
                  retJson.put("retCode","-1");
                  retJson.put("retMsg","不能删除要节点");
                  return retJson;
              }

              int delResult = imPushRoleManageMapper.deleteById(roleId);
              if(delResult >0){
                  retJson.put("retCode","0");
                  retJson.put("retMsg","删除成功");
              }else{
                  retJson.put("retCode","-1");
                  retJson.put("retMsg","删除失败");
              }
        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","-1");
            retJson.put("retMsg","删除失败："+e.getMessage());
        }
        return retJson;
    }


    /**
     * 根据主键ID获取相关信息
     * @param roleId
     * @return
     */
    @Override
    public ImPushRoleManage getImPushRoleManageById(String roleId) {
       return imPushRoleManageMapper.selectById(roleId);
    }


    /**
     * 保存编辑角色信息
     * @param imPushRoleManage
     * @return
     */
    @Override
    public JSONObject saveRoleEdit(ImPushRoleManage imPushRoleManage) {
        JSONObject retJson = new JSONObject();
        try {

            QueryWrapper<ImPushRoleManage> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("role_code",imPushRoleManage.getRoleCode());
            queryWrapper.ne("role_id",imPushRoleManage.getRoleId());
            queryWrapper.last("limit 1");

            ImPushRoleManage queryImPushRoleManage= imPushRoleManageMapper.selectOne(queryWrapper);
            if(queryImPushRoleManage != null){
                retJson.put("retCode","-1");
                retJson.put("retMsg","角色编码已存在");
                return retJson;
            }

            int editResult = imPushRoleManageMapper.updateById(imPushRoleManage);
            if(editResult >0){
                retJson.put("retCode","0");
                retJson.put("retMsg","更新成功");
            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","更新失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","-1");
            retJson.put("retMsg","更新失败:"+e.getMessage());
        }
        return retJson;
    }


    /**
     * 获取推送角色对应的人员信息表
     * @param imPushRolePersonManage
     * @return
     */
    @Override
    public List<ImPushRolePersonManage> getRolePersonTableList(ImPushRolePersonManage imPushRolePersonManage) {
        List<ImPushRolePersonManage> retList = new ArrayList<>();
        try {

            retList = imPushRolePersonManageMapper.getListByRoleId(imPushRolePersonManage);

        }catch (Exception e){
            e.printStackTrace();
        }
        return retList;
    }


    /**
     * 保存角色人员信息
     * @param imPushRolePersonManage
     * @return
     */
    @Override
    public JSONObject personSave(ImPushRolePersonManage imPushRolePersonManage) {
        JSONObject retJson = new JSONObject();
        try {
            imPushRolePersonManage.setManageId(IdUtil.simpleUUID());
            imPushRolePersonManage.setCreateTime(LocalDateTime.now());
            int addResult = imPushRolePersonManageMapper.insert(imPushRolePersonManage);
            if(addResult >0){
                retJson.put("retCode","0");
                retJson.put("retMsg","新增成功");
            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","新增失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","-1");
            retJson.put("retMsg","新增失败: "+e.getMessage());
        }
        return retJson;
    }

    /**
     * 编辑保存角色人员信息
     * @param imPushRolePersonManage
     * @return
     */
    @Override
    public JSONObject personEditSave(ImPushRolePersonManage imPushRolePersonManage) {
        JSONObject retJson = new JSONObject();
        try {
            imPushRolePersonManage.setCreateTime(LocalDateTime.now());
            int addResult = imPushRolePersonManageMapper.updateById(imPushRolePersonManage);
            if(addResult >0){
                retJson.put("retCode","0");
                retJson.put("retMsg","修改成功");
            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","修改失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","-1");
            retJson.put("retMsg","修改失败: "+e.getMessage());
        }
        return retJson;
    }


    /**
     * 角色下的推送对象单个删除
     * @param manageId
     * @return
     */
    @Override
    public boolean remove(String manageId) {
        int delResult = imPushRolePersonManageMapper.deleteById(manageId);
        return delResult >0?true:false;
    }

    /**
     * 根据ID获取相关信息
     * @param manageId
     * @return
     */
    @Override
    public ImPushRolePersonManage getById(String manageId) {
        ImPushRolePersonManage imPushRolePersonManage = imPushRolePersonManageMapper.selectById(manageId);
        if(imPushRolePersonManage != null){
            ImPushRoleManage imPushRoleManage = imPushRoleManageMapper.selectById(imPushRolePersonManage.getRoleId());
            if(imPushRoleManage != null){
                imPushRolePersonManage.setRoleName(imPushRoleManage.getRoleName());
                imPushRolePersonManage.setRoleLevel(imPushRoleManage.getRoleLevel());
            }
        }
        return imPushRolePersonManage;
    }

    /**
     * 角色推送对象批量导入模板下载
     * @return
     */
    @Override
    public ResponseEntity<InputStreamResource> downloadModelFile() {
        String isSuccess = "0";
        String errMsg ="";
        try {

            String filePath = imPushRoleImportModelPath;

            FileSystemResource file = new FileSystemResource(filePath);

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
                isSuccess = "-1";
                if(ex.getMessage() != null){
                    errMsg = ex.getMessage().substring(0,1000);
                }
                return null;
            }

        }catch (Exception e){
            e.printStackTrace();
            isSuccess = "-1";
            if(e.getMessage() != null){
                errMsg = e.getMessage().substring(0,1000);
            }
            return null;
        }
    }


    /**
     * 角色推送对象批量导入数据
     * @param file
     * @return
     */
    @Override
    public boolean batchImportData(MultipartFile file,String roleId) {
        boolean retResult = false;
        try {
            List<ImPushRolePersonManageImp> dataList = new ArrayList<>();

            // 使用EasyExcel读取
            EasyExcel.read(file.getInputStream(), ImPushRolePersonManageImp.class,
                    new ReadListener<ImPushRolePersonManageImp>() {
                        @Override
                        public void invoke(ImPushRolePersonManageImp data, AnalysisContext context) {
                            // 每读取一行调用一次
                            dataList.add(data);
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext context) {
                            // 读取完成
                            log.info("读取完成，共" + dataList.size() + "条数据");
                        }
                    }).sheet().doRead();


            if(dataList != null && dataList.size() >0){
                //对象转换
                List<ImPushRolePersonManage> imPushRolePersonManageList = objectConvert(dataList,roleId);
                if(imPushRolePersonManageList != null && imPushRolePersonManageList.size() >0){
                    // 分批处理，避免单次处理数据量过大
                    List<List<ImPushRolePersonManage>> batches = partitionList(imPushRolePersonManageList,200);
                    for (List<ImPushRolePersonManage> batch : batches){
                        //批量保存
                        int addCount = imPushRolePersonManageMapper.batchInsertWithId(batch);

                        retResult = true;
                    }
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return retResult;
    }

    /**
     * 对象转换
     * @param imPushRolePersonManageImpList
     * @return
     */
    private List<ImPushRolePersonManage> objectConvert(List<ImPushRolePersonManageImp> imPushRolePersonManageImpList,String roleId){
        List<ImPushRolePersonManage> imPushRolePersonManageList = new ArrayList<>();

        if(imPushRolePersonManageImpList != null && imPushRolePersonManageImpList.size() >0){
            QueryWrapper<ImPushRolePersonManage> imPushRoleQuery = new QueryWrapper<>();

            for(ImPushRolePersonManageImp imPushRolePersonManageImp:imPushRolePersonManageImpList){
                //String pushObjectName = wgpptPushObjectManageImp.getPushObjectName();
                //String pushObjectLeaderName = wgpptPushObjectManageImp.getPushObjectLeaderName();

                ImPushRolePersonManage imPushRolePersonManage = new ImPushRolePersonManage();
                imPushRolePersonManage.setManageId(IdUtil.simpleUUID());
                imPushRolePersonManage.setRoleId(roleId);
                imPushRolePersonManage.setPersonId(imPushRolePersonManageImp.getPersonId());
                imPushRolePersonManage.setPersonName(imPushRolePersonManageImp.getPersonName());
                imPushRolePersonManage.setHxLatnId(imPushRolePersonManageImp.getHxLatnId());
                imPushRolePersonManage.setHxLatnName(imPushRolePersonManageImp.getHxLatnName());
                imPushRolePersonManage.setHxAreaId(imPushRolePersonManageImp.getHxAreaId());
                imPushRolePersonManage.setHxAreaName(imPushRolePersonManageImp.getHxAreaName());
                imPushRolePersonManage.setHxRegionId(imPushRolePersonManageImp.getHxRegionId());
                imPushRolePersonManage.setHxRegionName(imPushRolePersonManageImp.getHxRegionName());
                imPushRolePersonManage.setXHx5BpId(imPushRolePersonManageImp.getXHx5BpId());
                imPushRolePersonManage.setXHx5BpName(imPushRolePersonManageImp.getXHx5BpName());
                imPushRolePersonManage.setCreateTime(LocalDateTime.now());

                //去重
                imPushRoleQuery.clear();
                imPushRoleQuery.eq("role_id",roleId);
                imPushRoleQuery.eq("person_id",imPushRolePersonManage.getPersonId());
                imPushRoleQuery.eq("person_name",imPushRolePersonManage.getPersonName());

                if(StringUtil.isNotEmpty(imPushRolePersonManageImp.getHxLatnId())){
                    imPushRoleQuery.eq("hx_latn_id",imPushRolePersonManage.getHxLatnId());
                }

                if(StringUtil.isNotEmpty(imPushRolePersonManageImp.getHxLatnName())){
                    imPushRoleQuery.eq("hx_latn_name",imPushRolePersonManage.getHxLatnName());
                }

                if(StringUtil.isNotEmpty(imPushRolePersonManageImp.getHxAreaId())){
                    imPushRoleQuery.eq("hx_area_id",imPushRolePersonManage.getHxAreaId());
                }

                if(StringUtil.isNotEmpty(imPushRolePersonManageImp.getHxAreaName())){
                    imPushRoleQuery.eq("hx_area_name",imPushRolePersonManage.getHxAreaName());
                }

                if(StringUtil.isNotEmpty(imPushRolePersonManageImp.getHxRegionId())){
                    imPushRoleQuery.eq("hx_region_id",imPushRolePersonManage.getHxRegionId());
                }

                if(StringUtil.isNotEmpty(imPushRolePersonManageImp.getHxRegionName())){
                    imPushRoleQuery.eq("hx_region_name",imPushRolePersonManage.getHxRegionName());
                }

                if(StringUtil.isNotEmpty(imPushRolePersonManageImp.getXHx5BpId())){
                    imPushRoleQuery.eq("x_hx5_bp_id",imPushRolePersonManage.getXHx5BpId());
                }

                if(StringUtil.isNotEmpty(imPushRolePersonManageImp.getXHx5BpName())){
                    imPushRoleQuery.eq("x_hx5_bp_name",imPushRolePersonManage.getXHx5BpName());
                }

                imPushRoleQuery.last("limit 1");

                ImPushRolePersonManage queryImPushRolePersonManage = imPushRolePersonManageMapper.selectOne(imPushRoleQuery);
                if(queryImPushRolePersonManage != null){
                    continue;
                }


                imPushRolePersonManageList.add(imPushRolePersonManage);
            }

            //去掉重复的XHxBpId
            /*imPushRolePersonManageList = imPushRolePersonManageList.stream()
                    .filter(distinctByKey(ImPushRolePersonManage::getXHxBpId))
                    .collect(Collectors.toList());*/
        }
        return  imPushRolePersonManageList;
    }

    /**
     * 将列表分割成多个批次
     */
    private <T> List<List<T>> partitionList(List<T> originalList, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();

        for (int i = 0; i < originalList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, originalList.size());
            partitions.add(new ArrayList<>(originalList.subList(i, end)));
        }

        return partitions;
    }


}
