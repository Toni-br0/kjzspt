package com.pearadmin.modules.report.service.impl;

import com.pearadmin.common.context.UserContext;
import com.pearadmin.common.tools.check.CheckUtil;
import com.pearadmin.common.tools.string.StringUtil;
import com.pearadmin.modules.im.domain.ImPushObjectManage;
import com.pearadmin.modules.im.mapper.ImPushObjectManageMapper;
import com.pearadmin.modules.report.domain.ReportAutoCreateLog;
import com.pearadmin.modules.report.domain.ReportIndexWarningLog;
import com.pearadmin.modules.report.mapper.ReportIndexWarningLogMapper;
import com.pearadmin.modules.report.service.ReportIndexWarningLogService;
import com.pearadmin.modules.sys.domain.SysUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * 创建日期：2025-10-22
 * 自助取数指标预警日志
 **/

@Slf4j
@Service
public class ReportIndexWarningLogServiceImpl implements ReportIndexWarningLogService {

    @Resource
    private ReportIndexWarningLogMapper reportIndexWarningLogMapper;

    @Resource
    private ImPushObjectManageMapper imPushObjectManageMapper;

    /**
     * 获取自助取数指标预警日志列表数据
     * @param reportIndexWarningLog
     * @return
     */
    @Override
    public List<ReportIndexWarningLog> getIndexWarningLogList(ReportIndexWarningLog reportIndexWarningLog) {

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

        List<ReportIndexWarningLog> list = reportIndexWarningLogMapper.getListByWhere(reportIndexWarningLog,loginUserId);
        if(list !=null && list.size() >0){
            for(ReportIndexWarningLog queryReportIndexWarningLog : list){
                String pushOtherObjectId = queryReportIndexWarningLog.getPushOtherObjectId();
                if(StringUtil.isNotEmpty(pushOtherObjectId)){
                    String[] pushObjectIdArr = pushOtherObjectId.split(";");
                    String pushOtherObjectName = "";

                    for(int i=0;i<pushObjectIdArr.length;i++){
                        String strPushObjectId = pushObjectIdArr[i];
                        int iPushObjectId = Integer.parseInt(strPushObjectId);
                        ImPushObjectManage imPushObjectManage = imPushObjectManageMapper.selectById(iPushObjectId);
                        if(imPushObjectManage != null){
                            if(!pushOtherObjectName.equals("")){
                                pushOtherObjectName = pushOtherObjectName+";"+imPushObjectManage.getPushObjectName();
                            }else{
                                pushOtherObjectName = imPushObjectManage.getPushObjectName();
                            }
                        }
                    }

                    queryReportIndexWarningLog.setPushOtherObjectName(pushOtherObjectName);
                }

            }
        }

        return list;
    }
}
