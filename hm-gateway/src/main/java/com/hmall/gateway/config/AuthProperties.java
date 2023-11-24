package com.hmall.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "hm.auth")
@Configuration
public
class AuthProperties {
    private List<String> includePaths;
    private List<String> excludePaths;
}
