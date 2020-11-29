package entity;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 *
 * 抽取工具类 服务需要时自行加入验证
 */
public class FeignInterceptor implements RequestInterceptor {

    private Logger logger = LoggerFactory.getLogger(getClass());
    @Override
    public void apply(RequestTemplate requestTemplate) {
        try {
            //使用RequestContextHolder工具获取request相关变量
            //获取用户令牌

            //记录了当前用户请求的所有数据，包含请求头和请求参数
            //用户当前请求的时候对应线程的数据 如果开启了熔断，熔断默认隔离措施为线程池隔离，
            // 此时会开启新的线程用于feign调用，需在配置文件里设置熔断配置为信号量隔离
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                //取出request
                HttpServletRequest request = attributes.getRequest();
                //获取所有头文件信息的key
                Enumeration<String> headerNames = request.getHeaderNames();
                if (headerNames != null) {
                    while (headerNames.hasMoreElements()) {
                        //头文件的key
                        String name = headerNames.nextElement();
                        //头文件的value
                        String values = request.getHeader(name);
                        logger.info("{}:{}",name,values);
                        //将令牌数据添加到头文件中
                        requestTemplate.header(name, values);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
