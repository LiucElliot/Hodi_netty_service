package com.hodi.liuc.hodi_meter_server.handler;

import com.hodi.liuc.hodi_meter_server.entity.ProtocolFrame;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class HeartbeatHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = (Logger) LoggerFactory.getLogger(HeartbeatHandler.class);
    private static final int MAX_MISSED_HEARTBEATS = 3;
    private final AtomicInteger missedHeartbeats = new AtomicInteger(0);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                if (missedHeartbeats.incrementAndGet() > MAX_MISSED_HEARTBEATS) {
                    logger.warning("心跳超时，关闭连接: " + ctx.channel());
                    ctx.close();
                }
            }
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ProtocolFrame) {
            ProtocolFrame frame = (ProtocolFrame) msg;
            if (frame.getControlCode() == (byte)0xA4) { // 心跳帧
                missedHeartbeats.set(0);
            }
        }
        ctx.fireChannelRead(msg);
    }
}
