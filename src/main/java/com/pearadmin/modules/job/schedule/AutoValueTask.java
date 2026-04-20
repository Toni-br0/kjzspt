package com.pearadmin.modules.job.schedule;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.quartz.base.BaseQuartz;
import com.pearadmin.common.tools.DateTimeUtil;
import com.pearadmin.modules.ppt.domain.ParamPojo;
import com.pearadmin.modules.ppt.domain.PptModelConfig;
import com.pearadmin.modules.ppt.mapper.PptModelConfigMapper;
import com.pearadmin.modules.ppt.service.impl.AutoValueServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 模板内容自动生成定时任务
 */

@Slf4j
@Component("autoValueTask")
public class AutoValueTask implements BaseQuartz {

    @Resource
    private PptModelConfigMapper pptModelConfigMapper;

    @Resource
    private AutoValueServiceImpl autoValueService;

    /**
     * 任务实现
     */
    @Override
    public void run(String params) throws Exception {
        log.info("autoValueTask-Params === >> " + params);
        log.info("++++++++++++++++autoValueTask: 开始执行");
        ParamPojo paramPojo = new ParamPojo();

        try {
            QueryWrapper<PptModelConfig> modelQuery = new QueryWrapper<>();
            modelQuery.select("model_id","model_level");
            List<PptModelConfig> modelList = pptModelConfigMapper.selectList(modelQuery);
            if(modelList != null && modelList.size() > 0) {
                //获取当前日期的上一个月
                String accountPeriod = DateTimeUtil.getLastMonth("yyyyMM");

                for(PptModelConfig pptModelConfig : modelList) {
                    int modelId = pptModelConfig.getModelId();
                    String modelLevel = pptModelConfig.getModelLevel();
                    paramPojo.setModelId(modelId);
                    paramPojo.setDzCode(modelLevel);
                    paramPojo.setAccountPeriod(accountPeriod);

                    JSONObject retJsonObject = autoValueService.autoValue(paramPojo);
                    String retCode = retJsonObject.getString("retCode");
                    String retMsg = retJsonObject.getString("retMsg");
                    if(retCode.equals("0")){
                        log.info("++++++++++++++++autoValueTask: 执行成功");
                    }else{
                        log.info("++++++++++++++++autoValueTask: 执行失败");
                        log.info("++++++++++++++++autoValueTask异常信息: {}",retMsg);
                    }

                    Thread.sleep(3000l);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
