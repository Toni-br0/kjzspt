package com.pearadmin.modules.report.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pearadmin.common.context.UserContext;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.common.web.domain.request.PageDomain;
import com.pearadmin.common.web.domain.response.Result;
import com.pearadmin.common.web.domain.response.module.ResultTable;
import com.pearadmin.modules.report.domain.ReportCondit;
import com.pearadmin.modules.report.domain.ReportIndex;
import com.pearadmin.modules.report.mapper.ReportConditMapper;
import com.pearadmin.modules.report.mapper.ReportIndexMapper;
import com.pearadmin.modules.report.service.RepIndexManageService;
import com.pearadmin.modules.sys.domain.SysDept;
import com.pearadmin.modules.sys.domain.SysDict;
import com.pearadmin.modules.sys.domain.SysUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建日期：2025-07-03
 * 指标管理
 **/

@Slf4j
@Service
public class RepIndexManageServiceImpl implements RepIndexManageService {

    @Resource
    private ReportIndexMapper reportIndexMapper;
    @Autowired
    private ReportConditMapper reportConditMapper;

    /**
     * Describe: 自助取数指标列表数据
     * Param SysDept PageDomain
     * Return 自助取数指标列表数据
     */
    @Override
    public List<ReportIndex> getIndexList(ReportIndex reportIndex){
        return reportIndexMapper.selIndexList(reportIndex);

    }

    /**
     * Describe: 自助取数指标列表数据
     * Param SysDept PageDomain
     * Return 自助取数指标列表数据
     */
    @Override
    public List<ReportIndex> getSubIndexList(ReportIndex reportIndex){
        return reportIndexMapper.selSubIndexList(reportIndex);

    }

    /**
     * Describe: 自助取数指标列表数据
     * Param SysDept PageDomain
     * Return 自助取数指标列表数据
     */
    @Override
    public PageInfo<ReportIndex> getIndexListNew(ReportIndex reportIndex, PageDomain pageDomain){
        PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
        reportIndex.setIsQuery("1");
        List<ReportIndex> list = reportIndexMapper.selIndexListTable(reportIndex);

        if(list != null && list.size() >0){
            ReportIndex parentReportIndex = null;
            for(ReportIndex queryReportIndex : list){
                parentReportIndex = reportIndexMapper.selectById(queryReportIndex.getParentId());
                if(parentReportIndex != null){
                    queryReportIndex.setParentIndexName(parentReportIndex.getIndexName());
                }
            }
        }

        return new PageInfo<>(list);

    }

    /**
     * Describe: 自助取数指标列表数据
     * Param SysDept PageDomain
     * Return 自助取数指标列表数据
     */
    @Override
    public PageInfo<ReportIndex> getIndexSubList(ReportIndex reportIndex, PageDomain pageDomain){
        PageHelper.startPage(pageDomain.getPage(), pageDomain.getLimit());
        reportIndex.setIsQuery("0");
        List<ReportIndex> list = reportIndexMapper.selIndexListTable(reportIndex);
        return new PageInfo<>(list);

    }


    /**
     * Describe: 保存指标信息
     * Param SysDept
     * Return 执行结果
     */
    @Override
    public JSONObject saveIndex(ReportIndex reportIndex){
        JSONObject retJsonObject = new JSONObject();
        //当前登录人信息
        SysUser currentUser = UserContext.currentUser();
        reportIndex.setCreateUserId(currentUser.getUserId());
        reportIndex.setCreateTime(LocalDateTime.now());

        if(StringUtil.isNotEmpty(reportIndex.getColumnName())){
            //判断指标列名是否存在
            QueryWrapper<ReportIndex> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("column_name", reportIndex.getColumnName());
            queryWrapper.eq("table_name", reportIndex.getTableName());
            queryWrapper.last("limit 1");

            ReportIndex queryReportIndex = reportIndexMapper.selectOne(queryWrapper);
            if(queryReportIndex != null){
                retJsonObject.put("retCode","-1");
                retJsonObject.put("retMsg","指标列名已存在");
                return retJsonObject;
            }
        }

        int result = reportIndexMapper.insert(reportIndex);
        if(result > 0){
            retJsonObject.put("retCode","0");
            retJsonObject.put("retMsg","保存成功");
        }else{
            retJsonObject.put("retCode","-1");
            retJsonObject.put("retMsg","保存失败");
        }
        return retJsonObject;
    }

