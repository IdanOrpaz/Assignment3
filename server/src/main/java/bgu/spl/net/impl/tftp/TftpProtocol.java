package bgu.spl.net.impl.tftp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;



class holder { 
    // TABLE OF THE LOGGED IN USERS 
    static ConcurrentHashMap<Integer, String>  logged_ids_new = new ConcurrentHashMap<>();
}

public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {

//-------------fields----------------//

    // INITIALIZATION PARAMETERS
    private boolean shouldTerminate = false;
    private int connectionId;
    private Connections<byte[]> connections;
    private File directory;

    // MANAGE MESSAGES IN "PROCESS"
    private boolean isLoggedIn;
    private byte[] opcodeByteArray;
    private int opcode;
    private String username; 
    private byte[] message;

    // MANAGE OUTGOING DATA
    private int blockNumber;
    private ByteArrayInputStream inputStream;
    private final int chunkSize = 512;
   
    // MANAGE INCOMING DATA
    private String targetFilePath;
    private String targetFileName;
    private ByteArrayOutputStream dataBuffer;


    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        // TODO implement this

        this.shouldTerminate = false;
        this.connectionId = connectionId;
        this.connections = connections;
        this.directory = new File("./Files" );
        this.isLoggedIn = false;
        this.opcode = -1;

        // MANAGE OUTGOING DATA
        this.blockNumber = 0;
   
        // MANAGE INCOMING DATA
        this.targetFilePath = "null";
        this.targetFileName = "null";
        this.dataBuffer = new ByteArrayOutputStream();

    }

    @Override
    public void process(byte[] message) {
        // TODO implement this

        //save the message for later use
        this.message = message;

        // extract the opcode
        byte[] msgOpcodeByteArray = {message[0],message[1]};
        this.opcodeByteArray = msgOpcodeByteArray;
        opcode = (short) ((( short ) opcodeByteArray [0]) << 8 | ( short ) ( opcodeByteArray [1]) & 0x00ff );

        //check if opecode isLegal. if so, continue. else, return ERROR 4
        if(!isLegalOpcode(opcode));

        // check if user is logged in
        else if(!isLoggedIn && opcode != 7){
            // if not, SEND ERROR 6
            sendErrors(6,"");}

        else if (opcode == 1){
            handleRRQ();}
        
        else if (opcode == 2){
            handleWRQ();}
 
        else if (opcode == 3){
            handleData();}

        else if (opcode == 4){
            sendData();
        }
        else if (opcode == 6){
            handleDirq();}
        
        else if (opcode == 7){
            handleLogRQ();}

        else if (opcode == 8){
            handleDelRQ();}

        else if (opcode == 10){
            handleDisc();}
    }

    @Override
    public boolean shouldTerminate() {
        // TODO implement this
        return this.shouldTerminate; // True if the connection should be terminated
    } 




//-------------------------------OUR METHODS--------------------------------//


// method checks if opecode is a legal opecode
    private boolean isLegalOpcode(int opcode){
        boolean legal = (opcode >= 1 && opcode <= 10);
        if (!legal){
            // SEND ERROR 4 - Unknown Opcode
            sendErrors(4, ""); 
            return legal;
        }
        return legal;
    }



    //method that handles LogRQ packet
    private void handleLogRQ() { 
        int indexOfZero = findNullTerminator(message);
        String possibleUsername = new String(message, 2 , indexOfZero - 2,  StandardCharsets.UTF_8); // is the UTF
        
        // Make sure the user is not logged in, and there is no existing user with the same name
        if((isLoggedIn)||(holder.logged_ids_new.contains(possibleUsername))){
            if(isLoggedIn)
                sendErrors(0, "User is Already Logged In");
            else{
                sendErrors(7 , "");
            }
        }
        else{
            // SUCCESSFUL LOG IN
            isLoggedIn = true;
            username = possibleUsername; 
            holder.logged_ids_new.put(connectionId, username);
            sendAck();
        }
    }


    // method that handles DELRQ packet
    private void handleDelRQ() {
        // create a string of the file name
        int indexOfZero = findNullTerminator(message);
        String fileNameToSearch = new String(message, 2 , indexOfZero - 2,  StandardCharsets.UTF_8); // is the UTF

        //look for the file in the directory
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                boolean fileFound = false;
                for (File file : files) {
                    if (file.isFile() && file.getName().equals(fileNameToSearch)) {
                        //file found, try to delete it
                        fileFound = true;
                        if (file.delete()) {
                            sendAck(); // SEND ACK
                            handleBcast(new byte[]{0}, fileNameToSearch); // notify all clients
                            break;
                        } else {
                            // not able to delete
                            // SEND ERROR 2 
                            sendErrors(2,"");
                            break;
                        }
                    }
                }
                if(!fileFound){ // file not found
                    // SEND ERROR 1
                    sendErrors(1,"");
                }
            }
        }
    }


