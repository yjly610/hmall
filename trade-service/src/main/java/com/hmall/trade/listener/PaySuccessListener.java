package com.hmall.trade.listener;

import com.hmall.common.utils.UserContext;
import com.hmall.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PaySuccessListener {
    private final IOrderService orderService;

    /**
     * 监听订单支付成功
     * @param orderId 业务订单号
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "mark.order.pay.queue",declare = "true"),
            exchange = @Exchange(name = "pay.topic",type = ExchangeTypes.TOPIC),
            key = "pay.success"
    ))
    public void paySuccess(Long orderId){
        //Long user = UserContext.getUser();
        //log.error("支付成功测试获取线程对象中的用户id"+user+"");
        log.info("监听支付成功："+orderId);
        orderService.markOrderPaySuccess(orderId);
    }
}