    /**
     * 根据id获取子指标
     * @param indexId
     * @return
     */
    @Override
    public List<ReportIndex> selectByParentId(int indexId){
        LambdaQueryWrapper<ReportIndex> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportIndex::getParentId,indexId);
        return reportIndexMapper.selectList(wrapper);
    }


    /**
     * 根据ID删除指标
     * @param indexId
     * @return
     */
    @Override
    public JSONObject remove(int indexId){
        JSONObject retJson = new JSONObject();

        ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
        if(reportIndex != null){
            int parentId = reportIndex.getParentId();
            if(parentId ==0){
                retJson.put("retCode","-1");
                retJson.put("retMsg","删除失败,不能删除根指标");
                return retJson;
            }

            //查询指标是否已在条件中使用
            QueryWrapper<ReportCondit> conditQueryWrapper = new QueryWrapper<>();
            conditQueryWrapper.eq("index_id",indexId);

            long conditCount = reportConditMapper.selectCount(conditQueryWrapper);
            if(conditCount >0){
                retJson.put("retCode","-1");
                retJson.put("retMsg","删除失败,此指标已在条件中使用");
                return retJson;

            }

            int result = reportIndexMapper.deleteById(indexId);
            if(result > 0){
                retJson.put("retCode","0");
                retJson.put("retMsg","删除成功");
                return retJson;
            }else{
                retJson.put("retCode","-1");
                retJson.put("retMsg","删除失败");
                return retJson;
            }

        }else{
            retJson.put("retCode","-1");
            retJson.put("retMsg","删除失败");
            return retJson;
        }
    }

    /**
     * 根据ID获取指标
     * @param indexId
     * @return
     */
    @Override
    public ReportIndex getById(int indexId){
       return reportIndexMapper.selectById(indexId);
    }


    /**
     * Describe: 修改指标
     * Param reportIndex
     * Return 执行结果
     */
    @Override
    public JSONObject updateIndex(ReportIndex reportIndex){

        JSONObject retJsonObject = new JSONObject();

        if(StringUtil.isNotEmpty(reportIndex.getColumnName())){
            QueryWrapper<ReportIndex> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("column_name", reportIndex.getColumnName());
            queryWrapper.eq("table_name", reportIndex.getTableName());
            queryWrapper.ne("index_id", reportIndex.getIndexId());
            queryWrapper.last("limit 1");

            ReportIndex queryReportIndex = reportIndexMapper.selectOne(queryWrapper);
            if(queryReportIndex != null){
                retJsonObject.put("retCode","-1");
                retJsonObject.put("retMsg","指标列名已存在");
                return retJsonObject;
            }
        }

        reportIndex.setCreateTime(LocalDateTime.now());

        //当前登录人信息
        SysUser currentUser = UserContext.currentUser();
        reportIndex.setCreateUserId(currentUser.getUserId());

        int result = reportIndexMapper.updateById(reportIndex);
        if(result > 0){
            ReportIndex queryIndex = reportIndexMapper.selectById(reportIndex.getIndexId());
            if(queryIndex != null){
                String isQuery = queryIndex.getIsQuery();
                if(StringUtil.isNotEmpty(isQuery) && isQuery.equals("1")){
                    //修改子指标信息
                    reportIndexMapper.updateSubIndex(reportIndex);
                }
            }

            retJsonObject.put("retCode","0");
            retJsonObject.put("retMsg","保存成功");
        }else{
            retJsonObject.put("retCode","-1");
            retJsonObject.put("retMsg","保存失败");
        }

        return retJsonObject;
    }

    /**
     * Describe: 根据ID获取指标类型和分类
     * Param: id
     * Return: Result
     */
    public JSONObject getTypeClass(int indexId){
        JSONObject retJsonObject = new JSONObject();
        ReportIndex reportIndex = reportIndexMapper.selectById(indexId);
        if(reportIndex != null){
            retJsonObject.put("retCode","0");
            retJsonObject.put("indexType",reportIndex.getIndexType());
            retJsonObject.put("dataCycle",reportIndex.getDataCycle());
            retJsonObject.put("analRole",reportIndex.getAnalRole());
            retJsonObject.put("classifyId",reportIndex.getClassifyId());
            retJsonObject.put("tableName",reportIndex.getTableName());
            retJsonObject.put("isFiveInten",reportIndex.getIsFiveInten());
            retJsonObject.put("isIncludeNoStand",reportIndex.getIsIncludeNoStand());
        }else{
            retJsonObject.put("retCode","-1");

        }

        return  retJsonObject;
    }

}
