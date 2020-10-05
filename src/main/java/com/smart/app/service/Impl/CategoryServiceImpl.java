package com.smart.app.service.Impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.smart.app.common.ServerResponse;
import com.smart.app.dao.TbCartMapper;
import com.smart.app.dao.TbCategoryMapper;
import com.smart.app.entity.TbCategory;
import com.smart.app.service.CategoryService;
import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

@Service
public class CategoryServiceImpl implements CategoryService {

    private Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class);
    @Resource
    TbCategoryMapper categoryMapper;

    /**
     * 添加商品类
     * @param categoryName
     * @param parentId
     * @return
     */
    public ServerResponse addCtegory(String categoryName,Integer parentId){

        if (parentId == null || StringUtils.isBlank(categoryName)){
           return ServerResponse.createByErrorMassage("添加品类参数错误");
        }
        TbCategory tbCategory = new TbCategory();
        tbCategory.setName(categoryName);
        tbCategory.setParentId(parentId);
        tbCategory.setStatus(1);

        int insert = categoryMapper.insert(tbCategory);

        if (insert > 0 ){
         return ServerResponse.createBySuccess("添加商品成功");
        }
        return ServerResponse.createBySuccessMessage("添加品类失败");
    }


    /**
     * 更新品类名
     * @param categoryId
     * @param categoryName
     * @return
     */
    public ServerResponse updateCategoryName(Integer categoryId,String categoryName){
        if (categoryId == null || StringUtils.isBlank(categoryName)){
            return ServerResponse.createByErrorMassage("添加商品参数错误");
        }

        TbCategory tbCategory = new TbCategory();
        tbCategory.setId(categoryId);
        tbCategory.setName(categoryName);

        int i = categoryMapper.updateByPrimaryKeySelective(tbCategory);
        if (i > 0){
            return ServerResponse.createBySuccess("跟新品类名字成功");
        }
            return ServerResponse.createByErrorMassage("跟新品类名字失败");
    }

    /**
     *根据categoryId获取categoryId节点的category信息
     */
    public  ServerResponse<List<TbCategory>> getChildrenParallCategory(Integer categoryId){

        List<TbCategory> categoryList = categoryMapper.selectCategoryChildrenByParentId(categoryId);

        if (CollectionUtils.isEmpty(categoryList)){
            logger.info("未找到当前分类的子类");
        }
        return ServerResponse.createBySuccess(categoryList);

    }

    /**
     * 递归查询本节点id和子节点的id
     * @param categoryId
     * @return
     */
    public ServerResponse<List<Integer>> selectCateoryAanChildrenById(Integer categoryId){

        //初始化
       Set<TbCategory> tbCategorySet =  Sets.newHashSet();
       findChildCategory(tbCategorySet,categoryId);

       List<Integer> tbcategoryList = Lists.newArrayList();
       if (categoryId!=null){
           for (TbCategory tbCategory : tbCategorySet) {
               tbcategoryList.add(tbCategory.getId());
           }
       }
       return ServerResponse.createBySuccess(tbcategoryList);
    }

    //递归算法，算出子节点
    private Set<TbCategory> findChildCategory(Set<TbCategory> tbCategorySet,Integer categoryId){
        TbCategory tbCategory = categoryMapper.selectByPrimaryKey(categoryId);
        if (tbCategory!= null){
            tbCategorySet.add(tbCategory);
        }
        //查找子节点，递归算法要有一个退出条件
        List<TbCategory> tbCategories = categoryMapper.selectCategoryChildrenByParentId(categoryId);

        for (TbCategory tbCategoryItem : tbCategories){
            findChildCategory(tbCategorySet,tbCategoryItem.getId());

        }
        return tbCategorySet;
    }
}
