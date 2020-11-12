package com.changgou.service;

import com.github.pagehelper.PageInfo;

import java.util.List;

public interface BaseService<T> {
    /***
     * Para多条件分页查询
     * @param para
     * @param page
     * @param size
     * @return
     */
    PageInfo<T> findPage(T t, int page, int size);

    /***
     * Para分页查询
     * @param page
     * @param size
     * @return
     */
    PageInfo<T> findPage(int page, int size);

    /***
     * Para多条件搜索方法
     * @param para
     * @return
     */
    List<T> findList(T para);

    /***
     * 删除Para
     * @param id
     */
    void delete(Integer id);

    /***
     * 修改Para数据
     * @param para
     */
    void update(T para);

    /***
     * 新增Para
     * @param para
     */
    void add(T para);

    /**
     * 根据ID查询Para
     * @param id
     * @return
     */
    T findById(Integer id);

    /***
     * 查询所有Para
     * @return
     */
    List<T> findAll();
}
