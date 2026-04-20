package com.pearadmin.modules.im.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.im.domain.ImPushObjectManage;
import com.pearadmin.modules.im.service.IImDorisConnService;
import com.pearadmin.modules.wgppt.domain.VWgzsUserLevelD;
import com.pearadmin.modules.wgppt.mapper.VWgzsUserLevelDMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * 创建日期：2026-02-03
 * 连接Doris数据库
 **/

@Slf4j
@Service
public class ImDorisConnServiceImpl implements IImDorisConnService {

    @Resource
    private VWgzsUserLevelDMapper vWgzsUserLevelDMapper;

    /**
     * 根据条件查询doris组织树信息
     * @param imPushObjectManage
     * @return
     */
    @Override
    @DS("second")
    public List<VWgzsUserLevelD> selectImPushObjectByWhere(ImPushObjectManage imPushObjectManage,String dataLevel) {

        List<VWgzsUserLevelD> vWgzsUserLevelDList = vWgzsUserLevelDMapper.getWgUserLeveIdListByWhere(imPushObjectManage.getPushObjectId(),imPushObjectManage.getPushObjectName(),dataLevel,imPushObjectManage.getObjectAreaId());
        return vWgzsUserLevelDList;
    }

}
