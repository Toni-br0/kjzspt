package com.pearadmin.modules.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pearadmin.modules.im.domain.ImPushRolePersonManage;
import com.pearadmin.modules.wgppt.domain.WgpptPushObjectManage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 *
 * 创建日期：2026-04-03
 * IM推送角色人员管理
 **/
@Mapper
public interface ImPushRolePersonManageMapper extends BaseMapper<ImPushRolePersonManage> {

    /**
     * 查询角色人员列表
     * @param imPushRolePersonManage
     * @return
     */
    public List<ImPushRolePersonManage> getListByRoleId(@Param("imPushRolePersonManage") ImPushRolePersonManage imPushRolePersonManage);


    public int batchInsertWithId(List<ImPushRolePersonManage> list);
}
