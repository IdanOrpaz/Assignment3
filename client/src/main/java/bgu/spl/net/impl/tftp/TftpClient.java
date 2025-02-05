package bgu.spl.net.impl.tftp;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
public class TftpClient {


    //TODO: implement the main logic of the client, when using a thread per client the main logic goes here
    public static void main(String[] args) {


    if (args.length == 0) {
        args = new String[]{"localhost", "7777"};
    }

    if (args.length != 2) {
        System.out.println("Usage: java TftpClient <server address> <port>");
        return;
    }

    String adressOfServer = args[0];
    int portOfServer = Integer.parseInt(args[1]);

    // connect to server 
    // start the 2 THREADS 
    try (Socket sock = new Socket(adressOfServer, portOfServer)){
                BufferedInputStream in = new BufferedInputStream(sock.getInputStream());
                BufferedOutputStream out = new BufferedOutputStream(sock.getOutputStream());
                 
        
        TftpEncoderDecoder encdec = new TftpEncoderDecoder();
        TftpProtocol protocol = new TftpProtocol();
 

        // Create and start a thread for reading user input and sending commands
        Thread keyboardThread = new Thread(new TftpKeyboardThread(out, encdec, protocol));
        keyboardThread.start();
       
        // Create and start a thread for listening to server responses
        Thread listeningThread = new Thread(new TftpListeningThread(in, out , sock, encdec, protocol));
        listeningThread.start();       


        // Wait for both threads to finish
        keyboardThread.join();
        listeningThread.join();

    } catch (Exception e) {
    e.printStackTrace();}

    }
}

