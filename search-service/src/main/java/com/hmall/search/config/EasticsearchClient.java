package com.hmall.search.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EasticsearchClient {
    @Bean
     public RestHighLevelClient getRestHighLevelClient(){
        return  new RestHighLevelClient(
                RestClient.builder(HttpHost.create("http://192.168.200.146:9200")));
    }
}
