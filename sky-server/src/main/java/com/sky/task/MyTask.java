package com.sky.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class MyTask {


    //定时任务:每五秒触发一次
    //Scheduled注解的cron属性:用来指定定时任务的执行时间
//    @Scheduled(cron = "0/5 * * * * ?")
//    public void executeTask(){
//        log.info("定时任务执行了 :#{}",new Date());
//    }
}
