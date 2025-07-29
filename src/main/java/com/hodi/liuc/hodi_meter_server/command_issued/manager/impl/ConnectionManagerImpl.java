package com.hodi.liuc.hodi_meter_server.command_issued.manager.impl;

import com.hodi.liuc.hodi_meter_server.command_issued.manager.ConnectionManager;
import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ConnectionManagerImpl implements ConnectionManager {
    private final ConcurrentHashMap<String, Channel> deviceChannelMap = new ConcurrentHashMap<>();
    private final AtomicInteger connectionCount = new AtomicInteger(0);

    @Override
    public void addConection(String deviceId, Channel channel) {
        deviceChannelMap.put(deviceId, channel);
        connectionCount.incrementAndGet();
    }

    @Override
    public Channel getConnection(String deviceId) {
        return deviceChannelMap.get(deviceId);
    }

    @Override
    public void removeConnection(String deviceId) {
        if (deviceChannelMap.remove(deviceId) != null) {
            connectionCount.decrementAndGet();
        }
    }

    @Override
    public int getConnectionCount() {
        return connectionCount.get();
    }
}
