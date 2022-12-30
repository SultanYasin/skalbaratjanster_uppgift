package org.example.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.example.ClientHandler;
import org.example.NodeHandler;

public class ProxyHandler extends SimpleChannelInboundHandler<ByteBuf> {


    private final ProxyServer proxyServer;
    private Channel nodeChannel;

    public ProxyHandler(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        var clientChannel = ctx.channel();

        var bootstrap = new Bootstrap(); //Startar en clientbootstrap då vi är klienten i detta fallet.

        var node = NodeHandler.getInstance().next();

        try{
            this.nodeChannel = bootstrap
                    .group(proxyServer.getWorkerGroup())
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            var pipeline = socketChannel.pipeline();
                            pipeline.addLast(new ClientHandler(clientChannel));
                        }
                    }).connect("localhost", node.getPort()).sync().channel();

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        nodeChannel.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
        nodeChannel.writeAndFlush(buf.copy());
    }

    //Om det blir något fel så stänger vi kontakten till vår klient och node.
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.channel().close();
        nodeChannel.close();
    }
}