// method that handles DIRC packet
    private void handleDirq() {

        File dirqDir = new File("./Files"); // create Files for File directory
        String[] filesList = dirqDir.list(); // insert file names into a string.

        // convert string array to byte array with 0 byte between each file name
        ArrayList<Byte> dirNamesInBytes = new ArrayList<>(); 
        for (String fileName : filesList){
            byte[] fileNameByte = fileName.getBytes(StandardCharsets.UTF_8);
            for(byte nextByte : fileNameByte){
                dirNamesInBytes.add(nextByte);
            }
            dirNamesInBytes.add((byte)0);
        }
        dirNamesInBytes.remove(dirNamesInBytes.size() - 1);

        // change byte array to byte[]
        byte[] byteArray = new byte[dirNamesInBytes.size()];
        for (int i = 0; i < dirNamesInBytes.size(); i++) {
            byteArray[i] = dirNamesInBytes.get(i);
        }

        process_byteArrayInput(byteArray); // Prepare the data transfer 
        sendData(); // Send the 1st DATA packet
        
    }


    // helper method- takes a List of byte[] and contantenates it's arguments into one byte[].
    private static byte[] concatenateByteArrays(List<byte[]> byteArrayList) {
        int totalLength = byteArrayList.stream().mapToInt(byteArray -> byteArray.length).sum();
        byte[] resultBytes = new byte[totalLength];
        int currentIndex = 0;

        for (byte[] byteArray : byteArrayList) {
            System.arraycopy(byteArray, 0, resultBytes, currentIndex, byteArray.length);
            currentIndex += byteArray.length;
        }
        return resultBytes;
    }


    // method that handles RRQ packet
    private void handleRRQ(){
        int indexOfZero = findNullTerminator(message); // create a string of the file name
        String fileNameToSearch = new String(message, 2 , indexOfZero - 2,  StandardCharsets.UTF_8); // is the UTF
        //look for the file in the directory
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                boolean fileFound = false;
                for (File file : files) {
                    if (file.isFile() && file.getName().equals(fileNameToSearch)) {
                        fileFound = true;
                        process_fileInput("./Files/" + fileNameToSearch); // Prepare the data transfer 
                        sendData(); // Send the 1st DATA packet
                        break;
                    }
                }
                if(!fileFound){
                    // SEND ERROR 1
                    sendErrors(1,"");
                }
            }
        }

    }



    // method that handles WRQ packet
    private void handleWRQ() {
        int indexOfZero = findNullTerminator(message); // create a string of the file name
        String fileNameToSearch = new String(message, 2 , indexOfZero - 2,  StandardCharsets.UTF_8);
        //look for the file in the directory
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                boolean fileFound = false;
                for (File file : files) {
                    if (file.isFile() && file.getName().equals(fileNameToSearch)) {
                        fileFound = true;
                        // SEND ERROR 5
                        sendErrors(5,"");
                        break;
                    }
                }
                if(!fileFound){
                    this.targetFileName = fileNameToSearch;
                    this.targetFilePath = "./Files/" + fileNameToSearch;
                    sendAck();
                }
            }
        }
    }


    // method that handles Errors. creates an Error packet ready to be sent to the client //
    private void sendErrors(int errorNum, String msgToWrite){
        switch (errorNum) {
            case 0:
                connections.send(connectionId, createErrorPacket(errorNum, msgToWrite));
                break;
            case 1:
                connections.send(connectionId,createErrorPacket(errorNum, "File not found. RRQ DELRQ of non-existing file."));
                break;
            case 2:
                connections.send(connectionId,createErrorPacket(errorNum, "Access violation. File cannot be written, read or deleted."));
                break;
            case 3:
                connections.send(connectionId,createErrorPacket(errorNum, "Disk full or allocation exceeded. No room in disk."));
                break;
            case 4:
                connections.send(connectionId,createErrorPacket(errorNum, "Illegal TFTP operation. Unknown Opcode."));
                break;
            case 5:
                connections.send(connectionId,createErrorPacket(errorNum, "File already exists. File name exists on WRQ."));
                break;
            case 6:
                connections.send(connectionId,createErrorPacket(errorNum, "User not logged in. Any opcode received before Login completes."));
                break;
            case 7:
                connections.send(connectionId,createErrorPacket(errorNum, "User already logged in. Login username already connected."));
                break;
        }
    }


    // method creates Packet for Errors
    private byte[] createErrorPacket(int errorNum, String errorMsg ){

        List<byte[]> byteToSend = new ArrayList<>();
        //add opcode to List
        byteToSend.add(new byte[]{0,5}); // error opcode
        //add ErrorCode to List
        byte [] errorNumByte = new byte []{( byte )  ( errorNum >> 8) , ( byte ) ( errorNum & 0xff ) };
        byteToSend.add(errorNumByte);
        //add errorMsg to List
        byteToSend.add(errorMsg.getBytes(StandardCharsets.UTF_8));
        byteToSend.add(new byte[]{0});
        //concatenate all byte[] arguments in List to one byte[]
        return concatenateByteArrays(byteToSend);
    }


    // method that handles DATA packets. write incoming data to buffer and eventually to a file. //    
    private void handleData() {
        int packetSize = (short) (((short) message[2]) << 8 | ((short) (message[3]) & 0x00ff));
        byte[] data = Arrays.copyOfRange(message,6,packetSize+6); // Extract data from the message
    
        try {
            dataBuffer.write(data); // Write data to the buffer
            
        } catch (IOException e) {
            sendErrors(0, "An unknown IO exception has occured");
            e.printStackTrace();
        }
        sendAck();

        // Normal termination - less than 512 bytes of data section
        if (packetSize < 512) {
            writeFileFromBuffer(); //write buffered data to file
            handleBcast(new byte[]{1}, this.targetFileName);
            this.targetFilePath = "null"; 
            this.targetFileName = "null";
        }
    }

    // helper method - create a file and Write buffer data to it
    private void writeFileFromBuffer() {
        byte[] fileData = dataBuffer.toByteArray(); // Get buffered data as byte array
        try {
            // Create the file if it does not exist
            File targetFile = new File(targetFilePath);
    
            // Write buffered data to the file
            try (FileOutputStream fos = new FileOutputStream(targetFile, true)) {
                fos.write(fileData);
            } 
        } 
        catch (FileNotFoundException  e) {
            sendErrors(2,"");
        }
        catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("No space left on device")) {
                sendErrors(3,"");
                e.printStackTrace();
            }
            else {
                sendErrors(2,"");
                e.printStackTrace();
            }
        } finally {
            try {
                dataBuffer.close(); // Close the buffer
                dataBuffer = new ByteArrayOutputStream(); // Reset the buffer for future use
            } catch (IOException e) {
                sendErrors(0, "An unknown IO exception has occured");
                e.printStackTrace();
            }
        }
    }


    // helper method - prepare for DATA transfer after RRQ
    private void process_fileInput(String filePath) {
        try {
            File file = new File(filePath);
            byte[] fileBytes = new byte[(int) file.length()];

            FileInputStream fileInputStream = new FileInputStream(file);
            fileInputStream.read(fileBytes);
            fileInputStream.close();
            this.inputStream = new ByteArrayInputStream(fileBytes);

        } catch (IOException e) {
            sendErrors(0, "An unknown IO exception has occured");
            e.printStackTrace();
        }
    }


    // helper method - prepare for DATA transfer after DIRQ
    private void process_byteArrayInput(byte[] byteInput) {
        this.inputStream = new ByteArrayInputStream(byteInput);
    }


    // method that handles ACK packets - send the next DATA packet
    private void sendData() {
        byte[] buffer = new byte[this.chunkSize];
        int bytesRead;
    
        try {
            bytesRead = inputStream.read(buffer, 0, this.chunkSize); // try to read 512 bytes from the buffer
            if (bytesRead != -1) { // There are bytes to read

                byte[] bytesReadBuffer = Arrays.copyOfRange(buffer, 0, bytesRead); // Extract the actual data read
                blockNumber = blockNumber + 1;

                // Build and send the DATA packet
                List<byte[]> temp = new ArrayList<>();
                temp.add(new byte[]{0,3}); // DATA opcode

                temp.add(new byte[]{(byte) ((bytesRead >> 8) & 0xFF), (byte) (bytesRead & 0xFF)}); // DATA section size
                temp.add(new byte[]{(byte) ((blockNumber >> 8) & 0xFF), (byte) (blockNumber & 0xFF)}); // Block number

                temp.add(bytesReadBuffer); // The actual DATA
                byte[] msg = concatenateByteArrays(temp);
                connections.send(connectionId, msg);
                
            }
            else {
                // No more data to read, reset variables for future use
                inputStream.close();
                this.inputStream = null;
                this.blockNumber = 0;
            }
        }

        catch (IOException e) {
            sendErrors(0, "An unknown IO exception has occured");
            e.printStackTrace();
        } 
    }


    // helper method - send the apropriate ACK packet 
    private void sendAck() {
        byte[] msg = new byte[]{0,4,0,0}; // 0,4 - permanent ACK opcode , 0,0 - default block number
        if (opcode == 3) { // DATA packet - add the corresponding block number
            msg[2] = message[4];
            msg[3] = message[5];
        }
        connections.send(connectionId, msg);
    }


    // method that handles DISC packets
    private void handleDisc() {
        sendAck(); // Notify the client that disconnect request reeived
        isLoggedIn = false; // ensure the client cannot send messages anymore
        this.shouldTerminate = true; // terminate this thread
        holder.logged_ids_new.remove(connectionId); // set the username to be disconnected
        this.connections.disconnect(connectionId); // remove the handler
    }


    // helper method that sends BCAST packets
    private void handleBcast(byte[] action , String fileName) {
        
        // BUILD THE MESSAGE
        List<byte[]> temp = new ArrayList<>();
        byte[] stringByteArray = fileName.getBytes(StandardCharsets.UTF_8);
        temp.add(new byte[]{0,9}); //OPCODE
        temp.add(action); // DELETE \ ADD identifier
        temp.add(stringByteArray); // Target file name
        temp.add(new byte[]{0}); // Terminator
        byte[] msg = concatenateByteArrays(temp);
    
        // SEND IT TO ALL LOGGED IN USERS
        for (Integer clientID : holder.logged_ids_new.keySet()) {
            connections.send(clientID, msg);
        }

    }
    
    // helper methods - finds Null indexes
    private static int findNullTerminator(byte[] byteArray) {
        for (int i = 2; i < byteArray.length; i++) { // first 2 bytes always populated with opcode
            if (byteArray[i] == 0) {
                return i;
            }
        }
        return byteArray.length; // If no null terminator found, return the length of the array
    }
}


