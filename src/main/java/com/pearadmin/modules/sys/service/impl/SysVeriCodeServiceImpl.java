package com.pearadmin.modules.sys.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.tools.sms.SmsUtil; // 引入刚才建的短信工具类
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.sys.domain.SysChangePwdVericode;
import com.pearadmin.modules.sys.domain.SysUser;
import com.pearadmin.modules.sys.mapper.SysChangePwdVericodeMapper;
import com.pearadmin.modules.sys.service.SysVeriCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 创建日期：2025-08-27 (修改: 2026-04-09 适配短信验证码)
 * 获取验证码及修改密码服务实现
 **/
@Slf4j
@Service
public class SysVeriCodeServiceImpl implements SysVeriCodeService {

    @Resource
    private SysChangePwdVericodeMapper sysChangePwdVericodeMapper;

    @Resource
    private SysUserServiceImpl sysUserService;

    @Resource
    private PasswordEncoder passwordEncoder;

    /**
     * 获取短信验证码
     * @param param 包含 pwdUserName 的 JSON 字符串
     * @return 响应体
     */
    @Override
    public JSONObject getSysVeriCode(String param) {
        JSONObject retJson = new JSONObject();
        SysChangePwdVericode sysChangePwdVericode = null;

        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            String userName = paramJson.getString("pwdUserName");

            // 1. 验证用户是否存在
            SysUser sysUser = sysUserService.getUserByUsername(userName);
            if(sysUser == null){
                retJson.put("retCode", "-1");
                retJson.put("retMsg", "用户不存在！");
                return retJson;
            }

            // 2. 验证用户是否绑定手机号
            String phone = sysUser.getPhone();
            if(StringUtil.isEmpty(phone)) {
                retJson.put("retCode", "-1");
                retJson.put("retMsg", "该用户未绑定手机号，无法下发短信！");
                return retJson;
            }

            // 3. 验证短信验证码是否在有效时间内
            QueryWrapper<SysChangePwdVericode> vericodeQuery = new QueryWrapper<>();
            vericodeQuery.eq("user_name", userName);
            vericodeQuery.eq("is_success","0");
            vericodeQuery.orderByDesc("start_date");
            vericodeQuery.last("limit 1");

            SysChangePwdVericode selSysChangePwdVericode = sysChangePwdVericodeMapper.selectOne(vericodeQuery);
            if(selSysChangePwdVericode != null){
                LocalDateTime startDate = selSysChangePwdVericode.getStartDate();
                LocalDateTime endDate = selSysChangePwdVericode.getEndDate();
                LocalDateTime currentDate = LocalDateTime.now();

                if (startDate.isBefore(currentDate) && endDate.isAfter(currentDate)){
                    retJson.put("retCode", "-1");
                    retJson.put("retMsg", "您的验证码仍在有效期内，请勿频繁获取！");
                    return retJson;
                }
            }

            // 4. 生成新验证码并组装入库实体
            String veriCode  = RandomUtil.randomNumbers(6);
            String notticeContent = "【客经智数平台】您的验证码为："+veriCode+"，有效期为5分钟。";
            LocalDateTime startTime = LocalDateTime.now();
            LocalDateTime endTime = startTime.plusMinutes(5);

            sysChangePwdVericode = new SysChangePwdVericode();
            sysChangePwdVericode.setVericodeId(RandomUtil.randomString(32));
            sysChangePwdVericode.setUserName(userName);
            sysChangePwdVericode.setVeriCode(veriCode);
            sysChangePwdVericode.setNotticeContent(notticeContent);
            sysChangePwdVericode.setStartDate(startTime);
            sysChangePwdVericode.setEndDate(endTime);

            // 5. 调用短厅接口下发短信
            String sendMsgResult = SmsUtil.sendSms(phone, notticeContent);
            log.info("++++++++++修改密码推送短信验证码结果：{}", sendMsgResult);
            sysChangePwdVericode.setRetContent(sendMsgResult);

            // 6. 解析短信接口返回结果
            if (StringUtil.isNotEmpty(sendMsgResult)) {
                JSONObject sendMsgJson = JSONObject.parseObject(sendMsgResult);

                String resultCode = sendMsgJson.getString("resultCode");
                String status = sendMsgJson.getString("status");


                if ("0".equals(resultCode) || "200".equals(status)) {
                    retJson.put("retCode", "0");
                    retJson.put("retMsg", "短信验证码发送成功！");
                    sysChangePwdVericode.setIsSuccess("0");
                } else {
                    // 兼容各种报错字段，防止前端提示
                    String errorMsg = sendMsgJson.getString("message"); // 新网关报错字段
                    if (StringUtil.isEmpty(errorMsg)) {
                        errorMsg = sendMsgJson.getString("msg");        // 业务层报错字段
                    }
                    if (StringUtil.isEmpty(errorMsg)) {
                        errorMsg = sendMsgJson.getString("reason");     // 备用报错字段
                    }
                    if (StringUtil.isEmpty(errorMsg)) {
                        errorMsg = sendMsgResult;                       // 兜底抛出完整报文
                    }

                    retJson.put("retCode", "-1");
                    retJson.put("retMsg", "短信发送失败：" + errorMsg);
                    sysChangePwdVericode.setIsSuccess("-1");
                }
            } else {
                retJson.put("retCode", "-1");
                retJson.put("retMsg", "短信服务响应为空！");
                sysChangePwdVericode.setIsSuccess("-1");
            }

        } catch (Exception e) {
            log.error("获取验证码异常: ", e);
            retJson.put("retCode", "-1");
            retJson.put("retMsg", "获取验证码失败,请联系管理员");

            if (sysChangePwdVericode != null) {
                sysChangePwdVericode.setIsSuccess("-1");
                String errmsg = e.toString();
                if(errmsg != null && errmsg.length() > 1000){
                    errmsg = errmsg.substring(0, 1000);
                }
                sysChangePwdVericode.setRetContent(errmsg);
            }
        } finally {
            // 保存发送记录
            if(sysChangePwdVericode != null){
                sysChangePwdVericodeMapper.insert(sysChangePwdVericode);
            }
        }

