package com.pearadmin.modules.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pearadmin.modules.im.domain.ImPushObjectManage;
import com.pearadmin.modules.wgppt.domain.WgpptPushObjectManage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 创建日期：2025-04-27
 * IM推送对象管理
 **/

@Mapper
public interface ImPushObjectManageMapper extends BaseMapper<ImPushObjectManage> {

    /**
     * 根据批量ID，批量查询
     * @param list
     * @return
     */
    List<ImPushObjectManage> selectListByList(List<Integer> list);

    /**
     * 批量插入
     * @param list
     * @return
     */
    public int batchInsertWithId(List<ImPushObjectManage> list);


    /**
     * 根据条件，批量查询
     * @param imPushObjectManage
     * @return
     */
    List<ImPushObjectManage> selectListByWhere(@Param("imPushObjectManage") ImPushObjectManage imPushObjectManage);

    /**
     * 根据组织ID更新组织下用户所属的组织名称
     * @param orgId
     * @param orgName
     * @return
     */
    public int updateAreaNameById(String orgId,String orgName);
}
