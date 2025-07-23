package com.hodi.liuc.hodi_meter_server.config;

import com.hodi.liuc.hodi_meter_server.decoder.ProtocolDecoder;
import com.hodi.liuc.hodi_meter_server.handler.ResourceReleaseHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

// ServerInitializer.java
public class ServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) {
        // 1. 获取管道
        ChannelPipeline pipeline = ch.pipeline();

        // 2. 添加ProtocolDecoder到管道
        pipeline.addLast("frameDecoder", new ProtocolDecoder()); // ← 这里是关键引用！

        // 3. 添加业务处理器
        pipeline.addLast("frameHandler", new ResourceReleaseHandler());

        // 可选：添加其他处理器如日志、SSL等
    }
}
