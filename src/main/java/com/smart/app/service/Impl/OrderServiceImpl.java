package com.smart.app.service.Impl;

import com.alipay.api.AlipayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.alipay.demo.trade.utils.ZxingUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.smart.app.common.Const;
import com.smart.app.common.ServerResponse;
import com.smart.app.dao.*;
import com.smart.app.entity.*;
import com.smart.app.service.OrderService;
import com.smart.app.util.BigDecimalUtil;
import com.smart.app.util.DateTimeUtil;
import com.smart.app.util.FTPUtil;
import com.smart.app.util.PropertiesUtil;
import com.smart.app.vo.AddressVo;
import com.smart.app.vo.OrderItemVo;
import com.smart.app.vo.OrderProductVo;
import com.smart.app.vo.OrderVo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.Permission;
import java.util.*;

public class OrderServiceImpl implements OrderService {

    private  static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);
    @Resource
    TbOrderMapper orderMapper;
    @Resource
    TbOrderitemMapper orderitemMapper;
    @Resource
    TbPayMapper payMapper;
    @Resource
    TbCartMapper cartMapper;
    @Resource
    TbProductMapper productMapper;
    @Resource
    TbAddressMapper addressMapper;

    public ServerResponse pay(Long orderNo,Integer userId,String path){
       //把订单号和二维码返回前端 承载
        Map<String, String> resultMap = Maps.newHashMap();
        TbOrder tbOrder = orderMapper.selectByUserIdOrderNo(userId, orderNo);
        if (tbOrder==null){
            return ServerResponse.createByErrorMassage("用户没有该订单");
        }
        resultMap.put("orderNo",String.valueOf(tbOrder.getOrderNo()));






        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成，
        String outTradeNo = tbOrder.getOrderNo().toString();

        // (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
        String subject = "xxx品牌xxx门店当面付扫码消费";

        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        String totalAmount = tbOrder.getPayment().toString();

        // (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
        // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
        String undiscountableAmount = "0";

        // 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
        // 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
        String sellerId = "";

        // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
        String body =  new StringBuilder().append("订单").append(outTradeNo).append("购买商品供").append(totalAmount).append("元").toString();

        // 商户操作员编号，添加此参数可以为商户操作员做销售统计
        String operatorId = "test_operator_id";

        // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String storeId = "test_store_id";

        // 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088100200300400500");

        // 支付超时，定义为120分钟
        String timeoutExpress = "120m";

        // 商品明细列表，需填写购买商品详细信息，
        List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();

        List<TbOrderitem> orderItems = orderitemMapper.getByOrderNoUserId(orderNo,userId);
        for (TbOrderitem orderItem : orderItems) {
            GoodsDetail goods1 = GoodsDetail.newInstance(orderItem.getProductId().toString(), orderItem.getProductName(),
            BigDecimalUtil.mul(orderItem.getCurrentPrice().doubleValue(),new Double(100).doubleValue()).longValue(),orderItem.getQuantity());

            goodsDetailList.add(goods1);

        }

        // 创建扫码支付请求builder，设置请求参数
        AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder()
                .setSubject(subject).setTotalAmount(totalAmount).setOutTradeNo(outTradeNo)
                .setUndiscountableAmount(undiscountableAmount).setSellerId(sellerId).setBody(body)
                .setOperatorId(operatorId).setStoreId(storeId).setExtendParams(extendParams)
                .setTimeoutExpress(timeoutExpress)
               .setNotifyUrl(PropertiesUtil.getProperty("alipay.callback.url"))//支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置
                .setGoodsDetailList(goodsDetailList);


        /** 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
         *  Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
         */
        Configs.init("zfbinfo.properties");

        /** 使用Configs提供的默认参数
         *  AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new
         */
        AlipayTradeService tradeService = new AlipayTradeServiceImpl.ClientBuilder().build();


        AlipayF2FPrecreateResult result = tradeService.tradePrecreate(builder);
        switch (result.getTradeStatus()) {
            case SUCCESS:
                logger.info("支付宝预下单成功: )");

                AlipayTradePrecreateResponse response = result.getResponse();
                dumpResponse(response);

                File folder = new File(path);
                //如果不存在创建目录
                if (!folder.exists()){
                    folder.setWritable(true);
                    folder.mkdirs();
                }



                // 需要修改为运行机器上的路径
                //上传二维码
                String qrPath = String.format(path +"/qr-%s.png",
                        response.getOutTradeNo());
                //二维码path
                String qrFileName = String.format("qr-%s.png",response.getOutTradeNo());
                ZxingUtils.getQRCodeImge(response.getQrCode(),256,qrPath);

                File targetFile = new File(path,qrFileName);

                try {
                    FTPUtil.uploadFile(Lists.<File>newArrayList(targetFile));
                } catch (IOException e) {
                    logger.error("上传二维码异常",e);
                }

                logger.info("filePath:" + qrPath);
                String qrurl = PropertiesUtil.getProperty("ftb")+targetFile.getName();

                resultMap.put("qrurl",qrurl);
                return ServerResponse.createBySuccess(resultMap);

                //                ZxingUtils.getQRCodeImge(response.getQrCode(), 256, filePath);

            case FAILED:
                logger.error("支付宝预下单失败!!!");
                return ServerResponse.createByErrorMassage("支付宝预下单失败!!!");

            case UNKNOWN:
                logger.error("系统异常，预下单状态未知!!!");
                return ServerResponse.createByErrorMassage("系统异常，预下单状态未知!!!");

            default:
                logger.error("不支持的交易状态，交易返回异常!!!");
                return ServerResponse.createByErrorMassage("不支持的交易状态，交易返回异常!!!");
        }

    }


    // 简单打印应答
    private void dumpResponse(AlipayResponse response) {
        if (response != null) {
            logger.info(String.format("code:%s, msg:%s", response.getCode(), response.getMsg()));
            if (StringUtils.isNotEmpty(response.getSubCode())) {
                logger.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(),
                        response.getSubMsg()));
            }
            logger.info("body:" + response.getBody());
        }
    }

    public ServerResponse aliCallback(Map<String,String> params){
        Long orderNo = Long.parseLong(params.get("out_trade_no"));
        String tradeNo = params.get("trade-no");
        String tradeStatus = params.get("trade_status");

        TbOrder tbOrder = orderMapper.selectByOrderNo(orderNo);
        if (tbOrder==null){
            return ServerResponse.createByErrorMassage("会调失败");
        }
        if (tbOrder.getStatus() >= Const.OrderStatusEnum.PAID.getCode()){
            return ServerResponse.createByErrorMassage("支付宝重复调用000");
        }
        if (Const.AlipayCallback.TRADE_STATUS_TRADE_SUCCESS.equals(tradeStatus)){
            tbOrder.setPaymentTime(DateTimeUtil.strTODate(params.get("get_payment")));
            tbOrder.setStatus(Const.OrderStatusEnum.PAID.getCode());
            orderMapper.updateByPrimaryKeySelective(tbOrder);
        }


        TbPay tbPay = new TbPay();
        tbPay.setUserId(tbOrder.getUserId());
        tbPay.setOrderNo(tbOrder.getOrderNo());
        tbPay.setPayPlatform(Const.PayPlatformEnum.ALIPAY.getCode());
        tbPay.setPlatformNumber(tradeNo);
        tbPay.setPlatformStatus(tradeStatus);

        payMapper.insert(tbPay);
        return ServerResponse.createBySuccess();


    }

    public ServerResponse queryPay(Integer userId,Long orderNo) {
        TbOrder tbOrder = orderMapper.selectByUserIdOrderNo(userId, orderNo);
        if (tbOrder == null) {
            return ServerResponse.createByErrorMassage("用户没有该订单");
        }
        //判断支付状态
        if (tbOrder.getStatus() >= Const.OrderStatusEnum.PAID.getCode()) {
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }








    public ServerResponse createOrder(Integer userId,Integer addressId){
        // 从购物车中获取数据
        List<TbCart> cartList = cartMapper.selectCheckedCartByUserId(userId);
       //计算这个订单总价
        ServerResponse serverResponse = this.getCartOrderItem(userId, cartList);
        if (!serverResponse.isSuccess()){
            return serverResponse;
        }
        List<TbOrderitem> tbOrderitems = (List<TbOrderitem>)serverResponse.getData();
       BigDecimal payment = this.getOrderTotalPrice(tbOrderitems);

       //生成订单
        TbOrder order = this.assembleOrder(userId,addressId,payment);
        if (order==null){
            return ServerResponse.createByErrorMassage("生成订单错误");
        }

        if (CollectionUtils.isEmpty(tbOrderitems)){
            return ServerResponse.createByErrorMassage("购物车为空");
        }

        for (TbOrderitem tbOrderitem : tbOrderitems) {
            tbOrderitem.setOrderNo(order.getOrderNo());
        }
        //批量插入
        orderitemMapper.batchInsert(tbOrderitems);
        //减库存
        this.reduceProductStock(tbOrderitems);
        //清空购物车
        this.cleanCart(cartList);
        //返回给前端数据
        OrderVo orderVo = assembleOrderVo(order, tbOrderitems);
        return ServerResponse.createBySuccess(orderVo);

    }

    private OrderVo assembleOrderVo(TbOrder order,List<TbOrderitem> orderitemList){

        OrderVo orderVo = new OrderVo();
        orderVo.setOrderNo(order.getOrderNo());
        orderVo.setPayment(order.getPayment());
        order.setPaymentType(order.getPaymentType());

        orderVo.setPostage(order.getPostage());
        orderVo.setStatus(order.getStatus());
        orderVo.setStatusDesc(Const.OrderStatusEnum.codeOf(order.getStatus()).getValue());

        orderVo.setAddressId(order.getAddressId());
        TbAddress tbAddress = addressMapper.selectByPrimaryKey(order.getAddressId());
        if (tbAddress!=null){
            orderVo.setReceiverName(tbAddress.getReceiverName());
            orderVo.setAddressVo(assembleAddressVo(tbAddress));
        }

        orderVo.setPaymentTime(DateTimeUtil.dateToStr(order.getPaymentTime()));
        orderVo.setSendTime(DateTimeUtil.dateToStr(order.getSendTime()));
        orderVo.setEndTime(DateTimeUtil.dateToStr(order.getEndTime()));
        orderVo.setCloseTime(DateTimeUtil.dateToStr(order.getCloseTime()));
        orderVo.setCloseTime(DateTimeUtil.dateToStr(order.getCloseTime()));

        orderVo.setImageHost(PropertiesUtil.getProperty("ftb"));

        List<OrderItemVo> orderItemVos = Lists.newArrayList();
        for (TbOrderitem tbOrderitem : orderitemList) {

            OrderItemVo orderItemVo = assembOrderItemVo(tbOrderitem);
            orderItemVos.add(orderItemVo);
        }
        orderVo.setOrderItemVoList(orderItemVos);
        return orderVo;
    }




    private OrderItemVo assembOrderItemVo(TbOrderitem tbOrderitem){
        OrderItemVo orderItemVo = new OrderItemVo();
        orderItemVo.setOrderNo(tbOrderitem.getOrderNo());
        orderItemVo.setProductId(tbOrderitem.getProductId());
        orderItemVo.setProductName(tbOrderitem.getProductName());
        orderItemVo.setProductImage(tbOrderitem.getProductImage());
        orderItemVo.setCurrentUnitPrice(tbOrderitem.getCurrentPrice());
        orderItemVo.setQuantity(tbOrderitem.getQuantity());
        orderItemVo.setTotalPrice(tbOrderitem.getTotalPrice());
        orderItemVo.setCreateTime(DateTimeUtil.dateToStr(tbOrderitem.getCreateTime()));
        return orderItemVo;
    }


    private AddressVo assembleAddressVo(TbAddress tbAddress){
        AddressVo addressVo = new AddressVo();
        addressVo.setReceiverName(tbAddress.getReceiverName());
        addressVo.setReceiverAddress(tbAddress.getReceiverAddress());
        addressVo.setReceiverProvince(tbAddress.getReceiverProvince());
        addressVo.setReceiverCity(tbAddress.getReceiverCity());
        addressVo.setReceiverDistrict(tbAddress.getReceiverDistrict());
        addressVo.setReceiverMobile(tbAddress.getReceiverMobile());
        addressVo.setReceiverPhone(tbAddress.getReceiverPhone());
        return addressVo;
    }



    //清空购物车
    private void cleanCart(List<TbCart> cartList){
        for (TbCart cart : cartList) {
            cartMapper.deleteByPrimaryKey(cart.getId());
        }
    }



    //减库存
    private void reduceProductStock(List<TbOrderitem> tbOrderitems){
        for (TbOrderitem tbOrderitem : tbOrderitems) {
            TbProduct product = productMapper.selectByPrimaryKey(tbOrderitem.getProductId());
            product.setStock(product.getStock()-tbOrderitem.getQuantity());
            productMapper.updateByPrimaryKeySelective(product);
        }
    }



    private TbOrder assembleOrder(Integer userId,Integer addressId, BigDecimal payment){
        TbOrder order = new TbOrder();
        long orderNo = this.generatOrderNo();
        order.setOrderNo(orderNo);
        order.setStatus(Const.OrderStatusEnum.NO_PAY.getCode());
        order.setPostage(0);
        order.setPaymentType(Const.PaymentTypeEnum.ONLINE_PAY.getCode());
        order.setPayment(payment);
        order.setUserId(userId);
        order.setAddressIdId(addressId);

        int insert = orderMapper.insert(order);
        if (insert>0){
            return order;
        }
        return null;
    }


    private long generatOrderNo(){
        //订单号生成规则
        long currentTime = System.currentTimeMillis();
        return currentTime+new Random().nextInt(100);
    }


    // 生成订单价格
    private BigDecimal getOrderTotalPrice(List<TbOrderitem> tbOrderitems ){
        BigDecimal payment = new BigDecimal(0);
        for (TbOrderitem tbOrderitem : tbOrderitems) {
            payment = BigDecimalUtil.add(payment.doubleValue(),tbOrderitem.getTotalPrice().doubleValue());
        }
        return payment;
    }


    public ServerResponse<List<TbOrderitem>> getCartOrderItem(Integer userId,List<TbCart> cartList){
        List<TbOrderitem> orderitems = Lists.newArrayList();
        if (CollectionUtils.isEmpty(cartList)){
            return ServerResponse.createByErrorMassage("购物车为空");
        }

        //校验购物车数据，包括产品的状态和数量
        for (TbCart cart : cartList) {
            TbOrderitem tbOrderitem = new TbOrderitem();
            TbProduct product = productMapper.selectByPrimaryKey(cart.getProductId());
            if (Const.ProductStatusEnum.ON_SALE.getCode() != product.getStatus()){
                return ServerResponse.createByErrorMassage("产品"+product.getName()+"产品不在售卖状态");
            }
            if (cart.getQuantity()>product.getStock()){
                return ServerResponse.createByErrorMassage("产品"+product.getName()+"产品库存不足");
            }

            tbOrderitem.setUserId(userId);
            tbOrderitem.setProductId(product.getId());
            tbOrderitem.setProductName(product.getName());
            tbOrderitem.setProductImage(product.getMainImage());
            tbOrderitem.setCurrentPrice(product.getPrice());
            tbOrderitem.setQuantity(cart.getQuantity());
            tbOrderitem.setTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(),cart.getQuantity()));
            orderitems.add(tbOrderitem);
        }
        return ServerResponse.createBySuccess(orderitems);
    }



    public ServerResponse<String> cancel(Integer userId,Long orderNo){
        TbOrder order = orderMapper.selectByUserIdOrderNo(userId, orderNo);
        if (order==null){
            return ServerResponse.createByErrorMassage("订单不存在");
        }
        if (order.getStatus()!= Const.OrderStatusEnum.NO_PAY.getCode()){
            return  ServerResponse.createByErrorMassage("已付款");
        }
        TbOrder updateOrder = new TbOrder();
        updateOrder.setId(order.getId());
        updateOrder.setStatus(order.getStatus());

        int i = orderMapper.updateByPrimaryKeySelective(updateOrder);
        if (i>0){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();

    }


    public ServerResponse getOrderCartProduct(Integer userId){
        OrderProductVo orderProductVo = new OrderProductVo();

        List<TbCart> tbCarts = cartMapper.selectCheckedCartByUserId(userId);
        ServerResponse serverResponse = this.getCartOrderItem(userId, tbCarts);
        if (!serverResponse.isSuccess()){
            return serverResponse;
        }
        List<TbOrderitem> orderitemList = (List<TbOrderitem>) serverResponse.getData();
        List<OrderItemVo> orderItemVos = Lists.newArrayList();

        BigDecimal payment = new BigDecimal(0);
        for (TbOrderitem tbOrderitem : orderitemList) {
           payment = BigDecimalUtil.add(payment.doubleValue(),tbOrderitem.getTotalPrice().doubleValue());
            orderItemVos.add(assembOrderItemVo(tbOrderitem));
        }
        orderProductVo.setProductTotalPrice(payment);
        orderProductVo.setImageHost("ftb");
        orderProductVo.setOrderItemVoList(orderItemVos);

        return ServerResponse.createBySuccess(orderProductVo);
    }



    public  ServerResponse<OrderVo> getOrderDetail(Integer userId,Long orderNo){
       TbOrder tbOrder = orderMapper.selectByUserIdOrderNo(userId, orderNo);
       if (tbOrder!=null){
           List<TbOrderitem> byOrderNoUserId = orderitemMapper.getByOrderNoUserId(orderNo, userId);
           OrderVo orderVo = assembleOrderVo(tbOrder, byOrderNoUserId);
           return ServerResponse.createBySuccess(orderVo);
       }
       return ServerResponse.createByErrorMassage("没有找到该订单");
    }


    public ServerResponse<PageInfo> orderList(Integer userId,int page,int size){

        PageHelper.startPage(page,size);
        List<TbOrder> tbOrders = orderMapper.selectByUserId(userId);
        List<OrderVo> orderVos = assembleOrderVoList(tbOrders, userId);
        PageInfo pageInfo = new PageInfo(orderVos);
        pageInfo.setList(tbOrders);
        return ServerResponse.createBySuccess(pageInfo);
    }


    private List<OrderVo> assembleOrderVoList(List<TbOrder> orderList,Integer userId){
        List<OrderVo> orderVoList = Lists.newArrayList();
        for (TbOrder tbOrder : orderList) {
            List<TbOrderitem> orderitemList = Lists.newArrayList();
            if (userId==null){
                orderitemList = orderitemMapper.getByOrderNo(tbOrder.getOrderNo());

            }else {
                orderitemList = orderitemMapper.getByOrderNoUserId(tbOrder.getOrderNo(), userId);
            }
            OrderVo orderVo = assembleOrderVo(tbOrder, orderitemList);
            orderVoList.add(orderVo);
        }
        return orderVoList;
    }



    public ServerResponse<PageInfo> managelist(int page,int size){
        PageHelper.startPage(page,size);
        List<TbOrder> tbOrders = orderMapper.selectAll();
        List<OrderVo> orderVos = this.assembleOrderVoList(tbOrders, null);

        PageInfo pageInfo = new PageInfo(tbOrders);
        pageInfo.setList(orderVos);
        return ServerResponse.createBySuccess(pageInfo);
    }


    public ServerResponse<OrderVo> manageDetail(Long orderNo){
        TbOrder tbOrder = orderMapper.selectByOrderNo(orderNo);
        if (tbOrder!=null){
            List<TbOrderitem> byOrderNo = orderitemMapper.getByOrderNo(orderNo);
            OrderVo orderVo = assembleOrderVo(tbOrder, byOrderNo);
            return ServerResponse.createBySuccess(orderVo);
        }
        return ServerResponse.createByErrorMassage("订单不存在");

    }


    public ServerResponse search(Long orderNo,int page,int size){

        PageHelper.startPage(page,size);
        TbOrder tbOrder = orderMapper.selectByOrderNo(orderNo);
        if (tbOrder!=null){
            List<TbOrderitem> byOrderNo = orderitemMapper.getByOrderNo(orderNo);
            OrderVo orderVo = assembleOrderVo(tbOrder, byOrderNo);

            PageInfo pageInfo = new PageInfo(Lists.newArrayList(tbOrder));
            pageInfo.setList(Lists.newArrayList(orderVo));
            return ServerResponse.createBySuccess(pageInfo);
        }
        return ServerResponse.createByErrorMassage("订单不存在");
    }


    public ServerResponse<String> sendGoods(Long orderNo){
        TbOrder tbOrder = orderMapper.selectByOrderNo(orderNo);
        if (tbOrder.getStatus() == Const.OrderStatusEnum.PAID.getCode()){
            tbOrder.setStatus(Const.OrderStatusEnum.SHIPPED.getCode());
            tbOrder.setSendTime(new Date());
            orderMapper.updateByPrimaryKeySelective(tbOrder);
            return ServerResponse.createBySuccess("发货成功");
        }
        return ServerResponse.createByErrorMassage("订单不存在");

    }


}
