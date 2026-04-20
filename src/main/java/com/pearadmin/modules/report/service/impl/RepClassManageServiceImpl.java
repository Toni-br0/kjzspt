package com.pearadmin.modules.report.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.constant.CommonConstant;
import com.pearadmin.common.context.UserContext;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.knowBase.domain.KnowbaseClassInfo;
import com.pearadmin.modules.report.domain.ReportClassify;
import com.pearadmin.modules.report.domain.ReportCondit;
import com.pearadmin.modules.report.domain.ReportIndex;
import com.pearadmin.modules.report.mapper.ReportClassifyMapper;
import com.pearadmin.modules.report.mapper.ReportConditMapper;
import com.pearadmin.modules.report.mapper.ReportIndexMapper;
import com.pearadmin.modules.report.service.RepClassManageService;
import com.pearadmin.modules.sys.domain.SysUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 创建日期：2025-07-01
 **/

@Slf4j
@Service
public class RepClassManageServiceImpl implements RepClassManageService {


    @Resource
    private ReportClassifyMapper reportClassifyMapper;
    @Autowired
    private ReportConditMapper reportConditMapper;

    @Resource
    private ReportIndexMapper reportIndexMapper;

    /**
     * 获取分类管理列表数据
     * @param reportClassify
     * @return
     */
    @Override
    public List<ReportClassify> getClassList(ReportClassify reportClassify){
        return reportClassifyMapper.selClassList(reportClassify);
    }

    /**
     * 保存分类数据
     * @param reportClassify
     * @return
     */
    @Override
    public JSONObject saveClassInfo(ReportClassify reportClassify) {
        JSONObject retJson = new JSONObject();
        try {
            boolean isAdd = true;
            Integer classifyId = reportClassify.getClassifyId();
            if(classifyId !=null && classifyId !=0){
                isAdd = false;
            }

            //判断类别编码是否存在
            QueryWrapper<ReportClassify> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("classify_code",reportClassify.getClassifyCode());

            ReportClassify queryReportClassify = reportClassifyMapper.selectOne(queryWrapper);
            if(queryReportClassify != null){
                if(isAdd){
                    retJson.put("retCode","-1");
                    retJson.put("retMsg","新增失败，编码已存在");
                    return retJson;
                }else{
                    if(!queryReportClassify.getClassifyId().equals(classifyId)){
                        retJson.put("retCode","-1");
                        retJson.put("retMsg","修改失败，编码已存在");
                        return retJson;
                    }

                }
            }

            //当前登录人信息
            SysUser currentUser = UserContext.currentUser();

            reportClassify.setCreateTime(LocalDateTime.now());
            reportClassify.setCreateUserId(currentUser.getUserId());

            int result = 0;
            if(isAdd){
                result = reportClassifyMapper.insert(reportClassify);
                if(result > 0){
                    retJson.put("retCode","0");
                    retJson.put("retMsg","新增成功");
                }else{
                    retJson.put("retCode","-1");
                    retJson.put("retMsg","新增失败");
                }

            }else{
                result = reportClassifyMapper.updateById(reportClassify);
                if(result > 0){
                    retJson.put("retCode","0");
                    retJson.put("retMsg","修改成功");
                }else{
                    retJson.put("retCode","-1");
                    retJson.put("retMsg","修改失败");
                }
            }

        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","-1");
            retJson.put("retMsg","保存失败");
        }
        return retJson;
    }


    /**
     * 通过分类编码获取分类信息
     */
    @Override
    public ReportClassify getClassById(String classifyId){
        return reportClassifyMapper.selectById(classifyId);
    }

    /**
     * 单个删除分类数据
     * Param: id
     * Return: 文件
     */
    @Override
    public JSONObject remove(int classifyId){
        JSONObject retJson = new JSONObject();
        try {
            //判断是否在条件中使用
            /*QueryWrapper<ReportCondit> queryConditWrapper = new QueryWrapper<>();
            queryConditWrapper.eq("classify_id",classifyId);
            long conditCount = reportConditMapper.selectCount(queryConditWrapper);
            if(conditCount > 0){
                retJson.put("retCode","-1");
                retJson.put("retMsg","删除失败,此类型已在条件中使用");
                return retJson;
            }*/

            //判断是否在指标中使用
            QueryWrapper<ReportIndex> queryIndexWrapper = new QueryWrapper<>();
            queryIndexWrapper.eq("classify_id",classifyId);
            long indexCount = reportIndexMapper.selectCount(queryIndexWrapper);
            if(indexCount >0){
                retJson.put("retCode","-1");
                retJson.put("retMsg","删除失败,此类型已在指标中使用");
                return retJson;
            }

            int result = reportClassifyMapper.deleteById(classifyId);
            if(result >0){
                retJson.put("retCode","0");
                retJson.put("retMsg","删除成功");
            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","删除失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","-1");
            retJson.put("retMsg","删除失败,系统异常,请联系管理员");
        }

        return retJson;
    }

    /**
     * 批量删除分类数据
     * @param classifyIds
     * @return
     */
    @Override
    public JSONObject batchRemove(String classifyIds) {
        JSONObject retJson = new JSONObject();
        try {
            List<Integer> delList = new ArrayList<>();
            for (String classId : classifyIds.split(CommonConstant.COMMA)) {
                int iClassId = Integer.parseInt(classId);
                delList.add(iClassId);
            }

            /*int conditCount = reportConditMapper.selConditCountByClassid(delList);
            if(conditCount >0){
                retJson.put("retCode","-1");
                retJson.put("retMsg","删除失败,此分类已在条件中使用");
                return retJson;
            }*/

            int indexCount = reportIndexMapper.selIndexCountByClassid(delList);
            if(indexCount >0){
                retJson.put("retCode","-1");
                retJson.put("retMsg","删除失败,选中的分类已在指标中使用");
                return retJson;
            }

            int result = reportClassifyMapper.deleteBatchIds(delList);
            if(result >0){
                retJson.put("retCode","0");
                retJson.put("retMsg","删除成功");
            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","删除失败");
            }

        }catch (Exception e){
            e.printStackTrace();
            retJson.put("retCode","-1");
            retJson.put("retMsg","删除失败,系统异常,请联系管理员");
        }
        return retJson;
    }

    /**
     * 获取分类下拉框数据
     * @return
     */
    @Override
    public Object getClassSelect(){
        JSONArray retJsonArray = new JSONArray();
        List<ReportClassify> list = reportClassifyMapper.selectList(null);
        if(list != null && list.size() >0){
            for(ReportClassify reportClassify : list){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("classifyId",reportClassify.getClassifyId());
                jsonObject.put("classifyName",reportClassify.getClassifyName());
                retJsonArray.add(jsonObject);
            }
        }

        return retJsonArray;
    }


    /**
     * 获取分类管理列表数据
     * @return
     */
    public List<ReportClassify> getClassList(){
        QueryWrapper<ReportClassify> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("sort_id");
        List<ReportClassify> list = reportClassifyMapper.selectList(queryWrapper);
        return list;
    }

}
