package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务类：定时提取订单状态
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单的方法,每分钟执行一次
     */

    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder(){
        log.info("定时任务执行：处理超时订单 :{}", LocalDateTime.now());

        //select * from orders where status = ? and order_time < (now() - 15 minutes)
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, LocalDateTime.now().minusMinutes(15));

        if(ordersList != null && ordersList.size() > 0){
            for(Orders orders : ordersList){
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时自动取消");

                orders.setCancelTime(LocalDateTime.now());//设置当前订单的取消时间
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 每天凌晨一点触发一次,处理一直处于派送中的订单
     */
    @Scheduled(cron = "0 0 1 * * ?")//每天凌晨一点触发一次
    //@Scheduled(cron = "0/5 * * * * ?")//先让他五秒钟触发一次
    public void processDeliveryOrder(){
        log.info("定时处理派送中的订单 :{}", LocalDateTime.now());

        //时间为当前时间减去一个小时
        List<Orders> OrderList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now().minusHours(1));

        if(OrderList != null && OrderList.size() > 0){
            for(Orders orders : OrderList){
                orders.setStatus(Orders.COMPLETED);
                orders.setCancelReason("订单超时自动取消");

                orders.setCancelTime(LocalDateTime.now());//设置当前订单的取消时间
                orderMapper.update(orders);
            }
        }
    }
}
