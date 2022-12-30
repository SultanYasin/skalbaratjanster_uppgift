package org.example;

import org.example.proxy.ProxyServer;

public class Main {
    public static void main(String[] args) {
        //Vi startar vår ReverseProxy-tjänst genom att skapa ett nytt ProxyServer-objekt och kör metoden start.
        //Proxyn tar emot ett int som motsvarar den porten vi kommer starta vår tjänst på.
        new ProxyServer(5000).start();
    }
}