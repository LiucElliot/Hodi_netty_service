package com.hodi.liuc.hodi_meter_server.command_issued;

import io.netty.buffer.ByteBuf;

import java.util.concurrent.CompletableFuture;

/**
 * 下发指令服务接口
 * @API 提供异步非阻塞命令下发能力
 */
public interface CommandService {
    /**
     * 异步下发设备命令
     *
     * @param deviceId 设备唯一标识
     * @param controlCode 协议控制码
     * @param payload 命令负载数据
     * @return 异步结果Future
     *
     * @API 保证线程安全，支持高并发调用
     */
    CompletableFuture<Void> sendCommand(String deviceId, byte controlCode, ByteBuf payload);
}
