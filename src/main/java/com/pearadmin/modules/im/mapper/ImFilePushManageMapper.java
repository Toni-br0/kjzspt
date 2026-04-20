package com.pearadmin.modules.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pearadmin.modules.im.domain.ImFilePushManage;
import com.pearadmin.modules.ppt.domain.PptModelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 创建日期：2025-04-25
 * IM文件推送管理
 **/

@Mapper
public interface ImFilePushManageMapper extends BaseMapper<ImFilePushManage> {

    @Select({"<script> SELECT * FROM ppt_model_config t1 "+
            " LEFT JOIN im_file_push_manage t2 ON t1.model_id = t2.model_id " +
            " where t2.model_id is null order by t1.create_time desc </script>"})
    List<PptModelConfig> getModelList();


    @Select({"<script> SELECT * FROM ppt_model_config t1 "+
            " LEFT JOIN im_file_push_manage t2 ON t1.model_id = t2.model_id and t2.model_id !=#{modelId} " +
            " where t2.model_id is null  order by t1.create_time desc </script>"})
    List<PptModelConfig> getModelSelList(int modelId);
}
