package com.smart.app.service.Impl;

import com.google.common.collect.Lists;
import com.smart.app.common.ResposeCode;
import com.smart.app.common.ServerResponse;
import com.smart.app.service.FileService;

import com.smart.app.util.FTPUtil;
import com.smart.app.vo.ProductDetailVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    private Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    public String upload(MultipartFile file,String path){
       //文件名
        String originalFilename = file.getOriginalFilename();
        //扩展名
        String substring = originalFilename.substring(originalFilename.lastIndexOf(".")+1);
        //上传文件名
        String uploadFilName = UUID.randomUUID().toString()+"."+"fileExtensionName";

        logger.info(originalFilename,path,uploadFilName);

        File fileDir = new File(path);
        //判断是否存在
        if (!fileDir.exists()){
            //如果不存在 创建 赋予权限可写
            fileDir.setWritable(true);
            fileDir.mkdirs();
        }
        File targetFile = new File(path,uploadFilName);

        try {
            file.transferTo(targetFile);
            //上传文件到tfp服务器
            FTPUtil.uploadFile(Lists.<File>newArrayList(targetFile));

            targetFile.delete();

        } catch (IOException e) {
            logger.error("上传文件异常",e);
            return null;
        }
        return targetFile.getName();
    }




}
