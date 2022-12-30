package org.example;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

@Getter
public class Node {

    private final int port;
    private Process process;
    @Setter
    private int requests;

    public Node(int port){
        this.port = port;
        this.requests = 0;
    }

    public void start() throws IOException {
        var args = new String[]{
                "java",
                "-jar",
                "spring.jar",
                "--server.port=" + port
        };

        this.process = Runtime
                .getRuntime().exec(args);
    }

    public void stop(){
        if (process == null){
            return;
        }
        process.destroy();
        process = null;
    }

}
