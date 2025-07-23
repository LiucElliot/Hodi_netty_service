package com.hodi.liuc.hodi_meter_server.encoder;

import com.hodi.liuc.hodi_meter_server.entity.ProtocolFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.springframework.stereotype.Component;

@Sharable
@Component
public class ProtocolEncoder extends MessageToByteEncoder<ProtocolFrame> {
    @Override
    protected void encode(ChannelHandlerContext ctx, ProtocolFrame msg, ByteBuf out) {
        out.writeByte(0x68)
                .writeBytes(msg.getRtua())
                .writeBytes(msg.getMstaSeq())
                .writeByte(0x68)
                .writeByte(msg.getControlCode())
                .writeShortLE(msg.getDataLength())
                .writeBytes(msg.getData(), msg.getData().readerIndex(), msg.getData().readableBytes());

        byte[] checksumRegion = new byte[out.readableBytes()];
        out.getBytes(0, checksumRegion);
        int sum = 0;
        for (byte b : checksumRegion) sum = (sum + (b & 0xFF)) & 0xFF;
        out.writeByte(sum).writeByte(0x16);
    }
}
