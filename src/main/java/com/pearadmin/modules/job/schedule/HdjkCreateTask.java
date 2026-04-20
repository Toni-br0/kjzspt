package com.pearadmin.modules.job.schedule;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pearadmin.common.quartz.base.BaseQuartz;
import com.pearadmin.modules.hdjk.domain.HdjkModelManage;
import com.pearadmin.modules.hdjk.mapper.HdjkModelManageMapper;
import com.pearadmin.modules.hdjk.service.impl.HdjkModelManageServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 创建日期：2025-05-27
 * 活动监控任务
 **/

@Slf4j
@Component("hdjkCreateTask")
public class HdjkCreateTask implements BaseQuartz {

  @Resource
  private HdjkModelManageMapper hdjkModelManageMapper;

  @Resource
  private HdjkModelManageServiceImpl hdjkModelManageService;

  @Override
  public void run(String params) throws Exception {
     try {

       String[] paramArr = params.split(";");
       String type = paramArr[0];
       String modelName = paramArr[1];
       //日报
       if(type.equals("1")){
         QueryWrapper<HdjkModelManage> queryWrapper = new QueryWrapper<>();
         queryWrapper.eq("model_type","1");
         queryWrapper.eq("model_name",modelName);
         List<HdjkModelManage> modelList = hdjkModelManageMapper.selectList(queryWrapper);

         if(modelList !=null && modelList.size() >0){
           int modelId = 0;
           for(HdjkModelManage hdjkModelManage : modelList){
             modelId = hdjkModelManage.getModelId();
             //调用创建文件的方法
             boolean result = hdjkModelManageService.createFile(modelId);
             log.info("+++++++++++hdjkCreateTask_{}执行结果：{}",modelName,result);
           }
         }
       }

     }catch (Exception e) {
       e.printStackTrace();
     }
  }

}
