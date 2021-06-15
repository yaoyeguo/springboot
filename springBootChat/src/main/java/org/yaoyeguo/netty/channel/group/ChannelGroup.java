package org.yaoyeguo.netty.channel.group;

import io.netty.channel.Channel;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

public class ChannelGroup {
    private static DefaultChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public static DefaultChannelGroup getClients() {
        return ChannelGroup.group;
    }

    public static void add(Channel channel) {
        getClients().add(channel);
    }

    public static boolean remove(Channel channel) {
        return getClients().remove(channel);
    }

    public static void sendAll(Object o) {
        getClients().writeAndFlush(o);
    }
}
