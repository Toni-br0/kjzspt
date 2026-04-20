package com.pearadmin.modules.sys.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pearadmin.modules.sys.domain.SysHx6Tree;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysHx6TreeMapper extends BaseMapper<SysHx6Tree> {


    /**
     * 根据 userId 获取划小列表
     *
     * @param sysHx6Tree
     * @return {@link SysHx6Tree}
     * */
    List<SysHx6Tree> selectHxByHxnumber(SysHx6Tree sysHx6Tree);

}
