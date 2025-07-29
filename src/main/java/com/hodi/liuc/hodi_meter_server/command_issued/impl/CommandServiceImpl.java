package com.hodi.liuc.hodi_meter_server.command_issued.impl;

import com.google.errorprone.annotations.Var;
import com.hodi.liuc.hodi_meter_server.command_issued.CommandService;
import com.hodi.liuc.hodi_meter_server.command_issued.manager.ConnectionManager;
import com.hodi.liuc.hodi_meter_server.entity.ProtocolFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 命令下发服务层实现
 * 异步命令下发与响应监听
 */
@Service
public class CommandServiceImpl implements CommandService {
    private static final int MAX_PENDING_COMMANDS = 100_000;
    private static final long COMMAND_TIMEOUT = 30_000; // TODO:暂定30秒超时

    private final ConnectionManager connectionManager;
    private final ConcurrentHashMap<String, CompletableFuture<Void>> pendingCommands = new ConcurrentHashMap<>();
    private final Semaphore backpressureSemaphore = new Semaphore(MAX_PENDING_COMMANDS);
    private final ScheduledExecutorService timeoutExecutor =
            Executors.newScheduledThreadPool(1, r -> new Thread(r, "指令应答超时"));
    private final AtomicLong sequenceGenerator = new AtomicLong(0);

    @Autowired
    public CommandServiceImpl(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Async
    @Override
    public CompletableFuture<Void> sendCommand(String deviceId, byte controlCode, ByteBuf payload) {
        if (!backpressureSemaphore.tryAcquire()) {
            return CompletableFuture.failedFuture(new RejectedExecutionException("下发指令队列已满"));
        }

        Channel channel = connectionManager.getConnection(deviceId);
        if (channel == null || !channel.isActive()) {
            backpressureSemaphore.release();
            return CompletableFuture.failedFuture(new IllegalStateException("该channel不可用"));
        }

        byte[] rtua = parseDeviceId(deviceId);
        byte[] seq = generateSequence();
        ProtocolFrame frame = new ProtocolFrame(rtua, seq, controlCode, payload, payload.readableBytes());

        CompletableFuture<Void> future = new CompletableFuture<>();
        String key = buildKey(deviceId, seq);
        pendingCommands.put(key, future);

        // 发送指令
        channel.writeAndFlush(frame).addListener(f -> {
            if (f.isSuccess()) {
                future.completeExceptionally(f.cause());
                removePendingCommand(key);
            }
        });

        // 设置超时
        timeoutExecutor.schedule(() -> {
            CompletableFuture<Void> expiredFuture = pendingCommands.remove(key);
           if (expiredFuture != null && !expiredFuture.isDone()) {
               expiredFuture.completeExceptionally(new TimeoutException("指令应答超时"));
               backpressureSemaphore.release();
           }
        }, COMMAND_TIMEOUT, TimeUnit.MILLISECONDS);

        return future.whenComplete((r, e) -> {
            if (e != null) {
                backpressureSemaphore.release();
            }
        });
    }

    /** 响应处理回调函数 */
    public void handleResponse(String deviceId, byte[] seq) {
        String key = buildKey(deviceId, seq);
        CompletableFuture<Void> future = pendingCommands.remove(key);
        if (future != null) {
            future.complete(null);
            backpressureSemaphore.release();
        }
    }

    private void removePendingCommand(String key) {
        CompletableFuture<Void> future = pendingCommands.remove(key);
        if (future != null) {
            backpressureSemaphore.release();
        }
    }

    private String buildKey(String deviceId, byte[] seq) {
        return deviceId + ":" + bytesToHex(seq);
    }

    /** 设备ID解析为deviceID */
    private byte[] parseDeviceId(String deviceId) {
        byte[] bytes = new byte[4];
        byte[] idBytes = deviceId.getBytes();
        System.arraycopy(idBytes, 0, bytes, 0, Math.min(idBytes.length, 4));
        return bytes;
    }

    /** 生成2字节序列号 */
    private byte[] generateSequence() {
        long seq = sequenceGenerator.incrementAndGet();
        return new byte[]{(byte) (seq >>> 8), (byte) seq};
    }

    /** bytes转16进制hex码 */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

}
