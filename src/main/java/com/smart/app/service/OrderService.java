package com.smart.app.service;

import com.github.pagehelper.PageInfo;
import com.smart.app.common.ServerResponse;
import com.smart.app.vo.OrderVo;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public interface OrderService {

    ServerResponse pay(Long orderNo, Integer userId, String path);

    ServerResponse aliCallback(Map<String,String> params);

    ServerResponse queryPay(Integer userId,Long orderNo);

    ServerResponse createOrder(Integer userId,Integer addressId);

    ServerResponse<String> cancel(Integer userId,Long orderNo);

    ServerResponse getOrderCartProduct(Integer userId);

    ServerResponse<OrderVo> getOrderDetail(Integer userId, Long orderNo);

    ServerResponse<PageInfo> orderList(Integer userId, int page, int size);

    ServerResponse<PageInfo> managelist(int page,int size);

    ServerResponse<OrderVo> manageDetail(Long orderNo);

    ServerResponse search(Long orderNo,int page,int size);

    ServerResponse<String> sendGoods(Long orderNo);
}
