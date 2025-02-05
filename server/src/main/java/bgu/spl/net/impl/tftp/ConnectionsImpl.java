package bgu.spl.net.impl.tftp;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;
import java.util.concurrent.ConcurrentHashMap;


public class ConnectionsImpl <T> implements Connections <T> {

//---------------fields-----------------------//

private ConcurrentHashMap<Integer, ConnectionHandler<T>> connections;


//-----------------constructor---------------//

    public ConnectionsImpl () {
        connections = new ConcurrentHashMap<>();
    }


//-------------------methods-------------------//


    public boolean connect(int connectionId, ConnectionHandler<T> handler){
        synchronized (this) {
            // If the handler already exists , don't re-add it
        for (ConnectionHandler<T> value : connections.values()) {
            if (value.equals(handler)) {
                return false;
            }
        }
        connections.put(connectionId, handler); // otherwise add as active client to the map
        return true;
        } 
    }


    // send message T to the client represented by connectionId.
    public boolean send(int connectionId, T msg) {
        synchronized (this) {
            ConnectionHandler<T> clientToMessage = connections.get(connectionId);
            if (clientToMessage != null) {
                clientToMessage.send(msg);
                return true;
        }
        return false;

        }  
    }

    public void disconnect(int connectionId){
        synchronized (this) {
            connections.remove(connectionId); // remove an active client from the map
        }  
    }
}
