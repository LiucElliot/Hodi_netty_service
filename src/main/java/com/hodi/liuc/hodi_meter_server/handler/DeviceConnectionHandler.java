package com.hodi.liuc.hodi_meter_server.handler;

import com.hodi.liuc.hodi_meter_server.command_issued.manager.ConnectionManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import org.slf4j.LoggerFactory;

import java.util.logging.Logger;

/** 设备连接层
 * 连接生命周期管理实现
 */
public class DeviceConnectionHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = (Logger) LoggerFactory.getLogger(DeviceConnectionHandler.class);
    private static final AttributeKey<String> DEVICE_ID_KEY = AttributeKey.valueOf("deviceId");

    private final ConnectionManager connectionManager;
    public DeviceConnectionHandler(final ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("新设备上线：" + ctx.channel().remoteAddress());
        setDeviceId(ctx.channel(), ctx.channel().attr(DEVICE_ID_KEY).get());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String deviceId = ctx.channel().attr(DEVICE_ID_KEY).get();
        if (deviceId != null) {
            connectionManager.removeConnection(deviceId);
            logger.info("设备断开连接:" + deviceId);
        }
        ctx.fireChannelInactive();
    }

    public void setDeviceId(Channel channel, String deviceId) {
        channel.attr(DEVICE_ID_KEY).set(deviceId);
        connectionManager.addConection(deviceId, channel);
        logger.info("设备注册：" + deviceId);
    }

}
