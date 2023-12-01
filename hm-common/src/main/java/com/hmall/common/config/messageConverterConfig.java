package com.hmall.common.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(MessageConverter.class)
public class messageConverterConfig {
    @Bean
    public MessageConverter getJackson2JsonMessageConverter(){
        Jackson2JsonMessageConverter jsonMessageConverter = new Jackson2JsonMessageConverter();
        jsonMessageConverter.setCreateMessageIds(true);  //给序列化的对象一个唯一标识id
        return jsonMessageConverter;
    }
}
