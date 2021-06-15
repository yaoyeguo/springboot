package org.yaoyeguo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
//@Component
@ConfigurationProperties(prefix = "netty.websocket")
@Getter
@Setter
public class NettyWsServerConfig {

    private String host;

    private int port;

    private String uri;
    private String url;
}
