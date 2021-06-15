package org.yaoyeguo.netty.handler;

import com.alibaba.fastjson.JSONObject;
import com.sun.xml.internal.ws.util.StringUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.yaoyeguo.config.NettyWsServerConfig;
import org.yaoyeguo.constant.MessageCodeConstant;
import org.yaoyeguo.model.user.User;
import org.yaoyeguo.netty.channel.group.ChannelGroup;
import org.yaoyeguo.netty.server.MessageService;
import org.yaoyeguo.netty.server.WebSocketInfoService;
import org.yaoyeguo.util.SpringContextHolder;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class WebSocketChannelHandler extends SimpleChannelInboundHandler<Object> {

    private WebSocketServerHandshaker handshaker;

    private WebSocketInfoService webSocketInfoService = new WebSocketInfoService();

    private MessageService messageService = new MessageService();

    private static NettyWsServerConfig wsServerConfig = SpringContextHolder.getBean(NettyWsServerConfig.class);

    public static AtomicInteger userAccount = new AtomicInteger(0);


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        webSocketInfoService.addChannel(ctx.channel());
        log.info("客户端:{}上线", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        webSocketInfoService.deleteChannel(ctx.channel());
        log.info("客户端:{}下线", ctx.channel().remoteAddress());
    }

    //服务端接受客户端发送的数据结束后调用
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object object) throws Exception {
        //处理客户端向服务端发起 http 请求的业务
        if (object instanceof FullHttpRequest) {
            handlerFullHttpRequest(ctx, (FullHttpRequest) object);
        } else if (object instanceof WebSocketFrame) {
            handlerWebSocketFrame(ctx, (WebSocketFrame) object);
        }
        System.out.println(object.toString());
    }

    /**
     * 处理客户端与服务端之间的 websocket 业务
     */
    private void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (ctx instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            webSocketInfoService.deleteChannel(ctx.channel());
            return;
        }
        if (ctx instanceof PingWebSocketFrame) {
            ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        if (ctx instanceof PongWebSocketFrame) {
            ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        if (!(frame instanceof TextWebSocketFrame)) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            throw new RuntimeException("【" + this.getClass().getName() + "】不支持消息");
        }
        String message = ((TextWebSocketFrame) frame).text();
        JSONObject jsonObject = JSONObject.parseObject(message);
        ;
        Integer code = jsonObject.getInteger("code");
        String nick = jsonObject.getString("nick");
        String chatMessage = jsonObject.getString("chatMessage");

        User user = WebSocketInfoService.webSocketInfoMap.get(ctx.channel());
        TextWebSocketFrame tws = null;
        switch (code) {
            //登录
            case MessageCodeConstant.LOGIN_CODE:
                String id = UUID.randomUUID().toString();
                webSocketInfoService.addUser(ctx.channel(), nick, id);
                webSocketInfoService.updateUserListAndCount();
                tws = new TextWebSocketFrame(messageService.messageJsonFactory(
                        MessageCodeConstant.SYSTEM_MESSAGE_CODE, null,
                        MessageCodeConstant.PERSONAL_SYSTEM_MESSGAE_CODE, user
                ));
                ctx.channel().writeAndFlush(tws);
                break;
            //群聊
            case MessageCodeConstant.GROUP_CHAT_CODE:
                tws = new TextWebSocketFrame(messageService.messageJsonFactory(
                        MessageCodeConstant.GROUP_CHAT_MESSAGE_CODE,
                        chatMessage,
                        user,
                        null
                ));
                ChannelGroup.sendAll(tws);
                break;

            //私聊
            case MessageCodeConstant.PRIVATE_CHAT_CODE:
                Channel myChannel = ctx.channel();
                String receiveMsgId = jsonObject.getString("id");
                String sendMsgId = user.getId();
                /*
                    向目标用户发送私聊信息，发送人 id 为 senderId
                 */
                tws = new TextWebSocketFrame(messageService.messageJsonFactory(
                        MessageCodeConstant.PRIVATE_CHAT_MESSAGE_CODE,
                        chatMessage,
                        user, sendMsgId
                ));
                webSocketInfoService.sendPrivateChatMessage(receiveMsgId, tws);
                break;
            default:
        }
    }

    private void handlerFullHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        final DecoderResult decoderResult = request.decoderResult();
        if (request.decoderResult().isFailure()) {
            sendHttpResonse(ctx, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        String upgrade = request.headers().get("Upgrade");
        if (StringUtil.isNullOrEmpty(upgrade) || !wsServerConfig.getUri().equals(upgrade)) {
            sendHttpResonse(ctx, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        //websocket工厂
        final WebSocketServerHandshakerFactory handshakerFactory = new WebSocketServerHandshakerFactory(
                wsServerConfig.getUrl(), null, false);
        //新建握手
        handshaker = handshakerFactory.newHandshaker(request);
        if (handshaker == null) {
            //如果为空，返回响应：不受支持的 websocket 版本
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            //执行握手
            handshaker.handshake(ctx.channel(), request);
        }
    }

    private void sendHttpResonse(ChannelHandlerContext ctx, DefaultFullHttpResponse response) {
        if (response.status().code() != HttpResponseStatus.OK.code()) {
            //创建缓冲区
            final ByteBuf byteBuf = Unpooled.copiedBuffer(response.status().toString(), CharsetUtil.UTF_8);
            //将源缓存区数据传达送到此缓存区
            response.content().writeBytes(byteBuf);
            //释放源缓存区
            byteBuf.release();
        }
        final ChannelFuture channelFuture = ctx.channel().writeAndFlush(response);
        if (response.status().code() != HttpResponseStatus.OK.code()) {
            channelFuture.addListener(ChannelFutureListener.CLOSE);
        }

    }

//    @Override
//    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//        System.out.println(msg.toString());
//    }

}
