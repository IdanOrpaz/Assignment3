package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.BidiMessagingProtocol;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;
import bgu.spl.net.impl.tftp.ConnectionsImpl;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseServer<T> implements Server<T> {


    private final int port;
    private final Supplier<BidiMessagingProtocol<T>> protocolFactory;
    private final Supplier<MessageEncoderDecoder<T>> encdecFactory;
    private ServerSocket sock;
    private Connections<T> connections;
    private AtomicInteger idCounter;

    public BaseServer(
            int port,
            Supplier<BidiMessagingProtocol<T>> protocolFactory,
            Supplier<MessageEncoderDecoder<T>> encdecFactory) {
            
        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
		this.sock = null;

        // FIELDS WE ADDED TO SUPPORT THE NEW INTERFACES
        this.connections = new ConnectionsImpl<>();
        this.idCounter = new AtomicInteger(0);
    }

    @Override
    public void serve() {

        try (ServerSocket serverSock = new ServerSocket(port)) {
			System.out.println("Server started");

            this.sock = serverSock; //just to be able to close

            while (!Thread.currentThread().isInterrupted()) {

                Socket clientSock = serverSock.accept();
                BidiMessagingProtocol<T> protocolInstance = protocolFactory.get();

                int id = idCounter.getAndIncrement();
                protocolInstance.start(id, connections); // INITIALIZE THE PROTOCOL
                BlockingConnectionHandler<T> handler = new BlockingConnectionHandler<>(
                        clientSock,
                        encdecFactory.get(),
                        protocolInstance
                );
                connections.connect(id, handler); // UPDATE the client as active
                execute(handler);
            }

        } catch (IOException ex) {
        }

        System.out.println("server closed!!!");
    }

    @Override
    public void close() throws IOException {
		if (sock != null)
			sock.close();
    }

    protected abstract void execute(BlockingConnectionHandler<T>  handler);

}
