package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpListeningThread implements Runnable {


//-----------------------Fields----------------------------//
    private final TftpProtocol protocol;
    private final TftpEncoderDecoder encdec;
    private final Socket sock;
    BufferedInputStream in;
    BufferedOutputStream out;
    boolean shouldTerminate = false;
    private volatile boolean connected = true;
    

//----------------------Constructor------------------------//
    public TftpListeningThread(BufferedInputStream in, BufferedOutputStream out, Socket sock, TftpEncoderDecoder encdec, TftpProtocol protocol) {
        this.in = in;
        this.out = out;
        this.sock = sock;
        this.encdec = encdec;
        this.protocol = protocol;
    }

//---------------------Methods------------------------------//
    public void run(){

        try (Socket sock = this.sock) { //just for automatic closing
            int read;

            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                // System.out.println(read);
                byte[] nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    byte[] response = protocol.process(nextMessage);
                    if (response != null) {
                        out.write(encdec.encode(response));
                        out.flush();
                    }
                }
            }}
         catch (IOException ex) {
            ex.printStackTrace();}
        }


    boolean shouldTerminate(){
        return shouldTerminate;
    }
}