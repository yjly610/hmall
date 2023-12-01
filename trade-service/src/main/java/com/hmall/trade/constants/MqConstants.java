package com.hmall.trade.constants;

import lombok.Data;

@Data
public class MqConstants {
    public static final String DELAY_EXCHANGE = "trade.delay.topic";
    public static final String DELAY_ORDER_ROUTING_KEY = "order.query";
    public static final String DELAY_ORDER_QUEUE = "trade.order.delay.queue";
}