        return retJson;
    }

    /**
     * 修改密码
     * @param param 包含 userName, veriCode, newPwd
     * @return 响应体
     */
    @Override
    public JSONObject changePwd(String param) {
        JSONObject retJson = new JSONObject();
        try {
            JSONObject paramJson = JSONObject.parseObject(param);
            String userName = paramJson.getString("userName");
            String veriCode = paramJson.getString("veriCode");
            String newPwd = paramJson.getString("newPwd");

            SysUser sysUser = sysUserService.getUserByUsername(userName);
            if(sysUser == null){
                retJson.put("retCode", "-1");
                retJson.put("retMsg", "用户不存在！");
                return retJson;
            }

            // 验证短信验证码是否正确且在有效期内
            QueryWrapper<SysChangePwdVericode> vericodeQuery = new QueryWrapper<>();
            vericodeQuery.eq("user_name", userName);
            vericodeQuery.eq("veri_code", veriCode);
            vericodeQuery.eq("is_success", "0");
            vericodeQuery.orderByDesc("start_date");
            vericodeQuery.last("limit 1");

            SysChangePwdVericode selSysChangePwdVericode = sysChangePwdVericodeMapper.selectOne(vericodeQuery);
            if(selSysChangePwdVericode == null){
                retJson.put("retCode", "-1");
                retJson.put("retMsg", "验证码不正确！");
                return retJson;
            } else {
                LocalDateTime startDate = selSysChangePwdVericode.getStartDate();
                LocalDateTime endDate = selSysChangePwdVericode.getEndDate();
                LocalDateTime currentDate = LocalDateTime.now();

                if(!(startDate.isBefore(currentDate) && endDate.isAfter(currentDate))){
                    retJson.put("retCode", "-1");
                    retJson.put("retMsg", "验证码已失效，请重新获取！");
                    return retJson;
                }
            }

            // 走 Security 的 passwordEncoder 加密后更新
            sysUser.setPassword(passwordEncoder.encode(newPwd));
            boolean upUserResult = sysUserService.updateById(sysUser);
            if(upUserResult){
                retJson.put("retCode", "0");
                retJson.put("retMsg", "密码修改成功！");
            } else {
                retJson.put("retCode", "-1");
                retJson.put("retMsg", "密码修改失败！");
            }
        } catch (Exception e){
            log.error("修改密码异常: ", e);
            retJson.put("retCode", "-1");
            retJson.put("retMsg", "系统异常，修改密码失败！");
        }
        return retJson;
    }
}