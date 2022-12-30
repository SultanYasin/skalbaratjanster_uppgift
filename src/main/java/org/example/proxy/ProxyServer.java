package org.example.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.example.NodeHandler;

import java.util.Scanner;

public class ProxyServer {


    private final int port;
    private final EventLoopGroup bossGroup, workerGroup;
    private NodeHandler nodeHandler;

    public ProxyServer(int port) {
        this.port = port;
        this.bossGroup = new NioEventLoopGroup();
        this.workerGroup = new NioEventLoopGroup();
        //När vi skapar vår ReverseProxy skapar vi också en NodeHandler instans. NodeHandler är en singleton och det kommer endast finnas en enda instans av nodehandler
        //oavsett om vi hämtar den genom vårt proxyserver objekt eller genom den statiska metoden getInstance i NodeHandler.
        //Vi vill endast ha en instans då vår nodehandler måste hålla koll på alla nodes. Skulle vi ha en nodehandler som håller koll på några nodes och en till som håller koll på
        //några andra så blir programmet mer komplext och vi skulle behöva spara alla våra nodehandlers i en lista eller liknande.
        //Vi kommer att köra vår nodehandler på en annan tråd och för att bygga ett program som snabbare klarar av fler skulle man kunna ha flera nodehandlers på olika trådar.
        this.nodeHandler = NodeHandler.getInstance();
    }

    public void start(){
        var bootstrap = new ServerBootstrap();

        try{
            var channel = bootstrap
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    //I vår childhandler bygger vi vår pipeline. Alltså bestämmer vad som ska hända när vi tar emot en request.
                    //Vår request kommer att gå igenom våra olika stopp. Varje stopp skapar vi med addLast på vårt pipelineobjekt.
                    //Då vi har så få händelser och egentligen bara ett enda(utöver Nettys egna) stopp har jag valt att bygga pipelinen som en anonym-klass.
                    .childHandler(new ChannelInitializer<SocketChannel>(){
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            var pipeline = socketChannel.pipeline();
                            pipeline.addLast(new ProxyHandler(ProxyServer.this));
                        }
                    })
                    .bind(port).sync().channel();

            /*Följande kod är endast till för att hålla programmet igång tills vi aktivt väljer att avsluta.
            Så länge scanner inte hittar strängen "exit" i terminalen fortsätter programmet körs vår whileloop. När vi hittar exit bryts loopen och
            vi stänger ner våra eventloops, vår server och våra skapade nodes. */
            var scanner = new Scanner(System.in);
            while (!scanner.nextLine().equals("exit")){}
            channel.close();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public EventLoopGroup getWorkerGroup(){
        return workerGroup;
    }

}
