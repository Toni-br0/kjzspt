package com.pearadmin.modules.sys.controller;

import cn.hutool.crypto.SecureUtil;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.sys.domain.SysUser;
import com.pearadmin.modules.sys.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.net.URLEncoder;

/**
 * 大屏单点登录(SSO)处理类
 */
@Slf4j
@Controller
@RequestMapping("/sso")
public class SsoLoginController {

    @Resource
    private SysUserService sysUserService;

    @Resource
    private UserDetailsService userDetailsService;

    private static final String SSO_SECRET = "kjzs_bigscreen_666";

    /**
     * 免密登录入口
     */
    @GetMapping("/login")
    public String ssoLogin(@RequestParam("username") String username,
                           @RequestParam("timestamp") Long timestamp,
                           @RequestParam("sign") String sign) {

        log.info("【SSO登录】收到跳转请求 -> 手机号: {}, 时间戳: {}, 签名: {}", username, timestamp, sign);

        try {
            // 1. 校验时效性（超过5分钟认为链接失效，防止重放攻击）
            long diff = Math.abs(System.currentTimeMillis() - timestamp);
            if (diff > 5 * 60 * 1000) {
                log.error("【SSO登录失败】链接已过期，误差：{} 毫秒", diff);
                return "redirect:/login?sso_msg=timeout";
            }

            // 2. 校验签名
            String rawStr = username + timestamp + SSO_SECRET;
            String localSign = SecureUtil.md5(rawStr).toLowerCase();
            if (!localSign.equals(sign)) {
                log.error("【SSO登录失败】签名错误。接收：{}, 预期：{}", sign, localSign);
                return "redirect:/login?sso_msg=sign_error";
            }

            // 3. 校验账号是否存在 (username 在本平台即手机号)
            SysUser sysUser = sysUserService.getUserByUsername(username);
            if (sysUser == null) {
                log.warn("【SSO登录失败】账号不存在：{}", username);
                // 特殊错误码，用于触发前端友好提示
                return "redirect:/login?sso_msg=not_registered&phone=" + username;
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("【SSO登录成功】用户 {} 已通过大屏跳转认证，进入首页。", username);
            return "redirect:/index";

        } catch (Exception e) {
            log.error("【SSO登录异常】", e);
            return "redirect:/login?sso_msg=system_error";
        }
    }
}