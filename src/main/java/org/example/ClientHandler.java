package org.example;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

//Hanterar kommunikationen mellan proxyn och tjänsterna
public class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final Channel originalChannel;

    public ClientHandler(Channel channel) {
        this.originalChannel = channel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
        originalChannel.writeAndFlush(byteBuf.copy()); //Vi behöver kopiera och skicka nya objekt då Netty raderar objekt osv.
        channelHandlerContext.channel().close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().close();
        originalChannel.close();
    }
}
