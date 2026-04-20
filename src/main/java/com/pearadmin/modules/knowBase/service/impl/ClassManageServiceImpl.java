package com.pearadmin.modules.knowBase.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.constant.CommonConstant;
import com.pearadmin.common.context.UserContext;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.knowBase.domain.KnowbaseClassInfo;
import com.pearadmin.modules.knowBase.domain.KnowbaseDraftInfo;
import com.pearadmin.modules.knowBase.domain.KnowbaseKnowInfo;
import com.pearadmin.modules.knowBase.domain.KnowbaseUndercarrInfo;
import com.pearadmin.modules.knowBase.mapper.KnowbaseClassInfoMapper;
import com.pearadmin.modules.knowBase.mapper.KnowbaseDraftInfoMapper;
import com.pearadmin.modules.knowBase.mapper.KnowbaseKnowInfoMapper;
import com.pearadmin.modules.knowBase.mapper.KnowbaseUndercarrInfoMapper;
import com.pearadmin.modules.knowBase.service.ClassManageService;
import com.pearadmin.modules.sys.domain.SysUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 创建日期：2025-06-25
 **/

@Slf4j
@Service
public class ClassManageServiceImpl implements ClassManageService {

    @Resource
    private KnowbaseClassInfoMapper knowbaseClassInfoMapper;
    @Autowired
    private KnowbaseKnowInfoMapper knowbaseKnowInfoMapper;
    @Autowired
    private KnowbaseUndercarrInfoMapper knowbaseUndercarrInfoMapper;
    @Autowired
    private KnowbaseDraftInfoMapper knowbaseDraftInfoMapper;

    /*
     * 获取类别管理列表数据
     */
    @Override
    public List<KnowbaseClassInfo> getClassList(KnowbaseClassInfo knowbaseClassInfo){
        QueryWrapper<KnowbaseClassInfo> queryWrapper = new QueryWrapper<>();
        if(StringUtil.isNotEmpty(knowbaseClassInfo.getClassCode())){
            queryWrapper.like("class_code", knowbaseClassInfo.getClassCode());
        }
        if(StringUtil.isNotEmpty(knowbaseClassInfo.getClassName())){
            queryWrapper.like("class_name", knowbaseClassInfo.getClassName());
        }

        queryWrapper.orderByAsc("sort_id");

        List<KnowbaseClassInfo> list = knowbaseClassInfoMapper.selectList(queryWrapper);
        return list;
    }


    /**
     * 保存类别信息
     * @param knowbaseClassInfo
     * @return
     */
    @Override
    public JSONObject saveClassInfo(KnowbaseClassInfo knowbaseClassInfo){
        JSONObject retJson = new JSONObject();
        boolean isAdd = true;
        Integer classId = knowbaseClassInfo.getClassId();
        if(classId !=null && classId !=0){
            isAdd = false;
        }

        //判断类别编码是否存在
        QueryWrapper<KnowbaseClassInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_code",knowbaseClassInfo.getClassCode());

        KnowbaseClassInfo quClassInfo = knowbaseClassInfoMapper.selectOne(queryWrapper);
        if(quClassInfo != null){
            if(isAdd){
                retJson.put("retCode","-1");
                retJson.put("retMsg","新增失败，编码已存在");
                return retJson;
            }else{
                if(!quClassInfo.getClassId().equals(classId)){
                    retJson.put("retCode","-1");
                    retJson.put("retMsg","修改失败，编码已存在");
                    return retJson;
                }

            }
        }

        //当前登录人信息
        SysUser currentUser = UserContext.currentUser();

        knowbaseClassInfo.setCreateTime(LocalDateTime.now());
        knowbaseClassInfo.setCreateUserId(currentUser.getUserId());
        knowbaseClassInfo.setCreateUserName(currentUser.getRealName());

        int result = 0;
        if(isAdd){
            result = knowbaseClassInfoMapper.insert(knowbaseClassInfo);
            if(result > 0){
                retJson.put("retCode","0");
                retJson.put("retMsg","新增成功");
            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","新增失败");
            }

        }else{
            result = knowbaseClassInfoMapper.updateById(knowbaseClassInfo);
            if(result > 0){
                retJson.put("retCode","0");
                retJson.put("retMsg","修改成功");
            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","修改失败");
            }
        }

        return retJson;
        }


