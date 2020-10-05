package com.smart.app.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.demo.trade.config.Configs;
import com.google.common.collect.Maps;
import com.smart.app.common.Const;
import com.smart.app.common.ResposeCode;
import com.smart.app.common.ServerResponse;
import com.smart.app.entity.TbUser;
import com.smart.app.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Iterator;
import java.util.Map;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Resource
    OrderService orderService;

    Logger logger = LoggerFactory.getLogger(OrderController.class);

    @PostMapping("/pay")
    public ServerResponse pay(HttpSession session, Long orderNo, HttpServletRequest request){

        TbUser tbUser = (TbUser) session.getAttribute(Const.CURRENT_USER);
        if (tbUser==null){
            return ServerResponse.createByErrorCodeMassage(ResposeCode.NEED_LOGIN.getCode(),ResposeCode.NEED_LOGIN.getDesc());
        }
        String path = request.getSession().getServletContext().getRealPath("upload");

        return orderService.pay(orderNo,tbUser.getUserid(),path);
    }


    //回调接口
    @PostMapping("/alipayCallBack")
    public Object alipayCallBack(HttpServletRequest request){

        Map<String,String> params = Maps.newHashMap();

        Map parameterMap = request.getParameterMap();

        //使用迭代器获取元素
        for (Iterator iter = parameterMap.keySet().iterator();iter.hasNext();){

            //获取name和 值是数组
            String name = (String) iter.next();
            String[] values = (String[]) parameterMap.get(name);
            String valueStr = "";
            for (int i = 0;i < values.length; i++){

                valueStr = (i==values.length -1)?valueStr + values[i]:valueStr + values[i]+",";
            }
            params.put(name,valueStr);
        }

        logger.info("支付宝回调，sign：{}，trade_status：{}，参数：{}",params.get("sign"),params.get("trade_status"),params.toString());

        //验证回调的正确性，是不是支付宝发的，并且还要避免重复通知
        params.remove("sign_type");

        try {
            boolean alipayRSACheckedV2 = AlipaySignature.rsaCheckV2(params, Configs.getAlipayPublicKey(),"utf-8",Configs.getSignType());
            if (!alipayRSACheckedV2){
                return ServerResponse.createByErrorMassage("请求非法");
            }
        } catch (AlipayApiException e) {
            logger.error("支付宝验证回调异常",e);
        }

        ServerResponse serverResponse = orderService.aliCallback(params);

        if (serverResponse.isSuccess()){
            return Const.AlipayCallback.RESPONSE_SUCCESS;
        }
        return Const.AlipayCallback.RESPONSE_FAILED;
    }

    //前台轮询查询订单的支付状态，前台调用此接口查看是否付款成功，如果付款成功跳到订单

    @PostMapping("/queryPay")
    public ServerResponse<Boolean> queryPay(HttpSession session, Long orderNo){

        TbUser tbUser = (TbUser) session.getAttribute(Const.CURRENT_USER);
        if (tbUser==null){
            return ServerResponse.createByErrorCodeMassage(ResposeCode.NEED_LOGIN.getCode(),ResposeCode.NEED_LOGIN.getDesc());
        }

        ServerResponse serverResponse = orderService.queryPay(tbUser.getUserid(), orderNo);
        if (serverResponse.isSuccess()){
            return ServerResponse.createBySuccess(true);
        }
        return ServerResponse.createBySuccess(false);
    }









    @PostMapping("/create")
    public ServerResponse create(HttpSession session,Integer addressId){
        TbUser tbUser = (TbUser) session.getAttribute(Const.CURRENT_USER);
        if (tbUser == null){
            return ServerResponse.createByErrorCodeMassage(ResposeCode.NEED_LOGIN.getCode(),ResposeCode.NEED_LOGIN.getDesc());

        }
        return orderService.createOrder(tbUser.getUserid(),addressId);
    }

//未付款状态下取消
    @PostMapping("/cancel")
    public ServerResponse cancel(HttpSession session,Long orderNo){
        TbUser tbUser = (TbUser) session.getAttribute(Const.CURRENT_USER);
        if (tbUser == null){
            return ServerResponse.createByErrorCodeMassage(ResposeCode.NEED_LOGIN.getCode(),ResposeCode.NEED_LOGIN.getDesc());

        }
        return orderService.cancel(tbUser.getUserid(),orderNo);
    }


    //获取购物车中已选中的
    @PostMapping("/getCartProduct")
    public ServerResponse getCartProduct(HttpSession session){
        TbUser tbUser = (TbUser) session.getAttribute(Const.CURRENT_USER);
        if (tbUser == null){
            return ServerResponse.createByErrorCodeMassage(ResposeCode.NEED_LOGIN.getCode(),ResposeCode.NEED_LOGIN.getDesc());
        }
        return orderService.getOrderCartProduct(tbUser.getUserid());
    }


    //获取订单详情
    @PostMapping("/datail")
    public ServerResponse datail(HttpSession session,Long orderNo){
        TbUser tbUser = (TbUser) session.getAttribute(Const.CURRENT_USER);
        if (tbUser == null){
            return ServerResponse.createByErrorCodeMassage(ResposeCode.NEED_LOGIN.getCode(),ResposeCode.NEED_LOGIN.getDesc());
        }
        return orderService.getOrderDetail(tbUser.getUserid(),orderNo);
    }


    //查看订单list
    @PostMapping("/list")
    public ServerResponse list(HttpSession session, @RequestParam(value = "page",defaultValue = "1")int page,@RequestParam(value = "size",defaultValue = "10")int size){
        TbUser tbUser = (TbUser) session.getAttribute(Const.CURRENT_USER);
        if (tbUser == null){
            return ServerResponse.createByErrorCodeMassage(ResposeCode.NEED_LOGIN.getCode(),ResposeCode.NEED_LOGIN.getDesc());
        }
        return orderService.orderList(tbUser.getUserid(),page,size);
    }




}
