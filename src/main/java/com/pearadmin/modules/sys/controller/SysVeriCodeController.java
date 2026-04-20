package com.pearadmin.modules.sys.controller;

import com.alibaba.fastjson.JSONObject;
import com.pearadmin.common.constant.ControllerConstant;
import com.pearadmin.common.web.base.BaseController;
import com.pearadmin.modules.sys.service.SysVeriCodeService;
import com.pearadmin.modules.sys.service.impl.SysVeriCodeServiceImpl;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 创建日期：2025-08-27
 **/
@RestController
@Api(tags = {"获取验证码"})
@RequestMapping("/veriCode/sysVeriCode")
public class SysVeriCodeController extends BaseController {

    @Resource
    private SysVeriCodeServiceImpl sysVeriCodeService;


    /**
     * 获取验证码
     * @return
     */
    @PostMapping("getSysVeriCode")
    public JSONObject getSysVeriCode(@RequestBody String param) {
        return sysVeriCodeService.getSysVeriCode(param);
    }

    /**
     * 修改密码
     * @return
     */
    @PostMapping("changePwd")
    public JSONObject changePwd(@RequestBody String param) {
        return sysVeriCodeService.changePwd(param);
    }

}
