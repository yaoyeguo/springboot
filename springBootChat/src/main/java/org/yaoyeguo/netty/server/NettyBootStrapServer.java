package org.yaoyeguo.netty.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.yaoyeguo.config.NettyWsServerConfig;

@Component
@Slf4j
public class NettyBootStrapServer implements ApplicationListener<ApplicationEvent> {

    private NettyServer nettyServer;

    @Autowired
    private NettyWsServerConfig wsServerConfig;

    public void start() {
        if (nettyServer != null) {
            return;
        }
        nettyServer = new NettyServer(wsServerConfig.getPort(), wsServerConfig.getHost());
        try {
            nettyServer.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("启动netty服务失败,{}", e.getMessage());
        }
    }


    public void destory() {
        if (nettyServer != null) {
            nettyServer.destory();
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            start();
        } else if (event instanceof ContextClosedEvent) {
            destory();
        }
    }
}
