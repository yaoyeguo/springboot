package org.yaoyeguo.netty.server;

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.yaoyeguo.config.NettyWsServerConfig;
import org.yaoyeguo.constant.MessageCodeConstant;
import org.yaoyeguo.model.user.User;

import io.netty.channel.Channel;
import org.yaoyeguo.model.user.WebSocketMessage;
import org.yaoyeguo.netty.channel.group.ChannelGroup;
import org.yaoyeguo.util.WebSocketUtil;

import javax.xml.soap.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;


public class WebSocketInfoService {
    /*
     * 存储Channel与用户信息
     * */
    public static ConcurrentMap<Channel, User> webSocketInfoMap = new ConcurrentHashMap<>();

    /**
     * 用户在线数量
     */
    private static AtomicInteger userCount = new AtomicInteger(0);

    /**
     * 新的客户端与服务端进行连接，先保存新的 channel，
     * 当连接建立后，客户端会发送用户登陆请求（LOGIN_CODE），这时再将用户信息保存进去
     */
    public void addChannel(Channel channel) {
        final User user = new User();
        user.setAddress(WebSocketUtil.getChannelAddress(channel));
        webSocketInfoMap.put(channel, user);
        ChannelGroup.add(channel);
    }

    public boolean addUser(Channel channel, String nick, String id) {
        final User user = webSocketInfoMap.get(channel);
        if (user == null) {
            return false;
        }
        user.setId(id);
        user.setNick(nick);
        user.setAvatarAddress(getRandomAvatar());
        user.setTime(System.currentTimeMillis());
        userCount.incrementAndGet();
        return true;
    }

    public void updateUserListAndCount() {
        TextWebSocketFrame tws = new TextWebSocketFrame(new MessageService().messageJsonFactory(
                MessageCodeConstant.SYSTEM_MESSAGE_CODE, null,
                MessageCodeConstant.UPDATE_USERCOUNT_SYSTEM_MESSGAE_CODE, userCount
        ));
        ChannelGroup.sendAll(tws);
        final Set<Channel> set = webSocketInfoMap.keySet();
        List<User> userList = new ArrayList<>();
        for (Channel channel : set) {
            final User user = webSocketInfoMap.get(channel);
            userList.add(user);
        }
        tws = new TextWebSocketFrame(new MessageService().messageJsonFactory(
                MessageCodeConstant.SYSTEM_MESSAGE_CODE,
                null,
                MessageCodeConstant.UPDATE_USERLIST_SYSTEM_MESSGAE_CODE,
                userList
        ));
        ChannelGroup.sendAll(tws);

    }

    //发送私有消息
    public void sendPrivateChatMessage(String id, TextWebSocketFrame tws) {
        final Set<Channel> set = webSocketInfoMap.keySet();
        Channel receivceChannel = null;
        for (Channel channel : set) {
            final User user = webSocketInfoMap.get(channel);
            if (user.getId().equals(id)) {
                receivceChannel = channel;
                break;
            }
        }
        if (receivceChannel != null) {
            receivceChannel.writeAndFlush(tws);
        }
    }

    public void deleteChannel(Channel channel) {
        final User user = webSocketInfoMap.get(channel);
        if (user != null) {
            webSocketInfoMap.remove(channel);
            userCount.decrementAndGet();
            ChannelGroup.remove(channel);
            TextWebSocketFrame tws = new TextWebSocketFrame(new MessageService().messageJsonFactory(
                    MessageCodeConstant.SYSTEM_MESSAGE_CODE,
                    user.getNick() + "离开聊天室",
                    MessageCodeConstant.NORMAL_SYSTEM_MESSGAE_CODE,
                    null
            ));
            updateUserListAndCount();
            ChannelGroup.sendAll(tws);
        }
    }

    public void deleteGroup(Channel channel) {

    }

    /**
     * 返回一个随机的头像地址
     */
    private String getRandomAvatar() {
        int num = new Random().nextInt(33) + 1;
        return "../img/" + num + ".png";
    }

}
