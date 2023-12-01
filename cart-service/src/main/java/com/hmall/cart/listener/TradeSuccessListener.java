package com.hmall.cart.listener;

import com.hmall.cart.service.ICartService;
import com.hmall.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Slf4j
@Configuration
public class TradeSuccessListener {
    @Autowired
    private  ICartService cartService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "trade.queue"),
            exchange = @Exchange(name = "trade.topic",type = ExchangeTypes.TOPIC),
            key = "trade.success"
    ))
    public void tradeSuccess(Map map){
        Long userId =Long.valueOf(map.get("userId")+"");

        UserContext.setUser(userId);
        ArrayList itemIds =(ArrayList) map.get("itemIds");
        log.info("监听订单处理成功，清理购物车："+ itemIds.toString());
        cartService.removeByItemIds(itemIds);
        //走监听删除数据库之后删除线程数据
        UserContext.removeUser();
    }

//    public void tradeSuccess(Map map){
//        Long userId = (Long)map.get("userId");
//        UserContext.setUser(userId);
//        map.get("itemIds")
//        log.info("监听订单处理成功，清理购物车："+ itemIds.toString());
//        cartService.removeByItemIds(itemIds);
//    }
}
