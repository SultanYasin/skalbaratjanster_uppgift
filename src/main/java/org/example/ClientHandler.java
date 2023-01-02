package org.example;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final Channel clientChannel;
    private final Node node;

    public ClientHandler(Channel clientChannel, Node node) {
        this.clientChannel = clientChannel;
        this.node = node;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx){
        System.out.printf("Server connected to node(%d) %s | ConnectionID: %s\n", node.getNodePId() ,ctx.channel().remoteAddress(), ctx.channel().id());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        System.out.printf("Server received message from node(%d) | ConnectionID: %s\n",node.getNodePId(), ctx.channel().id());
        boolean respond = SimpleCache.storeCache(clientChannel.id().toString(), byteBuf.copy());
        if(respond){
            clientChannel.writeAndFlush(byteBuf.copy());
        }
        System.out.printf("Server forwards message to client | ConnectionID: %s\n", clientChannel.id());
        ctx.channel().close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.printf("Connection to node(%d) inactive | ConnectionID: %s\n", node.getNodePId(), ctx.channel().id());
        node.removeConnection(ctx.channel().id().toString());
        clientChannel.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.printf("Error in %s, ClientHandler\n", ctx.channel().id());
        cause.printStackTrace();
        ctx.channel().close();
        clientChannel.close();
    }

}
