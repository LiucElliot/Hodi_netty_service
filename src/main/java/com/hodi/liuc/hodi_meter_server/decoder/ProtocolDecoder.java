package com.hodi.liuc.hodi_meter_server.decoder;

import com.hodi.liuc.hodi_meter_server.entity.ProtocolFrame;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 协议解码层
 * 字节流到协议帧的转换在此！！！
 */
public class ProtocolDecoder extends ByteToMessageDecoder {
    private static final int MIN_FRAME_LENGTH = 12;
    private static final byte START_FLAG = 0x68;
    private static final byte END_FLAG = 0x16;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        while (in.readableBytes() >= MIN_FRAME_LENGTH) {
            in.markReaderIndex();

            if (in.readByte() != START_FLAG) continue;

            byte[] rtua = new byte[4];
            in.readBytes(rtua);
            byte[] mstaSeq = new byte[2];
            in.readBytes(mstaSeq);

            if (in.readByte() != START_FLAG) continue;

            byte controlCode = in.readByte();
            int dataLength = in.readUnsignedShortLE();

            if (in.readableBytes() < dataLength + 2) return;

            ByteBuf data = in.readRetainedSlice(dataLength);
            byte checksum = in.readByte();

            if (calculateChecksum(rtua, mstaSeq, controlCode, dataLength, data) != checksum) continue;
            if (in.readByte() != END_FLAG) continue;

            out.add(new ProtocolFrame(rtua, mstaSeq, controlCode, data, dataLength));
            return;
        }
    }

    private byte calculateChecksum(byte[] rtua, byte[] mstaSeq,
                                   byte controlCode, int dataLength, ByteBuf data) {
        ByteBuf buf = Unpooled.buffer();
        try {
            buf.writeByte(START_FLAG)
                    .writeBytes(rtua)
                    .writeBytes(mstaSeq)
                    .writeByte(START_FLAG)
                    .writeByte(controlCode)
                    .writeShortLE(dataLength)
                    .writeBytes(data, data.readerIndex(), data.readableBytes());

            int sum = 0;
            while (buf.isReadable()) sum = (sum + buf.readUnsignedByte()) & 0xFF;
            buf.release();
            return (byte) sum;
        } finally {
            buf.release();
        }
    }

}
