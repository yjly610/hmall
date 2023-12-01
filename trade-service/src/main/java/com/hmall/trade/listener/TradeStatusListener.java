package com.hmall.trade.listener;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.hmall.api.client.ItemClient;
import com.hmall.api.client.PayClient;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.api.dto.PayOrderDTO;
import com.hmall.trade.constants.MqConstants;
import com.hmall.trade.domain.dto.MultiDelayMessage;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.domain.po.OrderDetail;
import com.hmall.trade.service.IOrderDetailService;
import com.hmall.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeStatusListener {
    private final IOrderService orderService;
    private final PayClient payClient;
    private final RabbitTemplate rabbitTemplate;
    private final ItemClient itemClient;
    private final IOrderDetailService orderDetailService;


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstants.DELAY_ORDER_QUEUE,durable = "true"),
            exchange = @Exchange(name = MqConstants.DELAY_EXCHANGE,type = ExchangeTypes.TOPIC,delayed = "true"),
            key = MqConstants.DELAY_ORDER_ROUTING_KEY
    ))
    public void tradeStatus(MultiDelayMessage<Long> message){
        //获取订单
        Long orderId = message.getData();
        Order order = orderService.getById(orderId);
        //判断订单状态是否是未执行
        if (order == null || order.getStatus() >1){
            //不是未执行或为空直接结束
            return;
        }
        //远程调用Pay-Service 查询支付状态
        PayOrderDTO payOrderDTO = payClient.tqueryPayOrderByBizOrderNo(orderId);
        //判断订单支付状态
        if (ObjectUtil.isNotEmpty(payOrderDTO) && payOrderDTO.getStatus() ==3){
            //已支付的话修改订单状态，结束
            orderService.markOrderPaySuccess(orderId);
            return;
        }
        //未支付
        if (message.hasNextDelay()){
            // 时间列表还有值则继续加入MQ
            Long time = message.removeNextDelay();
            try {

                log.error(message+"");
                rabbitTemplate.convertAndSend(MqConstants.DELAY_EXCHANGE, MqConstants.DELAY_ORDER_ROUTING_KEY, message, new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        message.getMessageProperties().setDelay(time.intValue());
                        return message;
                    }
                });
            } catch (AmqpException e) {
                e.printStackTrace();
            }
        }else {
            //事件列表没值 ，回退订单
            //订单状态修改为超时
            orderService.lambdaUpdate().set(Order::getStatus,5)
                    .set(Order::getCloseTime, LocalDateTime.now())
                    .eq(Order::getId,orderId).update();
            //库存恢复
            //构造所需集合
            List<OrderDetail> list = orderDetailService.lambdaQuery().eq(OrderDetail::getOrderId, orderId).list();
            List<OrderDetailDTO> detailDTOS = BeanUtil.copyToList(list, OrderDetailDTO.class);
            detailDTOS.forEach(d->{
                d.setNum(0-d.getNum());
            });
            itemClient.deductStock(detailDTOS);
        }
    }
}
