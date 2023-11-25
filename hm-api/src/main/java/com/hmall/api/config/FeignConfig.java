package com.hmall.api.config;

import com.hmall.api.fallback.ItemClientFallbackFactory;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.hmall.api.client",defaultConfiguration = DefaultFeignConfig.class)
public class FeignConfig {
    @Bean
    public ItemClientFallbackFactory getItemClientFallbackFactory(){
        return new ItemClientFallbackFactory();
    }
}
