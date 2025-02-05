package bgu.spl.net.impl.bidiecho;
import bgu.spl.net.api.BidiMessagingProtocol;
import java.time.LocalDateTime;
import bgu.spl.net.srv.Connections;
import java.util.concurrent.ConcurrentHashMap;;

// EXAMPLE IMPLEMENTATION FOR BIDIRECTIONAL FOR ECHO. THE IDEA - BIDIRECITONATL


// keeping ids to booleans , when I do start i write it has started

class holder {
    static ConcurrentHashMap<Integer, Boolean> ids_login = new ConcurrentHashMap<>();
}



public class bidiEchoProtocol implements BidiMessagingProtocol<String> {

    private boolean shouldTerminate = false;
    private int connectionId;
    private Connections<String> connections;

    @Override
    public void start(int connectionID, Connections<String> connections) {
        this.shouldTerminate = false;
        this.connectionId = connectionId;
        this.connections = connections;

    }

    @Override
    public void process(String msg) {
        shouldTerminate = "bye".equals(msg);
        System.out.println("[" + LocalDateTime.now() + "]:" + msg);
        for (Integer k: holder.ids_login.keySet()) {
            connections.send(k, createEcho(msg));
        }
        // shouldTerminate = "bye".equals(msg);
        // System.out.println("[" + LocalDateTime.now() + "]: " + msg);
        // return createEcho(msg);
        //TODO: implement process method
    }

    private String createEcho(String message) {
        String echoPart = message.substring(Math.max(message.length() - 2, 0), message.length());
        return message + " .. " + echoPart + " .. " + echoPart + " ..";
    }

    @Override
    public boolean shouldTerminate() {
        connections.disconnect(connectionId);
        holder.ids_login.remove(this.connectionId);
        return shouldTerminate;
    }
}