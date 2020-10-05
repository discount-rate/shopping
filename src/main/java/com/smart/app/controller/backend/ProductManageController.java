package com.smart.app.controller.backend;

import com.google.common.collect.Maps;
import com.smart.app.common.Const;
import com.smart.app.common.ResposeCode;
import com.smart.app.common.ServerResponse;
import com.smart.app.entity.TbProduct;
import com.smart.app.entity.TbUser;
import com.smart.app.service.FileService;
import com.smart.app.service.ProduceService;
import com.smart.app.service.UserService;
import com.smart.app.util.PropertiesUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

public class ProductManageController {

    @Resource
    UserService userService;
    @Resource
    ProduceService produceService;
    @Resource
    FileService fileService;

    /**
     * 保持或更新商品
     *
     * @param product
     * @return
     */
    @PostMapping("/productSave")
    public ServerResponse productSave(HttpSession session, TbProduct product) {

        TbUser tbUser = (TbUser) session.getAttribute(Const.CURRENT_USER);
        if (tbUser == null) {
            return ServerResponse.createByErrorCodeMassage(ResposeCode.NEED_LOGIN.getCode(), "用户未登录，请先登录");
        }
        //判断管理员权限
        if (userService.checkAdminRole(tbUser).isSuccess()) {
            return produceService.saveOrUpdateProduct(product);
        } else {
            return ServerResponse.createByErrorMassage("无权限");
        }
    }

    /**
     * 跟新产品上下架
     *
     * @param session
     * @return
     */
    @PostMapping("/setSaleStatus")
    public ServerResponse setSaleStatus(HttpSession session, Integer productId, Integer status) {

        TbUser tbUser = (TbUser) session.getAttribute(Const.CURRENT_USER);
        if (tbUser == null) {
            return ServerResponse.createByErrorCodeMassage(ResposeCode.NEED_LOGIN.getCode(), "用户未登录，请先登录");
        }
        //判断管理员权限
        if (userService.checkAdminRole(tbUser).isSuccess()) {

            return produceService.setSaleStatus(productId, status);
        } else {
            return ServerResponse.createByErrorMassage("无权限");
        }
    }


    /**
     * 查询商品详情
     *
     * @param session
     * @param productId
     * @return
     */
    @PostMapping("/getDetail")
    public ServerResponse getDetail(HttpSession session, Integer productId) {

        TbUser tbUser = (TbUser) session.getAttribute(Const.CURRENT_USER);
        if (tbUser == null) {
            return ServerResponse.createByErrorCodeMassage(ResposeCode.NEED_LOGIN.getCode(), "用户未登录，请先登录");
        }
        //判断管理员权限
        if (userService.checkAdminRole(tbUser).isSuccess()) {
            return produceService.manageProductDetail(productId);
        } else {
            return ServerResponse.createByErrorMassage("无权限");
        }
    }


    /**
     * 查询产品List
     *
     * @param session
     * @return
     */
    @PostMapping("/getList")
    public ServerResponse getList(HttpSession session, @RequestParam(value = "page", defaultValue = "1") int page, @RequestParam(value = "size", defaultValue = "10") int size) {
        TbUser tbUser = (TbUser) session.getAttribute(Const.CURRENT_USER);
        if (tbUser == null) {
            return ServerResponse.createByErrorCodeMassage(ResposeCode.NEED_LOGIN.getCode(), "用户未登录，请先登录");
        }
        //判断管理员权限
        if (userService.checkAdminRole(tbUser).isSuccess()) {

            return produceService.getProductList(page, size);
        } else {
            return ServerResponse.createByErrorMassage("无权限");
        }
    }

    /**
     * 搜索商品
     *
     * @param session
     * @param productName
     * @param productId
     * @param page
     * @param size
     * @return
     */
    public ServerResponse searchProduct(HttpSession session, String productName, Integer productId, @RequestParam(value = "page", defaultValue = "1") int page, @RequestParam(value = "size", defaultValue = "10") int size) {
        TbUser tbUser = (TbUser) session.getAttribute(Const.CURRENT_USER);

        if (tbUser == null) {
            return ServerResponse.createByErrorCodeMassage(ResposeCode.NEED_LOGIN.getCode(), "用户未登录，请先登录");
        }

        //判断管理员权限
        if (userService.checkAdminRole(tbUser).isSuccess()) {
           return produceService.searchProduct(productName, productId, page, size);
        } else {
            return ServerResponse.createByErrorMassage("无权限");
        }
    }


    @PostMapping("/upLoad")
    public ServerResponse upLoad(HttpSession session, @RequestParam(value = "upload_file",required = false) MultipartFile file, HttpServletRequest request){

        TbUser tbUser = (TbUser) session.getAttribute(Const.CURRENT_USER);

        if (tbUser==null){
            return ServerResponse.createByErrorCodeMassage(ResposeCode.ILLEGAL_ARGUMENT.getCode(),ResposeCode.ILLEGAL_ARGUMENT.getDesc());
        }

        if (userService.checkAdminRole(tbUser).isSuccess()) {
            //相对路径
            String path = request.getSession().getServletContext().getRealPath("upload");
            String targetFile = fileService.upload(file, path);
            String url = PropertiesUtil.getProperty("ftb") + targetFile;

            Map fileMap = Maps.newHashMap();
            fileMap.put("uri", targetFile);
            fileMap.put("url", url);
            return ServerResponse.createBySuccess(fileMap);
        }
        return ServerResponse.createByErrorMassage("无权限");
    }

    //副文本
    @PostMapping("/richtextupLoad")
    public Map richtextupLoad(HttpSession session, @RequestParam(value = "upload_file",required = false) MultipartFile file, HttpServletRequest request, HttpServletResponse response){

        HashMap hashMap = new HashMap();
        TbUser tbUser = (TbUser) session.getAttribute(Const.CURRENT_USER);

        if (tbUser==null){
           hashMap.put("success",false);
           hashMap.put("msg","请登录管理员");
           return hashMap;
        }

        if (userService.checkAdminRole(tbUser).isSuccess()) {
            //相对路径
            String path = request.getSession().getServletContext().getRealPath("upload");
            String targetFile = fileService.upload(file, path);
            if (StringUtils.isBlank(targetFile)){
                hashMap.put("success",false);
                hashMap.put("msg","上传失败");
                return hashMap;
            }
            String url = PropertiesUtil.getProperty("ftb") + targetFile;
            hashMap.put("success",false);
            hashMap.put("msg","上传失败");
            hashMap.put("file_path",url);

            response.addHeader("Access-Control-Allow-Headers","X-File-Name");
            return hashMap;

        }else {
            hashMap.put("success", false);
            hashMap.put("msg", "无操作权限");
            return hashMap;
        }
    }



}
