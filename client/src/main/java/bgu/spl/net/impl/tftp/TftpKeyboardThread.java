package bgu.spl.net.impl.tftp;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

public class TftpKeyboardThread implements Runnable {
// -reads commands from the keyboard:
// 1. check if command is a legitimate command.
// if command is ok 
// -sends packets to the server defined by the command:
// 1. find out what request has been inserted.
// 2. use encoder decoder to create the byte message. 
// 3. send the message.
// 4. wait for Listening Thread to return an answer.


//-------------------fields-------------//

    private static final String[] legalCommands = {"LOGRQ", "DELRQ", "RRQ", "WRQ", "DIRQ", "DISC"};
    private int opcode;
    private String message;
    private boolean isLoggedIn = false;

    // fields copied from connectionHandler:
    private final TftpProtocol protocol;
    private final TftpEncoderDecoder encdec;
    private Scanner scanner;
    // private final Socket sock;
    
    private BufferedOutputStream out;


    //-----------------------constructor---------------------------//
    public TftpKeyboardThread(BufferedOutputStream out, TftpEncoderDecoder encdec, TftpProtocol protocol){
        this.out = out;
        this.encdec = encdec;
        this.protocol= protocol;
    }

    //-------------------------methods---------------------------------//
    public void run(){
        while(!protocol.shouldTerminate()){
            scanner = new Scanner(System.in);
            // recieve command
            String userInput = scanner.nextLine();

            // check of command is legal, if so, save it. if not, print illegal command
           if (handleInputAndSetCommand(userInput)){
                
            // process the packet
                byte[] packetToSend = createPacketForCommand(opcode);
                
                if (packetToSend.length == 1){}
                else{
                    // send the packet
                    System.out.println("sending message to server");
                    try{
                        out.write(packetToSend);
                        out.flush();
                    }catch(IOException e) {}
                    if(!protocol.shouldTerminate()){
                        // wait for listening protocol to let write again 
                        synchronized(protocol.threadLock){
                            try{
                            protocol.threadLock.wait();}
                            catch(InterruptedException e) {}
                        }
                    }
                }
            }
        }

        // Tell Thread to wait for Listening Thread until you can start writing commands again.
        scanner.close();

    }



//-----------------------------------Our Methods-----------------------------------//

    // check of command is legal, if so, saves it. if not, prints illegal command
    private boolean handleInputAndSetCommand(String userInput){
        boolean isLegal = false;
        int index = userInput.indexOf(" ", 2);
        if(index > 2){
            for (String legalCommand : legalCommands) {
                String commandToCheck = userInput.substring(0, index);
                if (legalCommand.equals(commandToCheck)) {
                    //save command
                    this.opcode = commandToByteOpcode(commandToCheck);
                    //save message
                    this.message = userInput.substring(index+1, userInput.length());
                    return isLegal = true;
                }
            }

        }
        else if (userInput.equals("DISC")){
            this.opcode = commandToByteOpcode(userInput);
            this.message = null;
            isLegal = true;
        }        
        else if (userInput.equals("DIRQ")){
            this.opcode = commandToByteOpcode(userInput);
            this.message = null;
            isLegal = true;
        }
        if (!isLegal){
            System.out.println("Illegal Command");} // The command is not legal
            return isLegal;
    }

//gets the string of the command and returns the command as byte[]
    private int commandToByteOpcode(String commandToCheck){
        switch (commandToCheck) {
            case "LOGRQ":
                return 7;
            case "DELRQ":
                return 8;
            case "RRQ":
                return 1;
            case "WRQ":
                return 2;
            case "DIRQ":
                return 6;
            case "DISC":
                return 10;
            default:
                return -1;
        }
    }

//creates the packet that needs to be sent to the server
private byte[] createPacketForCommand(int opcode){
    switch (opcode) {
        case 7://LOGRQ
                protocol.LogrqRequest = true;
            return concatenateByteArrays(new byte[] {0,7});
        case 8: //DELRQ
            return concatenateByteArrays(new byte[] {0,8});  
        case 1: // RRQ 
            //check if file exists, if it exists, dont send packet. if doesnt exist, create file.
            if(!checkFileExistence(message)){
                protocol.RrqRequest = true;
                protocol.targetFilePath = "./" + message;
                return concatenateByteArrays(new byte[] {0,1});}
            else{
                System.out.println("File Already Exists");
                return new byte[] {0};}
        case 2://WRQ
            // check if file exists, if it exists, send packet
            if(checkFileExistence(message)){
                protocol.WrqRequest = true;
                protocol.fileName = message;
                return concatenateByteArrays(new byte[] {0,2});}
            else{ 
                System.out.println("File does not Exist");
                return new byte[] {0};}
        case 6://DIRQ
                protocol.DirqRequest = true;
            return new byte[] {0,6};
        case 10://ACK
            protocol.discRequest = true;
            return new byte[] {0,10};
        default:
            return new byte[] {0};
    }
}

private byte[] concatenateByteArrays(byte[] opcodeByte) {
        ArrayList<byte[]> byteArrayList = new ArrayList<>();
        // add the opcode 
        byteArrayList.add(opcodeByte);
        // add the message
        byteArrayList.add(message.getBytes(StandardCharsets.UTF_8));
        // add 0 at the end 
        byteArrayList.add(new byte[] {0});

        int totalLength = byteArrayList.stream().mapToInt(byteArray -> byteArray.length).sum();
        byte[] resultBytes = new byte[totalLength];
        int currentIndex = 0;

        for (byte[] byteArray : byteArrayList) {
            System.arraycopy(byteArray, 0, resultBytes, currentIndex, byteArray.length);
            currentIndex += byteArray.length;
        }

        return resultBytes;
    }

    // method checks if the file is in the working directory and prepares transfer. If not, creates one 
    private boolean checkFileExistence(String fileName) {
            String directoryPath = "./";


            Path filePath = Paths.get(directoryPath, fileName);

            boolean fileExists = Files.exists(filePath);

            if (opcode == 2 && fileExists ){
                File file = new File("./"+fileName) ;
                //set values needed for WRQ
                
                try{
                protocol.fis = new FileInputStream(filePath.toString());
                protocol.channel = protocol.fis.getChannel();}
                catch(FileNotFoundException ignore){}
                protocol.uploadedFile = fileName;
               
            }
            // if(opcode == 1){
            //     protocol.targetFilePath = "./" + fileName;
            // }
            return fileExists; 
    }
    

}
