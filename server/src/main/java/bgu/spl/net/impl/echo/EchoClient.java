// package bgu.spl.net.impl.echo;

// import java.io.BufferedOutputStream;
// import java.io.BufferedReader;
// import java.io.BufferedWriter;
// import java.io.IOException;
// import java.io.InputStreamReader;
// import java.io.OutputStreamWriter;
// import java.net.Socket;
// import java.nio.charset.StandardCharsets;

// public class EchoClient {

//     public static void main() throws IOException {

//         // if (args.length == 0) {
//         //     args = new String[]{"localhost", "hello"};
//         // }

//         // if (args.length < 2) {
//         //     System.out.println("you must supply two arguments: host, message");
//         //     System.exit(1);
//         // }

//         // byte[] x = new byte[0];
//         // byte[] y = new byte[7];

//         // // Initialize resources inside try-with-resources
//         // try (Socket sock = new Socket(args[0], 7777);
//         //         BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
//         //         BufferedOutputStream out = new BufferedOutputStream(sock.getOutputStream())) {

//         //         out.write(x);
//         //         out.write(y);

//         // } // Resources will be automatically closed here


//         // String username = "Amit";

//         // // Convert username to bytes using UTF-8 encoding
//         // byte[] usernameBytes = username.getBytes(StandardCharsets.UTF_8);

//         // // Append zero byte to terminate the username
//         // byte[] logrqMessage = new byte[usernameBytes.length + 2];
//         // logrqMessage[0] = 0; // Opcode for LOGRQ
//         // logrqMessage[1] = 7; // Opcode value
//         // System.arraycopy(usernameBytes, 0, logrqMessage, 2, usernameBytes.length);

//         // try (Socket sock = new Socket("127.0.0.1", 7777);
//         //         BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
//         //         BufferedOutputStream out = new BufferedOutputStream(sock.getOutputStream())) {

//         //     // Send LOGRQ message to the server
//         //     out.write(logrqMessage);
//         //     out.flush();

//             // Read response from the input stream
//             // String line = in.readLine();
//             // System.out.println("message from server: " + line);

//         }
//     }
// }
