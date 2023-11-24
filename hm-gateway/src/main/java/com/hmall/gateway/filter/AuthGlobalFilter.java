package com.hmall.gateway.filter;

import cn.hutool.core.collection.CollectionUtil;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter {

    private final JwtTool jwtTool;
    private final AuthProperties authProperties;
    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获取请求对象
        ServerHttpRequest request = exchange.getRequest();

        //判断是否不需要拦截的路径
        URI uri = request.getURI();
        if (isExcludePaths(uri)){
            //放行
            return chain.filter(exchange);
        }
        //拦截路径 - 获取请求头
        HttpHeaders headers = request.getHeaders();
        List<String> authorization = headers.get("authorization");
        if (CollectionUtil.isNotEmpty(authorization)){
            //非空解析
            String token = authorization.get(0);
            try {
                Long userId = jwtTool.parseToken(token);
                //解析成功 - 直接放行、
                //放行前 将userId放入请求头
                exchange.mutate().request(builder -> builder.header("user-info",userId+"")).build();
                return chain.filter(exchange);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete(); //返回401 没登陆
    }

    //判断是否是排除路径
    private boolean isExcludePaths(URI uri) {
        String uriPath = uri.getPath();
        List<String> paths = authProperties.getExcludePaths();
        for (String path : paths) {
            boolean match = antPathMatcher.match(path, uriPath);
            if (match) return match;
        }
        return false;
    }
}
