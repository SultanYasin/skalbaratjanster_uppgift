package org.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NodeHandler {

    private static NodeHandler INSTANCE = null;
    public static NodeHandler getInstance(){
        if(INSTANCE == null){
            INSTANCE = new NodeHandler();
        }
        return INSTANCE;
    }

    private int port;
    private final List<Node> activeNodes;
    private final int minimumNodeCount;

    private final ReentrantReadWriteLock lock;
    private boolean alive = true;

    private NodeHandler() {
        this.port = 8080;
        this.activeNodes = new ArrayList<>();
        this.minimumNodeCount = 3;

        this.lock = new ReentrantReadWriteLock();

        this.start(minimumNodeCount);
    }

    public void start(int amountOfNodes){
        for(int i = 0; i < amountOfNodes; i++){
            var node = new Node(port++);
            activeNodes.add(node);

            try{
                node.start();
            }catch (Exception e){
                e.printStackTrace();
                i--;
            }
        }
    }

    public Node next() {
        if(activeNodes.isEmpty()){
            return null;
        }

        Node node = activeNodes.get(0);
        System.out.println("------------------------------------------");
        for (Node n:activeNodes) {
            if(n.getRequests() < node.getRequests()){
                node = n;
            }
            System.out.println("Node:" + n.getPort() + " | " + n.getRequests());
        }

        node.setRequests(node.getRequests() + 1);
        if(node.getRequests() > 3){
            close(node);
            start(1);
            return next();
        }
        return node;
    }

    public void close(Node node){
        node.stop();
        activeNodes.remove(node);
    }

}
