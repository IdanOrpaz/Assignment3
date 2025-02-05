// package bgu.spl.net.impl.echo;

// import java.io.BufferedReader;
// import java.io.BufferedWriter;
// import java.io.IOException;
// import java.io.InputStreamReader;
// import java.io.OutputStreamWriter;
// import java.net.Socket;

// public class EchoClient {

//     public static void main(String[] args) throws IOException {

//         if (args.length == 0) {
//             args = new String[]{"localhost", "hello"};
//         }

//         if (args.length < 2) {
//             System.out.println("you must supply two arguments: host, message");
//             System.exit(1);
//         }

//         // create socket and BufferedReader and BufferedWriter. 
//         //BufferedReader and BufferedWriter automatically using UTF-8 encoding
//         try (Socket sock = new Socket(args[0], 7777);
//                 BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
//                 BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()))) {

//             //create 2 Threads.
//             Thread keyboarThread = new TftpKeyboardThread();
//             Thread listeningThread = new TftpLosteningThread();
            
//         }
//     }
// }
