package com.smart.app.service;

import com.smart.app.common.ServerResponse;
import com.smart.app.entity.TbCategory;

import java.util.List;

public interface CategoryService {


    ServerResponse addCtegory(String categoryName, Integer parentId);

    ServerResponse updateCategoryName(Integer categoryId,String categoryName);

    ServerResponse<List<TbCategory>> getChildrenParallCategory(Integer categoryId);

    ServerResponse<List<Integer>> selectCateoryAanChildrenById(Integer categoryId);

}
