package com.changgou.service;

import com.changgou.goods.pojo.Category;

import java.util.List;

public interface CategoryService extends BaseService<Category>{
    /***
     * 根据父节点ID查询
     * @param pid:父节点ID
     */
    List<Category> findByParentId(Integer pid);
}
