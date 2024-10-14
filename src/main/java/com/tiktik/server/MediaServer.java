package com.tiktik.server;

import java.net.InetSocketAddress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.unix.PreferredDirectByteBufAllocator;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MediaServer {

	@Autowired
	private FlvHandler flvHandler;

    public void start(InetSocketAddress socketAddress) {
        // 创建一个用于接收连接的主线程组 (bossGroup)，线程数为1
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // 创建一个用于处理连接的工作线程组 (workGroup)，线程数为200
        EventLoopGroup workGroup = new NioEventLoopGroup(200);

        ServerBootstrap bootstrap = new ServerBootstrap()// 初始化服务器启动辅助类
                .group(bossGroup, workGroup) // 将主线程组和工作线程组传递给启动类
                .channel(NioServerSocketChannel.class) // 指定使用 NIO 传输类型的通道，是通道标识，这个代码是客户端还是服务器
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception { // 初始化子通道处理器
                        // 配置 CORS (跨域资源共享) 设置，允许任意源访问
                        CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().allowNullOrigin().allowCredentials().build();
                        // 获取该子通道的管道，并添加一系列处理器到其中
                        socketChannel.pipeline()
                                .addLast(new HttpResponseEncoder()) // 添加 HTTP 响应编码器
                                .addLast(new HttpRequestDecoder()) // 添加 HTTP 请求解码器
                                .addLast(new ChunkedWriteHandler()) // 添加分块写处理器以支持大文件传输
                                .addLast(new HttpObjectAggregator(64 * 1024)) // 聚合 HTTP 消息并限制最大消息大小为 64KB
                                .addLast(new CorsHandler(corsConfig)) // 添加跨域处理器
                                .addLast(flvHandler); // 添加自定义的 FLV 处理器，负责处理 FLV 流
                    }
                })
                .localAddress(socketAddress) // 设置服务器监听的地址和端口
                .option(ChannelOption.SO_BACKLOG, 128) // 设置等待连接的队列大小为 128
                .option(ChannelOption.ALLOCATOR, PreferredDirectByteBufAllocator.DEFAULT) // 优先使用直接内存分配
                // 两小时内没有数据的通信时,TCP会自动发送一个活动探测数据报文
                .childOption(ChannelOption.TCP_NODELAY, true) // 禁用 Nagle 算法以减少延迟
                .childOption(ChannelOption.SO_KEEPALIVE, true) // 保持长连接，启用 TCP 保活机制
                .childOption(ChannelOption.SO_RCVBUF, 128 * 1024) // 设置接收缓冲区大小为 128KB
                .childOption(ChannelOption.SO_SNDBUF, 1024 * 1024) // 设置发送缓冲区大小为 1MB
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1024 * 1024 / 2, 1024 * 1024)); // 设置写缓冲水位线
        //绑定端口,开始接收进来的连接
        try {
            ChannelFuture future = bootstrap.bind(socketAddress).sync(); // 绑定端口，阻塞启动服务器，等待启动完毕才会进入下行代码
            future.channel().closeFuture().sync(); // 阻塞等待通道关闭才会进入下行代码
        } catch (InterruptedException e) {
            e.printStackTrace(); // 捕获中断异常并打印堆栈跟踪
        } finally {
            bossGroup.shutdownGracefully(); //关闭主线程组
            workGroup.shutdownGracefully(); //关闭工作线程组
        }
    }
}