package com.smart.app.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.smart.app.common.Const;
import com.smart.app.common.ResposeCode;
import com.smart.app.common.ServerResponse;
import com.smart.app.dao.TbCategoryMapper;
import com.smart.app.dao.TbProductMapper;
import com.smart.app.entity.TbCategory;
import com.smart.app.entity.TbProduct;
import com.smart.app.service.CategoryService;
import com.smart.app.service.ProduceService;
import com.smart.app.util.DateTimeUtil;
import com.smart.app.util.PropertiesUtil;
import com.smart.app.vo.ProductDetailVo;
import com.smart.app.vo.ProductListVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductServiceImpl implements ProduceService {

    @Resource
    TbProductMapper productMapper;
    @Resource
    TbCategoryMapper categoryMapper;
    @Resource
    CategoryService categoryService;

    /**
     * 保持或更新商品
     * @param product
     * @return
     */
    public ServerResponse saveOrUpdateProduct(TbProduct product) {
        if (product != null) {

            //将子图赋值给主图
            if (StringUtils.isNoneBlank(product.getSubImages())) {
                String[] subImageArray = product.getSubImages().split(",");
                if (subImageArray.length > 0) {
                    product.setMainImage(subImageArray[0]);
                }
            }
            if (product.getId() != null) {
                int count = productMapper.updateByPrimaryKey(product);
                if (count > 0) {
                    return ServerResponse.createBySuccess("更新产品成功");
                }
                return ServerResponse.createByErrorMassage("更新产品失败");
            }
        } else {
            int insertCount = productMapper.insert(product);
            if (insertCount > 0) {
                return ServerResponse.createBySuccess("增加产品成功");
            }
            return ServerResponse.createByErrorMassage("增加产品失败");
        }
        return ServerResponse.createByErrorMassage("新增或更新产品失败");
    }

    /**
     * 跟新产品上下架
     * @param productId
     * @param status
     * @return
     */
    public ServerResponse<String> setSaleStatus(Integer productId,Integer status){
        if (productId==null || status==null){
            return ServerResponse.createByErrorCodeMassage(ResposeCode.ILLEGAL_ARGUMENT.getCode(),ResposeCode.ILLEGAL_ARGUMENT.getDesc());
        }
        TbProduct product = new TbProduct();
        product.setId(productId);
        product.setStatus(status);

        int i = productMapper.updateByPrimaryKeySelective(product);
        if (i>0){
            return ServerResponse.createBySuccess("修改商品销售状态成功");
        }
        return ServerResponse.createByErrorMassage("修改商品销售状态失败");
    }


    /**
     * 查询商品详细信息
     * @param productId
     * @return
     */
    public ServerResponse<ProductDetailVo> manageProductDetail(Integer productId){
        if (productId == null){
            return ServerResponse.createByErrorCodeMassage(ResposeCode.ILLEGAL_ARGUMENT.getCode(),ResposeCode.ILLEGAL_ARGUMENT.getDesc());
        }

        TbProduct product = productMapper.selectByPrimaryKey(productId);
        if (product==null){
            return ServerResponse.createByErrorMassage("商品不存在或已下架");
        }
       // BeanUtils.copyProperties(TbProduct,productDetailVo);

        ProductDetailVo productDetailVo = assembleProductDetailVo(product);
        return ServerResponse.createBySuccess(productDetailVo);
    }
    //product转换ProductDetailVo
    private ProductDetailVo assembleProductDetailVo(TbProduct product){
        ProductDetailVo productDetailVo = new ProductDetailVo();
        productDetailVo.setId(product.getId());
        productDetailVo.setSubtitle(product.getSubtitle());
        productDetailVo.setPrice(product.getPrice());
        productDetailVo.setMainImage(product.getMainImage());
        productDetailVo.setSubImages(product.getSubImages());
        productDetailVo.setCategoryId(product.getCategoryId());
        productDetailVo.setDetail(product.getDetail());
        productDetailVo.setName(product.getName());
        productDetailVo.setStatus(product.getStatus());
        productDetailVo.setStock(product.getStock());

        productDetailVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix","http://img.happymmall.com/"));

        TbCategory category = categoryMapper.selectByPrimaryKey(product.getCategoryId());
        if(category == null){
            productDetailVo.setParentCategoryId(0);//默认根节点
        }else{
            productDetailVo.setParentCategoryId(category.getParentId());
        }

        productDetailVo.setCreateTime(DateTimeUtil.dateToStr(product.getCreateTime()));
        productDetailVo.setUpdateTime(DateTimeUtil.dateToStr(product.getUpdateTime()));
        return productDetailVo;
    }


    /**
     *查询产品Lisst
     * @param page
     * @param size
     * @return
     */
    public ServerResponse<PageInfo> getProductList(int page,int size){
        PageHelper.startPage(page,size);
        List<TbProduct> tbProducts = productMapper.selectProductList();

        List<ProductListVo> productListVos = new ArrayList();
        for (TbProduct tbProduct : tbProducts) {
            ProductListVo productListVo = assembleProductListVo(tbProduct);
            productListVos.add(productListVo);
        }
        //自动分页处理
        PageInfo pageInfo = new PageInfo(tbProducts);
        pageInfo.setList(productListVos);
        return ServerResponse.createBySuccess(pageInfo);
    }

    private ProductListVo assembleProductListVo(TbProduct product){
        ProductListVo productListVo = new ProductListVo();
        productListVo.setId(product.getId());
        productListVo.setName(product.getName());
        productListVo.setCategoryId(product.getCategoryId());
        productListVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix","http://img.happymmall.com/"));
        productListVo.setMainImage(product.getMainImage());
        productListVo.setPrice(product.getPrice());
        productListVo.setSubtitle(product.getSubtitle());
        productListVo.setStatus(product.getStatus());
        return productListVo;
    }

    /**
     * 搜索商品
     * @param productName
     * @param productId
     * @param page
     * @param size
     * @return
     */
    public ServerResponse<PageInfo> searchProduct(String productName,Integer productId,int page,int size){

        PageHelper.startPage(page,size);

        if (StringUtils.isNoneBlank(productName)){
            productName = new StringBuilder().append("%").append(productName).append("%").toString();
        }
        List<TbProduct> productList = productMapper.selectByNameAndProductId(productName,productId);
        List<ProductListVo> productListVoList = Lists.newArrayList();

        for (TbProduct product : productList) {
            ProductListVo productListVo = assembleProductListVo(product);
            productListVoList.add(productListVo);
        }
        PageInfo pageInfo = new PageInfo(productList);
        pageInfo.setList(productListVoList);
        return ServerResponse.createBySuccess(pageInfo);
    }


    /**
     * 查询商品详情
     * @param productId
     * @return
     */
    public ServerResponse<ProductDetailVo> getProductDetail(Integer productId){

        if (productId==null){
            return ServerResponse.createByErrorCodeMassage(ResposeCode.ILLEGAL_ARGUMENT.getCode(),ResposeCode.ILLEGAL_ARGUMENT.getDesc());
        }

        TbProduct product = productMapper.selectByPrimaryKey(productId);
        if (product==null){
            return ServerResponse.createByErrorMassage("商品不存在或已下架");
        }
        if (product.getStatus() != Const.ProductStatusEnum.ON_SALE.getCode()){
            return ServerResponse.createByErrorMassage("商品不存在或已下架");
        }
        ProductDetailVo productDetailVo = assembleProductDetailVo(product);
        return ServerResponse.createBySuccess(productDetailVo);
    }


    /**
     * 分页查询
     * @param keyword
     * @param categoryId
     * @param page
     * @param size
     * @param orderBy
     * @return
     */
    public ServerResponse<PageInfo> getProductList(String keyword,Integer categoryId,int page,int size,String orderBy) {

        if (StringUtils.isBlank(keyword) && categoryId == null) {
            return ServerResponse.createByErrorCodeMassage(ResposeCode.ILLEGAL_ARGUMENT.getCode(), ResposeCode.ILLEGAL_ARGUMENT.getDesc());
        }
       List<Integer> categoryIdList = new ArrayList<>();

        if (categoryId != null) {
            TbCategory tbCategory = categoryMapper.selectByPrimaryKey(categoryId);
            if (tbCategory == null && StringUtils.isBlank(keyword)) {
                //如果为空返回一个空的结果集
                PageHelper.startPage(page, size);

                List<ProductListVo> productListVos = Lists.newArrayList();

                PageInfo pageInfo = new PageInfo(productListVos);
                return ServerResponse.createBySuccess(pageInfo);
            }
            categoryIdList = categoryService.selectCateoryAanChildrenById(tbCategory.getId()).getData();
        }

        if (StringUtils.isNoneBlank(keyword)) {
            keyword = new StringBuilder().append("%").append(keyword).append("&").toString();
        }
        PageHelper.startPage(page, size);
        //排序处理
        if (StringUtils.isNoneBlank(orderBy)) {
            if (Const.ProductListOrderBy.PRICE_ASC_DESC.contains(orderBy)) {
                String[] orderByArray = orderBy.split("_");
                PageHelper.orderBy(orderByArray[0] + " " + orderByArray[1]);
            }
        }
        List<TbProduct> products = productMapper.selectByNameAndCategory(StringUtils.isBlank(keyword) ? null : keyword, categoryIdList.size() == 0 ? null : categoryIdList);

        List<ProductListVo> productListVoList = Lists.newArrayList();
        for (TbProduct tbProduct : products) {
            ProductListVo productListVo = assembleProductListVo(tbProduct);
            productListVoList.add(productListVo);
        }
        PageInfo pageInfo = new PageInfo(products);
        pageInfo.setList(productListVoList);
        return ServerResponse.createBySuccess(pageInfo);
    }



}