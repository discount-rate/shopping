package com.smart.app.controller.backend;

import com.github.pagehelper.PageInfo;
import com.smart.app.common.Const;
import com.smart.app.common.ResposeCode;
import com.smart.app.common.ServerResponse;
import com.smart.app.dao.TbUserMapper;
import com.smart.app.entity.TbUser;
import com.smart.app.service.OrderService;
import com.smart.app.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/manage")
public class OrderManageController {


    @Resource
    UserService userService;
    @Resource
    OrderService orderService;


    @PostMapping("/managelist")
    public ServerResponse managelist(HttpSession session, @RequestParam(value = "page",defaultValue = "1")int page, @RequestParam(value = "size",defaultValue = "10")int size){
        TbUser tbUser = (TbUser) session.getAttribute(Const.CURRENT_USER);
        if (tbUser == null){
            return ServerResponse.createByErrorCodeMassage(ResposeCode.NEED_LOGIN.getCode(),ResposeCode.NEED_LOGIN.getDesc());
        }

        if (userService.checkAdminRole(tbUser).isSuccess()){
            return orderService.managelist(page, size);
        }else {
            return ServerResponse.createByErrorMassage("无权限操作");
        }
    }

    @PostMapping("/managedetail")
    public ServerResponse managedetail(HttpSession session, Long orderNo){
        TbUser tbUser = (TbUser) session.getAttribute(Const.CURRENT_USER);
        if (tbUser == null){
            return ServerResponse.createByErrorCodeMassage(ResposeCode.NEED_LOGIN.getCode(),ResposeCode.NEED_LOGIN.getDesc());
        }

        if (userService.checkAdminRole(tbUser).isSuccess()){
            return orderService.manageDetail(orderNo);
        }else {
            return ServerResponse.createByErrorMassage("无权限操作");
        }
    }


    //按订单号精确搜索
    @PostMapping("/search")
    public ServerResponse<PageInfo> search(HttpSession session, Long orderNo, @RequestParam(value = "page",defaultValue = "1")int page, @RequestParam(value = "size",defaultValue = "10")int size){
        TbUser tbUser = (TbUser) session.getAttribute(Const.CURRENT_USER);
        if (tbUser == null){
            return ServerResponse.createByErrorCodeMassage(ResposeCode.NEED_LOGIN.getCode(),ResposeCode.NEED_LOGIN.getDesc());
        }

        if (userService.checkAdminRole(tbUser).isSuccess()){
            return orderService.search(orderNo,page,size);
        }else {
            return ServerResponse.createByErrorMassage("无权限操作");
        }
    }



    //发货
    @PostMapping("/sendGoods")
    public ServerResponse sendGoods(HttpSession session, Long orderNo){
        TbUser tbUser = (TbUser) session.getAttribute(Const.CURRENT_USER);
        if (tbUser == null){
            return ServerResponse.createByErrorCodeMassage(ResposeCode.NEED_LOGIN.getCode(),ResposeCode.NEED_LOGIN.getDesc());
        }

        if (userService.checkAdminRole(tbUser).isSuccess()){
            return orderService.sendGoods(orderNo);
        }else {
            return ServerResponse.createByErrorMassage("无权限操作");
        }
    }



}
