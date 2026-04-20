package com.pearadmin.modules.im.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 *
 * 创建日期：2026-02-05
 * IM推送对象绑定信息
 **/

@Data
@Alias("ImBindInfo")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImBindInfo {

    /**
     * 绑定ID
     */
    @TableId
    private String bindId;

    /**
     * 绑定名称
     */
    private String bindName;

    /**
     * 绑定时间
     */
    private LocalDateTime bindTime;


}
