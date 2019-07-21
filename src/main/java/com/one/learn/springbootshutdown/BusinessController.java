package com.one.learn.springbootshutdown;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author One
 * @Description
 * @date 2019/07/21
 */
@Slf4j
@RestController
public class BusinessController {

    @RequestMapping("/working")
    public String working() throws InterruptedException {
        log.warn("开始处理业务");
        Thread.sleep(10000);
        log.warn("结束处理业务");
        return "业务完成";
    }
}
