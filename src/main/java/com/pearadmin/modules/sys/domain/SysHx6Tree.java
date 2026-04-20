package com.pearadmin.modules.sys.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pearadmin.common.web.base.BaseDomain;
import lombok.Data;
import org.apache.ibatis.type.Alias;

@Data
@Alias("SysHx6Tree")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SysHx6Tree {

    /**
     * 划小编号
     */
    @TableId
    private String hxNumber;

    private String hxName;

    private String hxLevel;
    /**
     * 父级编号
     */
    private String parentHxNumber;

    /**
     * 排序
     */
    private Integer orderNum;

}
