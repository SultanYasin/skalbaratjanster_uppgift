package org.example;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpServerCodec;

import java.util.Scanner;

public class ReverseProxyServer {

    private final int reverseProxyPort;
    private final EventLoopGroup bossGroup, workerGroup;
    private final NodeHandler nodeHandler;

    public ReverseProxyServer(int port) {
        System.out.printf("Server created\n", port);
        this.reverseProxyPort = port;
        this.bossGroup = new NioEventLoopGroup();
        this.workerGroup = new NioEventLoopGroup();
        this.nodeHandler = new NodeHandler();
    }

    public void start() {
        var bootstrap = new ServerBootstrap();

        try {
            var channel = bootstrap
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            var pipeline = socketChannel.pipeline();
                            pipeline.addLast(new ReverseProxyHandler(ReverseProxyServer.this));
                        }
                    })
                    .bind(reverseProxyPort).sync().channel();
            System.out.printf("Server initialized on port %d\n", reverseProxyPort);

            var scanner = new Scanner(System.in);
            while (!scanner.nextLine().equals("exit")) {}
            System.out.println("Shutting down server");
            nodeHandler.killCheckerThread();
            channel.close();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            nodeHandler.destroyAllNodes();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public EventLoopGroup getWorkerGroup(){
        return workerGroup;
    }

    public NodeHandler getNodeHandler() { return nodeHandler; }

}
