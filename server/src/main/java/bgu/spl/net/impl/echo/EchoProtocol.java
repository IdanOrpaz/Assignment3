package bgu.spl.net.impl.echo;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.srv.Connections;

import java.time.LocalDateTime;

public class EchoProtocol implements BidiMessagingProtocol<byte[]> {

    private boolean shouldTerminate = false;
    private int connectionId = 0;
    private Connections<byte[]> connections = null;
    
    public void start(int connectionId, Connections<byte[]> connections) {
        // Implement the start method
        this.connectionId = connectionId;
        this.connections = connections;
    }


    @Override
    public void process(byte[] msg) {
        System.out.println("[" + LocalDateTime.now() + "]: " + msg);
    }


    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }
}

