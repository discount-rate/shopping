package com.smart.app.service;

import com.github.pagehelper.PageInfo;
import com.smart.app.common.ServerResponse;
import com.smart.app.entity.TbProduct;
import com.smart.app.vo.ProductDetailVo;

public interface ProduceService {

    ServerResponse saveOrUpdateProduct(TbProduct product);

    ServerResponse<String> setSaleStatus(Integer productId,Integer status);

    ServerResponse<ProductDetailVo> manageProductDetail(Integer productId);

    ServerResponse<PageInfo> getProductList(int page, int size);

    ServerResponse<PageInfo> searchProduct(String productName,Integer productId,int page,int size);

    ServerResponse<ProductDetailVo> getProductDetail(Integer productId);

    ServerResponse<PageInfo> getProductList(String keyword,Integer categoryId,int page,int size,String orderBy);
}
