package com.hmall.gateway.filter;

import reactor.core.publisher.Mono;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;

@Component
public class MyGlobalFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        System.out.println("拦截到了请求。。。。。");
        return chain.filter(exchange);
    }

    /**
     * 过滤器优先级  值越小 优先级越高
     * @return
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
