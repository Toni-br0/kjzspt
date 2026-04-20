package com.pearadmin.modules.knowBase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pearadmin.modules.knowBase.domain.KnowbaseDraftInfo;
import com.pearadmin.modules.knowBase.domain.KnowbaseKnowInfo;
import com.pearadmin.modules.knowBase.domain.KnowbaseUndercarrInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 创建日期：2025-6-23
 * 知识信息表
 **/

@Mapper
public interface KnowbaseKnowInfoMapper extends BaseMapper<KnowbaseKnowInfo> {
    /**
     * 查询知识信息表(已发布)
     * @return
     */
    public List<KnowbaseKnowInfo> selKnowInfoList(@Param("knowbaseKnowInfo") KnowbaseKnowInfo knowbaseKnowInfo);

    /**
     * 根据知识类别编码查询在已发布知识中的总数
     * @param knowClass
     * @return
     */
    public int selKnowInfoByClassCode(List<String> knowClass);

    /**
     * 查询知识信息表(草稿箱)
     * @return
     */
    public List<KnowbaseDraftInfo> selDraftInfoList(@Param("knowbaseKnowInfo") KnowbaseKnowInfo knowbaseKnowInfo);


    /**
     * 查询知识下架信息表
     * @return
     */
    public List<KnowbaseUndercarrInfo> selUndercarrInfoList(@Param("knowbaseKnowInfo") KnowbaseKnowInfo knowbaseKnowInfo);

    /**
     * 根据知识类别编码查询在已下架知识中的总数
     * @param knowClass
     * @return
     */
    public int selUndercarrInfoByClassCode(List<String> knowClass);

    /**
     * 根据知识类别编码查询在草稿箱中的总数
     * @param knowClass
     * @return
     */
    public int selDraftInfoByClassCode(List<String> knowClass);


}
