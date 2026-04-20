package com.pearadmin.modules.im.service;

import com.pearadmin.modules.im.domain.ImPushObjectManage;
import com.pearadmin.modules.wgppt.domain.VWgzsUserLevelD;

import java.util.List;

/**
 *
 * 创建日期：2026-02-03
 * 连接Doris数据库
 **/

public interface IImDorisConnService {

    /**
     * 根据条件查询doris组织树信息
     * @param imPushObjectManage
     * @return
     */
    public List<VWgzsUserLevelD> selectImPushObjectByWhere(ImPushObjectManage imPushObjectManage,String dataLevel);

}
