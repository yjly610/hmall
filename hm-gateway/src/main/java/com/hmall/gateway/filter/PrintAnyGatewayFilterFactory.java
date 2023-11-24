package com.hmall.gateway.filter;

import lombok.Data;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class PrintAnyGatewayFilterFactory extends AbstractGatewayFilterFactory<PrintAnyGatewayFilterFactory.Config> {
    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter(new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                System.out.println("PringAny--Filter");
                System.out.println("参数a："+config.a);
                System.out.println("参数b："+config.b);
                System.out.println("参数c："+config.c);
                return chain.filter(exchange);
            }
        },-1);
    }

    //参数--需要自定义内部类
    @Data
    public static class Config{
        private String a;
        private String b;
        private String c;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("a","b","c");
    }

    @Override
    public Class<Config> getConfigClass() {
        return Config.class;
    }
}
