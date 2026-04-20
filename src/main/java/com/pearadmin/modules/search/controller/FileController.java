/*
package com.pearadmin.modules.search.controller;

*/
/**
 * 创建日期：2024-11-26
 **//*





import com.pearadmin.modules.search.domain.FileDTO;
import com.pearadmin.modules.search.service.IFileService;
import com.pearadmin.modules.search.util.ResultDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@RestController
@RequestMapping("/elasticsearch/file")
public class FileController {

    @Autowired
    private IFileService fileService;


*/
/**
     *  保存文件
     * @param file
     * @return
     * @throws Exception*//*



    @PostMapping(value = "/saveFile")
    public ResultDto<?> saveFile(@RequestParam("file") MultipartFile file) throws Exception {
        fileService.saveFile(file);
        log.info("保存文件成功");
        return ResultDto.ok("保存文件成功");
    }


*/
/**
     * 关键字查询-repository
     *
     * @throws Exception*//*




    @PostMapping(value = "/search")
    public ResultDto<?> search(@RequestBody FileDTO dto) {
        return ResultDto.OK(fileService.search(dto));
    }



    */
/**
     * 关键字查询-原生方法
     *
     * @throws Exception*//*


    @PostMapping(value = "/searchPage")
    public ResultDto<?> searchPage(@RequestBody FileDTO dto) {
        return ResultDto.OK(fileService.searchPage(dto));
    }

    @PostMapping(value = "/delById")
    public ResultDto<?> delById(@RequestBody String ids) {
        return ResultDto.OK(fileService.deleteById(ids));
    }

}
*/
