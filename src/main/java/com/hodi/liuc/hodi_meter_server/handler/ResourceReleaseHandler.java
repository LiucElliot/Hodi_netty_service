package com.hodi.liuc.hodi_meter_server.handler;

import com.hodi.liuc.hodi_meter_server.entity.ProtocolFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

// 自定义资源释放Handler（需实现）
@Slf4j
public class ResourceReleaseHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        // [强制内存释放] ByteBuf手动释放点
        while (ctx.channel().unsafe().outboundBuffer() != null) {
            ByteBuf buf = (ByteBuf) ctx.channel().unsafe().outboundBuffer().current();
            if (buf != null && buf.refCnt() > 0) {
                // [OOM防护] 显式释放未被引用的buffer
                buf.release();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // [异常处理] 关闭问题连接防止雪崩
        log.error("Channel exception", cause);
        ctx.close();
    }


}
