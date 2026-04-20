package com.pearadmin.modules.sys.controller;

import com.pearadmin.common.web.base.BaseController;
import com.pearadmin.modules.ppt.domain.PptModelConfig;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/**
 *
 * 创建日期：2026-04-15
 * 备份insert语句
 **/

@RestController
@Api(tags = {"备份insert语句"})
@Slf4j
@RequestMapping("/sys/backInsert")
public class BackInsertController extends BaseController {

    /**
     * 基础路径
     */
    private final String MODULE_PATH = "system/backInsert/";

    /**
     * 打开备份insert语句页面
     * @return
     */
    @GetMapping("toBackInsert")
    @ApiOperation(value = "打开备份insert语句页面",notes = "打开备份insert语句页面")
    @PreAuthorize("hasPermission('/sys/backInsert/toBackInsert','sys:backInsert:toBackInsert')")
    public ModelAndView toBackInsert(Model model) {

        return jumpPage(MODULE_PATH + "backInsert");
    }

}
