package com.changgou.dao;

import com.changgou.goods.pojo.Album;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.common.Mapper;

@Component
public interface AlbumMapper extends Mapper<Album> {
}
