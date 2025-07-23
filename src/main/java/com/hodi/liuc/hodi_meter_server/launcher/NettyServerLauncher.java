package com.hodi.liuc.hodi_meter_server.launcher;

import com.hodi.liuc.hodi_meter_server.decoder.ProtocolDecoder;
import com.hodi.liuc.hodi_meter_server.encoder.ProtocolEncoder;
import com.hodi.liuc.hodi_meter_server.handler.ProtocolFrameHandler;
import com.hodi.liuc.hodi_meter_server.handler.ResourceReleaseHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class NettyServerLauncher implements CommandLineRunner, DisposableBean {
    // 1. 利用Spring配置注入实现解耦
    @Value("${netty.server.main.port}")
    private int nettyPort;
    @Value("${netty.server.main.boss-threads}")
    private int bossThreads;
    @Value("${netty.server.main.worker-threads}")
    private int workerThreads;

    // [线程安全设计] 使用final确保线程组可见性
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(bossThreads, new DefaultThreadFactory("netty-boss"));
    private final EventLoopGroup workerGroup = new NioEventLoopGroup(workerThreads, new DefaultThreadFactory("netty-worker"));
    private Channel serverChannel;

    @Override
    public void run(String... args) throws Exception {
        // 2. 遵循Spring生命周期管理
        startNettyServer();
    }

    private void startNettyServer() {
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // [OOM预防] SO_BACKLOG配置防止连接风暴
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // [生产优化] 禁用Nagle算法降低延迟
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            // 1. 添加协议解码器、编码器
                            pipeline.addLast("frameDecoder", new ProtocolDecoder());
                            pipeline.addLast("frameEncoder", new ProtocolEncoder());
                            // 2. 添加业务处理器
                            pipeline.addLast("frameHandler", new ProtocolFrameHandler());
                            // 3. 资源释放处理器
                            pipeline.addLast("resourceRelease", new ResourceReleaseHandler());
                        }
                    });

            // [连接监控] 绑定端口并添加监听器
//            ChannelFuture future = bootstrap.bind(nettyPort).sync();
            ChannelFuture future = bootstrap.bind(nettyPort);
            serverChannel = future.channel();
            future.addListener(f -> {
                if (f.isSuccess()) {
                    log.info("通讯服务器已启动，通讯端口： {}", nettyPort);
                } else {
                    log.error("通讯服务器启动失败！", f.cause());
                    // [异常处理] 关闭资源防止泄漏
                    stopNettyServer();
                }
            });

            // [线程切换预警] 避免阻塞Spring主线程
            serverChannel.closeFuture().addListener(f -> log.warn("Netty channel closed unexpectedly"));

        } catch (Exception e) {
            log.error("Netty server start exception", e);
            throw new IllegalStateException("Netty startup failed", e);
        }
    }

    // 3. 双重关闭保证机制
    @Override
    @PreDestroy  // Spring容器销毁前调用
    public void destroy() {
        stopNettyServer();
    }

    // [资源释放关键] 安全关闭方法
    private void stopNettyServer() {
        log.info("正在关闭 Netty server...");
        if (serverChannel != null) {
            // [内存泄漏防护] 先关闭channel再释放线程组
            serverChannel.close().awaitUninterruptibly();
            log.info("已关闭所有 Netty channel");
        }
        // [优雅关闭] 按顺序关闭线程组
        log.info("正在优雅按序释放线程组...");
        workerGroup.shutdownGracefully().syncUninterruptibly();
        bossGroup.shutdownGracefully().syncUninterruptibly();
        log.info("Netty server 所有资源已释放");
    }
}