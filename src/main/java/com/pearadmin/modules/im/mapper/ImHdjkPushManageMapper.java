package com.pearadmin.modules.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pearadmin.modules.im.domain.ImHdjkPushManage;
import com.pearadmin.modules.hdjk.domain.HdjkModelManage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 创建日期：2025-04-25
 * IM活动监控推送管理
 **/

@Mapper
public interface ImHdjkPushManageMapper extends BaseMapper<ImHdjkPushManage> {

    @Select({"<script> SELECT * FROM hdjk_model_manage t1 "+
            " LEFT JOIN im_hdjk_push_manage t2 ON t1.model_id = t2.model_id " +
            " where t2.model_id is null order by t1.create_time desc </script>"})
    List<HdjkModelManage> getModelList();


    @Select({"<script> SELECT * FROM hdjk_model_manage t1 "+
            " LEFT JOIN im_hdjk_push_manage t2 ON t1.model_id = t2.model_id and t2.model_id !=#{modelId} " +
            " where t2.model_id is null  order by t1.create_time desc </script>"})
    List<HdjkModelManage> getModelSelList(int modelId);


    @Select({"<script> select ihpm.*,hmm.send_file_name send_file_name,hmm.model_type model_type "+
            " from im_hdjk_push_manage ihpm ,hdjk_model_manage hmm  " +
            " where ihpm.model_id = hmm.model_id "+
            " <when test='modelName !=null and modelName != \"\"'> and ihpm.model_name like concat('%',#{modelName},'%') </when>"+
            " <when test='sendFileName !=null and sendFileName != \"\"'> and hmm.send_file_name like concat('%',#{sendFileName},'%') </when>"+
            " <when test='runState !=null and runState != \"\"'> and ihpm.run_state = ${runState} </when>"+
            " order by ihpm.file_creat_time desc"+
            "</script>"})
    List<ImHdjkPushManage> getHdjkPushManageList(String modelName,String sendFileName,String runState);


    /**
     * 根据开始时间和结束时间查询数据
     * @param startTime
     * @param endTime
     * @return
     */
    List<ImHdjkPushManage> getListByTime(String startTime,String endTime);
}
