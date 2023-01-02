package org.example;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NodeHandler {

    private Map<Long, Node> startingQueue;
    private Map<Long, Node> activeNodes;
    private Map<Long, Node> closingQueue;

    private int portCounter = 8080;

    private final ReentrantReadWriteLock lock;
    private boolean alive = true;

    private int minAmountOfNodes;
    private int runCheckerInterval;

    public NodeHandler(){
        this.startingQueue = new HashMap<>();
        this.activeNodes = new HashMap<>();
        this.closingQueue = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.minAmountOfNodes = 3;
        this.runCheckerInterval = 3;
        nodeInitializer(minAmountOfNodes);
        startThread();
    }

    private void nodeInitializer(int nrOfNodesToInit){
        for(int i = 0; i < nrOfNodesToInit; i++){
            try{
                initNode(portCounter);
                portCounter++;
            } catch (IOException ioe){
                System.out.printf("Couldn't init a node on port %d\n", portCounter);
                ioe.printStackTrace();
            }
        }
    }

    private void initNode(int port) throws IOException {
        try {
            lock.writeLock().lock();
            Node node = new Node(port).start();
            startingQueue.put(node.getNodePId(), node);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void destroyAllNodes(){
        for(Long nodeId : startingQueue.keySet()){
            startingQueue.get(nodeId).stop();
        }
        startingQueue.clear();
        for(Long nodeId : activeNodes.keySet()){
            activeNodes.get(nodeId).stop();
        }
        activeNodes.clear();
        for(Long nodeId : closingQueue.keySet()){
            closingQueue.get(nodeId).stop();
        }
        closingQueue.clear();
    }

    public void increaseNodeRequest(long nodeId, String nodeChannelId){
        try {
            lock.writeLock().lock();
            activeNodes.get(nodeId).increaseRequests(nodeChannelId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Node getNextNode(){
        try {
            lock.writeLock().lock();
            if (activeNodes.isEmpty()) return null;

            Node node = null;

            for (Long nodeId : activeNodes.keySet()) {
                Node collectedNode = activeNodes.get(nodeId);
                if (node == null) node = collectedNode;
                else if (collectedNode.getTotalRequests() < node.getTotalRequests()) {
                    node = collectedNode;
                }
            }
            return node;
        }finally {
            lock.writeLock().unlock();
        }
    }

    private void checker(){
        closingQueue.forEach((key, value) -> {
            if (value.connectionsIsEmpty()){
                value.stop();
            }
        });

        List<Long> nullableNodes = new ArrayList<>();
        List<Long> movableNodes = new ArrayList<>();
        startingQueue.forEach((key, value) -> {
            if (value.getStartInstant() == null || value.getStartInstant().plusSeconds(15).compareTo(Instant.now()) < 0){
                nullableNodes.add(key);
            }else{
                try {
                URL url = new URL("http://localhost:" + value.getPort() + "/alive");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setConnectTimeout(300);
                con.setReadTimeout(300);
                int responseCode = con.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK){
                    movableNodes.add(key);
                }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        for(Long nodeId : nullableNodes){
            try {
                lock.writeLock().lock();
                startingQueue.get(nodeId).stop();
                startingQueue.remove(nodeId);
                System.out.printf("Startup for node %d failed. Sent to destruction.\n", nodeId);
            } finally {
                lock.writeLock().unlock();
            }
        }

        for(Long nodeId : movableNodes){
            try {
                lock.writeLock().lock();
                activeNodes.put(nodeId, startingQueue.get(nodeId));
                startingQueue.remove(nodeId);
                System.out.printf("Startup for node %d is finished. Moved from startingQueue to activeNodes.\n", nodeId);
            } finally {
                lock.writeLock().unlock();
            }
        }

        var requests = 0.0;
        var rps = 0.0;
        requests = activeNodes.entrySet().stream().reduce(0, (subtotal, entry) -> subtotal + entry.getValue().getRequests(), Integer::sum);
        rps = requests / (double)(activeNodes.size() + startingQueue.size()) / runCheckerInterval;

        if(rps < 1.0 && activeNodes.size() > minAmountOfNodes){
            System.out.printf("Requests per second is less than 1(%f). Amount of nodes >= %d. Closing a node.\n", rps, minAmountOfNodes);
            closeFirstNode();
        }else if(rps > 1.0 || activeNodes.size()+startingQueue.size() < minAmountOfNodes){
            System.out.printf("Requests per second is greater than 5(%f). Initializing a node.\n", rps);
            nodeInitializer(1);
        }
        activeNodes.forEach((key, value) -> {value.resetRequests();});
    }

    private void closeFirstNode(){
        var firstNode = activeNodes.entrySet().stream().findFirst().orElse(null);

        if(firstNode != null){
            try {
                lock.writeLock().lock();
                closingQueue.put(firstNode.getKey(), firstNode.getValue());
                activeNodes.remove(firstNode.getKey());
                System.out.printf("Node %d is redundant. Sent to destruction by moving from activeNodes to closingQueue.\n", firstNode.getKey());
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    private void startThread() {
        var thread = new Thread(this::runThread);
        thread.start();
    }

    private void runThread() {
        try {
            lock.writeLock().lock();
            if (!alive) {
                return;
            }
            checker();
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            lock.writeLock().unlock();
        }

        try {
            int millis = runCheckerInterval * 1000;
            Thread.sleep(millis);
        } catch(Exception e) {
            e.printStackTrace();
        }
        runThread();
    }

    public void killCheckerThread(){
        alive = false;
        System.out.println("Checker thread is killed");
    }
}
