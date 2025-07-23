package com.hodi.liuc.hodi_meter_server.entity;

import io.netty.buffer.ByteBuf;
import lombok.Data;

/**
 * 帧实体
 */
@Data
public class ProtocolFrame {
    private final byte[] rtua;       // 4字节终端地址
    private final byte[] mstaSeq;    // 2字节主站地址+序号
    private final byte controlCode;  // 1字节控制码
    private final ByteBuf data;      // 数据域
    private final int dataLength;   // 数据域长度
}
