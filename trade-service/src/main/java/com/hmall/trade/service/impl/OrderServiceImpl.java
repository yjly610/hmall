package com.hmall.trade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.api.client.CartClient;
import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.utils.UserContext;
import com.hmall.trade.constants.MqConstants;
import com.hmall.trade.domain.dto.MultiDelayMessage;
import com.hmall.trade.domain.dto.OrderFormDTO;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.domain.po.OrderDetail;
import com.hmall.trade.mapper.OrderMapper;
import com.hmall.trade.service.IOrderDetailService;
import com.hmall.trade.service.IOrderService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2023-05-05
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private final ItemClient itemClient;
    private final IOrderDetailService detailService;
    private final CartClient cartClient;
    private final RabbitTemplate rabbitTemplate;

    @Override
    //@Transactional
    @GlobalTransactional
    public Long createOrder(OrderFormDTO orderFormDTO) {
        // 1.订单数据
        Order order = new Order();
        // 1.1.查询商品
        List<OrderDetailDTO> detailDTOS = orderFormDTO.getDetails();
        // 1.2.获取商品id和数量的Map
        Map<Long, Integer> itemNumMap = detailDTOS.stream()
                .collect(Collectors.toMap(OrderDetailDTO::getItemId, OrderDetailDTO::getNum));
        Set<Long> itemIds = itemNumMap.keySet();
        // 1.3.查询商品
        List<ItemDTO> items = itemClient.queryItemByIds(itemIds);
        if (items == null || items.size() < itemIds.size()) {
            throw new BadRequestException("商品不存在");
        }
        // 1.4.基于商品价格、购买数量计算商品总价：totalFee
        int total = 0;
        for (ItemDTO item : items) {
            total += item.getPrice() * itemNumMap.get(item.getId());
        }
        order.setTotalFee(total);
        // 1.5.其它属性
        order.setPaymentType(orderFormDTO.getPaymentType());
        order.setUserId(UserContext.getUser());
        order.setStatus(1);
        // 1.6.将Order写入数据库order表中
        save(order);

        // 2.保存订单详情
        List<OrderDetail> details = buildDetails(order.getId(), items, itemNumMap);
        detailService.saveBatch(details);

        // 3.清理购物车商品
        //cartClient.deleteCartItemByIds(itemIds);
        //改为使用RabbitMQ异步通知控制删除购物车数据
        String exchangeName = "trade.topic";
        String key = "trade.success";
        //准备数据
        Map<String,Object> map = new HashMap<>();
        Long userId = UserContext.getUser();
        map.put("userId",userId);
        map.put("itemIds",itemIds);
        rabbitTemplate.convertAndSend(exchangeName,key,map);

        // 4.扣减库存
        try {
            itemClient.deductStock(detailDTOS);
        } catch (Exception e) {
            throw new RuntimeException("库存不足！");
        }

        //5.发送MQ消息检测订单支付状态
        try {
            MultiDelayMessage<Long> oldMessage = MultiDelayMessage.of(order.getId(),10000L,10000L,10000L,30000L);
            Long time = oldMessage.removeNextDelay();
            log.error(oldMessage+"");
            rabbitTemplate.convertAndSend(MqConstants.DELAY_EXCHANGE, MqConstants.DELAY_ORDER_ROUTING_KEY, oldMessage, new MessagePostProcessor() {
                @Override
                public Message postProcessMessage(Message message) throws AmqpException {
                    message.getMessageProperties().setDelay(time.intValue());
                    return message;
                }
            });
        } catch (AmqpException e) {
            e.printStackTrace();
        }


        return order.getId();
    }

    /**
     * 如果消息存在重试，可能会存在业务逻辑问题 ：
     * 如；第一次修改改为2之后，客户申请了急速退款，可能会退款后，又修改为已付款问题
     * 处理方式：
     * 1加锁
     * 2sql语句控制
     * @param orderId
     */
    @Override
    public void markOrderPaySuccess(Long orderId) {
        this.lambdaUpdate()
                .set(Order::getStatus,2)
                .set(Order::getUpdateTime,LocalDateTime.now())
                .eq(Order::getId,orderId)
                .eq(Order::getStatus,1)
                .update();
//        Order order = new Order();
//        order.setId(orderId);
//        //订单的状态，1、未付款 2、已付款,未发货 3、已发货,未确认 4、确认收货，交易成功
//        // 5、交易取消，订单关闭 6、交易结束，已评价
//        order.setStatus(2);
//        order.setPayTime(LocalDateTime.now());
//        updateById(order);
    }

    private List<OrderDetail> buildDetails(Long orderId, List<ItemDTO> items, Map<Long, Integer> numMap) {
        List<OrderDetail> details = new ArrayList<>(items.size());
        for (ItemDTO item : items) {
            OrderDetail detail = new OrderDetail();
            detail.setName(item.getName());
            detail.setSpec(item.getSpec());
            detail.setPrice(item.getPrice());
            detail.setNum(numMap.get(item.getId()));
            detail.setItemId(item.getId());
            detail.setImage(item.getImage());
            detail.setOrderId(orderId);
            details.add(detail);
        }
        return details;
    }
}
