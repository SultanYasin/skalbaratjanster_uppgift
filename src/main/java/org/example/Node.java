package org.example;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Node {
    private final int port;
    private Process process;
    private int requests;
    private int totalRequests;

    private List<String> connections;

    public Node(int port){
        this.port = port;
        this.requests = 0;
        this.totalRequests = 0;
        this.connections = new ArrayList<>();
    }

    public Node start() throws IOException {
        var args = new String[]{
                "java",
                "-jar",
                "spring.jar",
                "--server.port=" + port
        };

        this.process = Runtime
                .getRuntime().exec(args);

        System.out.printf("Node %d created on port %d\n", process.pid(), port);

        return this;
    }

    public void stop(){
        if (process == null){
            return;
        }
        process.destroy();
        if (process.isAlive()) {
            process.destroyForcibly();
        }
        System.out.printf("Node '%d' on port %d destroyed\n", process.pid(), port);
        process = null;
    }

    public void increaseRequests(String nodeChannelId){
        System.out.printf("Node '%d' on port %d received a request on channel %s\n", process.pid(), port, nodeChannelId);
        connections.add(nodeChannelId);
        this.requests++;
        this.totalRequests++;
    }

    public void removeConnection(String nodeChannelId){
        connections.remove(nodeChannelId);
    }

    public boolean connectionsIsEmpty(){
        return connections.isEmpty();
    }

    public void resetRequests(){
        this.requests = 0;
    }
    public int getRequests(){
        return this.requests;
    }
    public int getTotalRequests(){
        return this.totalRequests;
    }
    public int getPort(){
        return this.port;
    }
    public long getNodePId(){
        return process.pid();
    }

    public Instant getStartInstant() {
        return process.info().startInstant().orElse(null);
    }

    public boolean processFinished(){
        return !process.isAlive();
    }

}
