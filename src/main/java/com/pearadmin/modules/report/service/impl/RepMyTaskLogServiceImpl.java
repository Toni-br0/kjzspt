package com.pearadmin.modules.report.service.impl;

import com.pearadmin.common.context.UserContext;
import com.pearadmin.common.tools.check.CheckUtil;
import com.pearadmin.modules.im.domain.ImPushObjectManage;
import com.pearadmin.modules.im.mapper.ImPushObjectManageMapper;
import com.pearadmin.modules.report.domain.ReportAutoCreateInfo;
import com.pearadmin.modules.report.domain.ReportAutoCreateLog;
import com.pearadmin.modules.report.mapper.ReportAutoCreateLogMapper;
import com.pearadmin.modules.report.service.RepMyTaskLogService;
import com.pearadmin.modules.sys.domain.SysUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * 创建日期：2025-08-06
 * 自助取数任务日志
 **/

@Slf4j
@Service
public class RepMyTaskLogServiceImpl implements RepMyTaskLogService {

    @Resource
    private ReportAutoCreateLogMapper reportAutoCreateLogMapper;

    @Resource
    private ImPushObjectManageMapper imPushObjectManageMapper;

    @Override
    public List<ReportAutoCreateLog> getMyTaskLogList(ReportAutoCreateLog reportAutoCreateLog) {

        //当前登录人信息
        SysUser currentUser = UserContext.currentUser();
        String loginUserId = currentUser.getUserId();
        /*if(loginUserId.equals("1309861917694623744")){ //管理员
            loginUserId = null;
        }*/
        boolean isAdmin = CheckUtil.hasSuperAdmin(currentUser);
        if(isAdmin){
            loginUserId = null;
        }

        List<ReportAutoCreateLog> list = reportAutoCreateLogMapper.getListByWhere(reportAutoCreateLog,loginUserId);
        if(list !=null && list.size() >0){
            for(ReportAutoCreateLog queryReportAutoCreateLog : list){
                String pushObjectType = queryReportAutoCreateLog.getPushObjectType()==null?"":queryReportAutoCreateLog.getPushObjectType();
                //不是组织维度
                if(!pushObjectType.equals("zzwd")){
                    String pushObjectId = queryReportAutoCreateLog.getPushObjectId();
                    String[] pushObjectIdArr = pushObjectId.split(";");
                    String pushObjectName = "";

                    for(int i=0;i<pushObjectIdArr.length;i++){
                        String strPushObjectId = pushObjectIdArr[i];
                        int iPushObjectId = Integer.parseInt(strPushObjectId);
                        ImPushObjectManage imPushObjectManage = imPushObjectManageMapper.selectById(iPushObjectId);
                        if(imPushObjectManage != null){
                            if(!pushObjectName.equals("")){
                                pushObjectName = pushObjectName+";"+imPushObjectManage.getPushObjectName();
                            }else{
                                pushObjectName = imPushObjectManage.getPushObjectName();
                            }
                        }
                    }

                    queryReportAutoCreateLog.setPushObjectName(pushObjectName);
                }
            }
        }

        return list;

    }
}
