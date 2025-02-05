package bgu.spl.net.impl.bidiecho;
import bgu.spl.net.srv.Server;

public class bidiEchoServer {

    public static void main(String[] args) {

        // STOPPED AT 24 MINUTES
        // Connection holds connection handler

        // // you can use any server... 
        // Server.threadPerClient(
        //         7777, //port
        //         () -> new EchoProtocol(), //protocol factory
        //         bidiEncoderDecoder::new //message encoder decoder factory
        // ).serve();

    }

    // NOT COMPILATING: NEW SIGNATURE , 2 OPTIONS - EDIT THE CURRENT CODE OF THE SERVER , OR DUPLICATE \ CHANGE NAMES
    // AS RUNNING COMMAND REFERS JUST TO TFTPSERVER
    // WE CAN CHANGE THE INTERFACE IN THE ASSIGNMENT , INCLUDING THE SERVER
}