    /**
     * 单个删除类别数据
     * @param classId
     * @return
     */
    @Override
    public JSONObject remove(int classId){
        JSONObject retJson = new JSONObject();
        try {
            KnowbaseClassInfo knowbaseClassInfo = knowbaseClassInfoMapper.selectById(classId);
            if(knowbaseClassInfo != null){

                //查询此类别是否在已发布知识中被使用
                QueryWrapper<KnowbaseKnowInfo> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("know_class",knowbaseClassInfo.getClassCode());
                long count = knowbaseKnowInfoMapper.selectCount(queryWrapper);
                if(count > 0){
                    retJson.put("retCode","-1");
                    retJson.put("retMsg","删除失败，该类别在已发布的知识中被使用，无法删除");
                    return retJson;
                }

                //查询此类别是否在已下架知识中被使用
                QueryWrapper<KnowbaseUndercarrInfo> underWrapper = new QueryWrapper<>();
                underWrapper.eq("know_class",knowbaseClassInfo.getClassCode());
                long underCount = knowbaseUndercarrInfoMapper.selectCount(underWrapper);
                if(underCount > 0){
                    retJson.put("retCode","-1");
                    retJson.put("retMsg","删除失败，该类别在已下架的知识中被使用，无法删除");
                    return retJson;
                }

                //查询此类别是否在草稿箱中被使用
                QueryWrapper<KnowbaseDraftInfo> draftWrapper = new QueryWrapper<>();
                draftWrapper.eq("know_class",knowbaseClassInfo.getClassCode());
                long draftCount = knowbaseDraftInfoMapper.selectCount(draftWrapper);
                if(draftCount > 0){
                    retJson.put("retCode","-1");
                    retJson.put("retMsg","删除失败，该类别在草稿箱中被使用，无法删除");
                    return retJson;
                }

                int result = knowbaseClassInfoMapper.deleteById(classId);
                if(result > 0){
                    retJson.put("retCode","0");
                    retJson.put("retMsg","删除成功");
                }else{
                    retJson.put("retCode","-1");
                    retJson.put("retMsg","删除失败");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","-1");
            retJson.put("retMsg","删除失败");
        }

        return retJson;
    }

    /**
     * 批量删除类别数据
     * @param classIds
     * @return
     */
    @Override
    public JSONObject  batchRemove(String classIds){
        JSONObject retJson = new JSONObject();

        try {
            KnowbaseClassInfo knowbaseClassInfo = null;
            List<String> delKnowList = new ArrayList<>();

            List<Integer> delList = new ArrayList<>();
            for (String classId : classIds.split(CommonConstant.COMMA)) {
                int iClassId = Integer.parseInt(classId);
                delList.add(iClassId);

                knowbaseClassInfo = knowbaseClassInfoMapper.selectById(iClassId);
                if(knowbaseClassInfo != null){
                    delKnowList.add(knowbaseClassInfo.getClassCode());
                }
            }

            //查询此类别是否在已发布知识中被使用
            int knowClassCount = knowbaseKnowInfoMapper.selKnowInfoByClassCode(delKnowList);
            if(knowClassCount >0){
                retJson.put("retCode","-1");
                retJson.put("retMsg","删除失败，选中的类别在已发布知识中被使用，无法删除");
                return retJson;
            }

            //查询此类别是否在已下架知识中被使用
            int undercarrCount = knowbaseKnowInfoMapper.selUndercarrInfoByClassCode(delKnowList);
            if(undercarrCount >0){
                retJson.put("retCode","-1");
                retJson.put("retMsg","删除失败，选中的类别在已下架知识中被使用，无法删除");
                return retJson;
            }

            //查询此类别是否在草稿箱中被使用
            int draftInfoCount = knowbaseKnowInfoMapper.selDraftInfoByClassCode(delKnowList);
            if(draftInfoCount >0){
                retJson.put("retCode","-1");
                retJson.put("retMsg","删除失败，选中的类别在草稿箱中被使用，无法删除");
                return retJson;
            }


            int result = knowbaseClassInfoMapper.deleteBatchIds(delList);
            if(result > 0){
                retJson.put("retCode","0");
                retJson.put("retMsg","删除成功");
            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","删除失败");
            }

        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","-1");
            retJson.put("retMsg","删除失败");
        }

        return retJson;
    }

    /**
     * 根据类别ID获取类别信息
     * @param classId
     * @return
     */
    @Override
    public KnowbaseClassInfo getClassInfoById(int classId){
        KnowbaseClassInfo knowbaseClassInfo = knowbaseClassInfoMapper.selectById(classId);
        if(knowbaseClassInfo == null){
            knowbaseClassInfo = new KnowbaseClassInfo();
        }
        return knowbaseClassInfo;
    }

    /**
     * 获取类别下拉树
     * @return
     */
    @Override
    public Object getClassSelect(){
        List<JSONObject> retList = new ArrayList<>();
        QueryWrapper<KnowbaseClassInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("sort_id");
        List<KnowbaseClassInfo> list = knowbaseClassInfoMapper.selectList(queryWrapper);
        if(list != null && list.size() >0){
            for(KnowbaseClassInfo knowbaseClassInfo: list){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("value",knowbaseClassInfo.getClassCode());
                jsonObject.put("name",knowbaseClassInfo.getClassName());

                retList.add(jsonObject);
            }
        }

        return retList;
    }

    /**
     * 获取类别下拉树(已选中)
     * @return
     */
    @Override
    public Object getClassSelectSel(String classCode){
        List<JSONObject> retList = new ArrayList<>();
        QueryWrapper<KnowbaseClassInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("sort_id");
        List<KnowbaseClassInfo> list = knowbaseClassInfoMapper.selectList(queryWrapper);
        if(list != null && list.size() >0){
            for(KnowbaseClassInfo knowbaseClassInfo: list){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("value",knowbaseClassInfo.getClassCode());
                jsonObject.put("name",knowbaseClassInfo.getClassName());

                if(knowbaseClassInfo.getClassCode().equals(classCode)){
                    jsonObject.put("selected",true);
                }

                retList.add(jsonObject);
            }
        }

        return retList;
    }


}
