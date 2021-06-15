package org.yaoyeguo.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaoyeguo.netty.handler.WebSocketChannelHandler;

@Component
@Slf4j
public class NettyServer {

    private int port = 18090;
    private String host = "0.0.0.0";
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;

    public NettyServer() {
    }

    public NettyServer(int port) {
        this.port = port;
    }

    public NettyServer(int port, String host) {
        this.port = port;
        this.host = host;
    }

    public NettyServer(int port, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        this.port = port;
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
    }

    private static NettyServer nettyServer;

    public static NettyServer getInstance() {
        if (nettyServer != null) {
            return nettyServer;
        }
        synchronized (NettyServer.class) {
            if (nettyServer != null) {
                return nettyServer;
            }
            nettyServer = new NettyServer();
            return nettyServer;
        }
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap sb = new ServerBootstrap();
        try {
            sb.group(bossGroup, workerGroup);
            sb.channel(NioServerSocketChannel.class);
//            sb.localAddress(this.host, this.port);
            sb.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    //解码器
                    ch.pipeline().addLast("http-codec", new HttpServerCodec());
                    //将多个消息转换成单一的消息对象
                    ch.pipeline().addLast("aggregator", new HttpObjectAggregator(65536));
                    //支持异步发送大的码流，一般用于发送文件流
                    ch.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
                    //用于处理websocket
                    ch.pipeline().addLast("websocket", new WebSocketChannelHandler());
                }
            });
            channelFuture = sb.bind(this.port).sync();
            channelFuture.channel().closeFuture().sync();
            log.info("start server port:{}(tcp)", this.channelFuture.channel().localAddress());
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void destory() {
        if (channelFuture == null) {
            return;
        }
        log.info("关闭netty服务,address:{}", channelFuture.channel().localAddress());
        try {
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("关闭netty服务异常,msg:{}", e.getMessage());
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully().syncUninterruptibly();
            workerGroup.shutdownGracefully().awaitUninterruptibly();
        }
    }
}
