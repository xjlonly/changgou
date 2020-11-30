package com.changgou.user.dao;
import com.changgou.user.pojo.OauthClientDetails;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.common.Mapper;

/****
 * @Author:shenkunlin
 * @Description:OauthClientDetails的Dao
 * @Date 2019/6/14 0:12
 *****/
@Component
public interface OauthClientDetailsMapper extends Mapper<OauthClientDetails> {
}
