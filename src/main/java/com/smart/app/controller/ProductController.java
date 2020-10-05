package com.smart.app.controller;

import com.github.pagehelper.PageInfo;
import com.smart.app.common.ServerResponse;
import com.smart.app.service.ProduceService;
import com.smart.app.vo.ProductDetailVo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Resource
    ProduceService produceService;

    /**
     * 查看商品详情
     * @param productId
     * @return
     */
    @PostMapping("/detail")
    public ServerResponse<ProductDetailVo> detail(Integer productId){
       return produceService.getProductDetail(productId);

    }


    /**
     * 分页查询
     * @param keyword
     * @param categoryId
     * @param page
     * @param Size
     * @param orderBy
     * @return
     */
    @PostMapping("/list")
    public ServerResponse<PageInfo> list(@RequestParam(value = "keyword",required = false)String keyword,@RequestParam(value = "categoryId",required = false)Integer categoryId,
                                         @RequestParam(value = "page",defaultValue = "1") int page,
                                         @RequestParam(value = "Size",defaultValue = "10") int Size,
                                         @RequestParam(value = "orderBy",defaultValue = "") String orderBy
                                         ){

        return produceService.getProductList(keyword, categoryId, page, Size, orderBy);
    }
}
