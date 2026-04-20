package com.pearadmin.modules.sys.service;

import com.alibaba.fastjson.JSONObject;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 创建日期：2025-08-27
 * 获取验证码
 **/

public interface SysVeriCodeService {
  /**
   * 获取验证码
   * @return
   */
  public JSONObject getSysVeriCode(String param);

  /**
   * 修改密码
   * @return
   */
  public JSONObject changePwd(String param);

}
