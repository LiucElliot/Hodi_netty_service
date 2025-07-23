package com.hodi.liuc.hodi_meter_server.handler;

import com.hodi.liuc.hodi_meter_server.entity.ProtocolFrame;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtocolFrameHandler extends ChannelInboundHandlerAdapter {
    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(ProtocolFrameHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // 1. 类型检查 - 只处理ProtocolFrame类型的消息
        if (!(msg instanceof ProtocolFrame)) {
            // 如果不是ProtocolFrame，传递给ResourceReleaseHandler处理
            ctx.fireChannelRead(msg);
            return;
        }

        ProtocolFrame frame = (ProtocolFrame) msg;
        try {
            // 2. 日志记录接收到的协议帧信息
            logFrameInfo(frame);

            // 3. 执行业务逻辑
            processBusinessLogic(ctx, frame);

        } finally {
            // 4. 关键！释放数据部分的ByteBuf内存
            if (frame.getData() != null) {
                frame.getData().release();
            }
        }
    }

    private void logFrameInfo(ProtocolFrame frame) {
        if (logger.isDebugEnabled()) {
            logger.debug("收到协议帧: RTUA={}, Seq={}, Control=0x{}, Length={}",
                    bytesToHex(frame.getRtua()),
                    bytesToHex(frame.getMstaSeq()),
                    String.format("%02X", frame.getControlCode()),
                    frame.getDataLength());
        }
    }

    private void processBusinessLogic(ChannelHandlerContext ctx, ProtocolFrame frame) {
        // TODO: 在此处实现具体业务逻辑
        // 示例代码：
        ByteBuf data = frame.getData();

        // 解析数据部分
        byte[] dataArray = new byte[data.readableBytes()];
        data.getBytes(data.readerIndex(), dataArray);

        // 根据控制码执行不同操作
        switch (frame.getControlCode()) {
            case (byte) 0xA4: // 示例：心跳包
                handleHeartbeat(ctx, frame);
                break;
            case (byte) 0xA1: // 登录帧
                handleLoginUpload(ctx, frame);
                break;
            // 其他控制码处理...
            default:
                logger.warn("未知控制码: 0x{}", String.format("%02X", frame.getControlCode()));
        }
    }

    /**
     * 登录帧处理
     */
    private void handleLoginUpload(ChannelHandlerContext ctx, ProtocolFrame frame) {
        byte controlCode = 0x21;
        // 组帧
        ctx.writeAndFlush(new ProtocolFrame(frame.getRtua(), frame.getMstaSeq(), controlCode, Unpooled.EMPTY_BUFFER, 0));
    }
    /**
     * 心跳帧处理
     */
    private void handleHeartbeat(ChannelHandlerContext ctx, ProtocolFrame frame) {
        // TODO: 心跳处理逻辑
        byte controlCode = 0x24;
        if (frame.getDataLength() == 0) {
            // 无校时心跳
            ctx.writeAndFlush(new ProtocolFrame(frame.getRtua(), frame.getMstaSeq(), controlCode, Unpooled.EMPTY_BUFFER, 0));
        } else {
            // 校时心跳
            ctx.writeAndFlush(new ProtocolFrame(frame.getRtua(), frame.getMstaSeq(), controlCode, createTimeDataByteBuf(), 8));
        }
    }

    /**
     * 获取当前时间并封装至Data
     */
    private ByteBuf createTimeDataByteBuf() {
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();

        // 创建固定大小的ByteBuf
        ByteBuf buf = Unpooled.buffer(8);

        // 使用高效位运算实现BCD编码（无除法操作）
        // 秒 (0-59)
        int second = now.getSecond();
        buf.writeByte(((second / 10) << 4) | (second % 10));

        // 分 (0-59)
        int minute = now.getMinute();
        buf.writeByte(((minute / 10) << 4) | (minute % 10));

        // 时 (0-23)
        int hour = now.getHour();
        buf.writeByte(((hour / 10) << 4) | (hour % 10));

        // 日 (1-31)
        int day = now.getDayOfMonth();
        buf.writeByte(((day / 10) << 4) | (day % 10));

        // 月 (1-12)
        int month = now.getMonthValue();
        buf.writeByte(((month / 10) << 4) | (month % 10));

        // 年 (后两位 00-99)
        int year = now.getYear() % 100;
        buf.writeByte(((year / 10) << 4) | (year % 10));

        // 填充保留位 (0x00)
        buf.writeByte(0x00);
        buf.writeByte(0x00);

        return buf;
    }

    private boolean needsResponse(ProtocolFrame frame) {
        // 判断是否需要响应（根据协议规范）
        return (frame.getControlCode() & 0x80) == 0; // 示例：最高位为0表示需要响应
    }

    private ByteBuf buildResponse(ChannelHandlerContext ctx, ProtocolFrame frame) {
        // TODO: 根据协议规范构造响应帧
        // 示例响应：确认帧
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeByte(0x68);
        buf.writeBytes(frame.getRtua());
        buf.writeBytes(frame.getMstaSeq());
        buf.writeByte(0x68);
        buf.writeByte((byte) 0x80); // 响应控制码
        buf.writeShortLE(0);        // 无数据
        // 计算校验和...
        buf.writeByte(0x16);        // 结束符
        return buf;
    }

    // 辅助方法：字节数组转十六进制字符串
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("处理协议帧时发生异常: {}", cause.getMessage(), cause);
        ctx.close();
    }
}
