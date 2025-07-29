package com.hodi.liuc.hodi_meter_server.command_issued.manager;

import io.netty.channel.Channel;

/**
 * 连接管理层
 */
public interface ConnectionManager {

    void addConection(String deviceId, Channel channel);

    Channel getConnection(String deviceId);

    void removeConnection(String deviceId);

    int getConnectionCount();
}
