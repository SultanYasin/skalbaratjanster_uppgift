package org.example;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.http.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ReverseProxyHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final ReverseProxyServer reverseProxyServer;
    private Channel nodeChannel;
    private Node nextNode;
    public ReverseProxyHandler(ReverseProxyServer reverseProxyServer){
        this.reverseProxyServer = reverseProxyServer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx){
        var clientChannel = ctx.channel();
        System.out.printf("Server connected to client | ConnectionID: %s\n", clientChannel.id());

        nextNode = reverseProxyServer.getNodeHandler().getNextNode();
        var bootstrap = new Bootstrap();
        try{
            this.nodeChannel = bootstrap
                    .group(reverseProxyServer.getWorkerGroup())
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            var pipeline = socketChannel.pipeline();
                            pipeline.addLast(new ClientHandler(clientChannel, nextNode));
                        }
                    })
                    .connect("localhost", nextNode.getPort()).sync().channel();

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
        System.out.printf("Server received a message from client | ConnectionID: %s\n", ctx.channel().id());
        CacheKey cacheKey = SimpleCache.getCacheKeyFromByteBuf(buf.copy());
        SimpleCache.storeChannel(ctx.channel().id().toString(), cacheKey);
        ByteBuf cachedBuf = SimpleCache.getBuf(cacheKey);
        if(cachedBuf != null){
            ctx.channel().writeAndFlush(cachedBuf.copy());
            System.out.printf("Cached response for path %s\n", cacheKey.path);
        }
        nodeChannel.writeAndFlush(buf.copy());
        System.out.printf("Server forwards message to node(%d) | ConnectionID: %s\n", nextNode.getNodePId(), nodeChannel.id());
        reverseProxyServer.getNodeHandler().increaseNodeRequest(nextNode.getNodePId(), nodeChannel.id().toString());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.printf("Connection to client inactive | ConnectionID: %s\n", ctx.channel().id());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.printf("Error in %s, ReverseProxyHandler\n", ctx.channel().id());
        cause.printStackTrace();
        ctx.channel().close();
    }
}
