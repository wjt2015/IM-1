package com.yim.im.client;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.yim.im.client.api.ChatApi;
import com.yim.im.client.api.ClientMsgListener;
import com.yim.im.client.api.UserApi;
import com.yim.im.client.handler.ClientConnectorHandler;
import com.yim.im.client.handler.code.AesDecoder;
import com.yim.im.client.handler.code.AesEncoder;
import com.yrw.im.common.code.MsgDecoder;
import com.yrw.im.common.code.MsgEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Date: 2019-04-15
 * Time: 16:42
 *
 * @author yrw
 */
public class Client {
    private static Logger logger = LoggerFactory.getLogger(Client.class);

    public static Injector injector = Guice.createInjector();

    private String connectorHost;
    private Integer connectorPort;
    private ClientMsgListener clientMsgListener;

    public Client() {
    }

    public Client start() {
        assert connectorHost != null;
        assert connectorPort != null;
        assert clientMsgListener != null;

        ClientConnectorHandler.setClientMsgListener(clientMsgListener);

        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(group)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) throws Exception {
                    ChannelPipeline p = ch.pipeline();

                    //out
                    p.addLast("MsgEncoder", new MsgEncoder());
                    p.addLast("AesEncoder", injector.getInstance(AesEncoder.class));

                    //in
                    p.addLast("MsgDecoder", injector.getInstance(MsgDecoder.class));
                    p.addLast("AesDecoder", injector.getInstance(AesDecoder.class));
                    p.addLast("ClientConnectorHandler", injector.getInstance(ClientConnectorHandler.class));
                }
            }).connect(connectorHost, connectorPort)
            .addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    logger.info("Client connect connector successfully...");
                } else {
                    logger.error("Client connect connector failed!");
                }
            });
        return this;
    }

    public Client setConnectorHost(String connectorHost) {
        this.connectorHost = connectorHost;
        return this;
    }

    public Client setConnectorPort(Integer connectorPort) {
        this.connectorPort = connectorPort;
        return this;
    }

    public Client setClientMsgListener(ClientMsgListener clientMsgListener) {
        this.clientMsgListener = clientMsgListener;
        return this;
    }

    public <T> T getApi(Class<T> clazz) {
        assert clazz == UserApi.class || clazz == ChatApi.class;
        return injector.getInstance(clazz);
    }
}