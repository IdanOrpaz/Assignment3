package bgu.spl.net.impl.tftp;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TftpProtocol{

//-------------fields----------------//

    private boolean shouldTerminate = false;
    // private int connectionId;


    // values that may change in TftpKeyboard Thread
    public Object threadLock = new Object();

    public volatile boolean WrqRequest = false;
    public volatile boolean discRequest = false; 
    public volatile boolean RrqRequest = false;
    public volatile boolean DirqRequest = false;
    public volatile boolean LogrqRequest = false;
    public volatile String fileName = new String();
    private boolean isLoggedIn = false;
    private byte[] opcodeByteArray;
    private int opcode = -1;
    private String username; 
    private byte[] message;

    private File directory;

    // OUTGOING DATA
    FileInputStream fis = null;
    FileChannel channel = null;
    int bufferSize = 512;
    int blockNumber = 1;

    // INCOMING DATA
    // FileOutputStream fos = null;
    // File targetFile = null;
    public volatile String uploadedFile = null;
    public volatile String targetFilePath = null;
    private ByteArrayOutputStream dataBuffer = new ByteArrayOutputStream();




    public  TftpProtocol() {
        // TODO implement this
        this.shouldTerminate = false;
        this.directory = new File("./Files" );
        if (!directory.exists()) {
            directory.mkdirs();  // Create the "Files" directory
        }
    }


    public byte[] process(byte[] message) {
        // TODO implement this

        //save byte[] array
        this.message = message;

        // get the opecode of the packet
        byte[] msgOpcodeByteArray = {message[0],message[1]};
        this.opcodeByteArray = msgOpcodeByteArray;
        opcode = (short) ((( short ) opcodeByteArray [0]) << 8 | ( short ) ( opcodeByteArray [1]) & 0x00ff );

        if (opcode == 5){//ERROR
            printError();
            return null;
        }
        else if (opcode == 9){//Bcast
            printBcast();
            return null;
        }
        else if (opcode == 3){//DATA   
            handleData();
            return sendAck();}

        else if (opcode == 4){//ACK
            //handleAck() returns {0} if there's no message to send.
            byte[] msg = handleAck();
            if(msg.length < 2){
                return null;}
            return msg;}
        else{
            return null;
        }
    }


    public boolean shouldTerminate() {
        // TODO implement this
        // True if the connection should be terminated
        
        //1.disconnect in connections
        //2.remove \ assign as not active in holder/ids_login
        return this.shouldTerminate;
    } 




//-------------------------------OUR METHODS--------------------------------//



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


    // For incoming DATA packets during file upload , and new LOGRQ, WRQ, DELRQ, DISC requests
    private void handleData() {
        // get packetsize and byte array of the data
        int packetSize = (short) (((short) message[2]) << 8 | ((short) (message[3]) & 0x00ff));
        byte[] data = Arrays.copyOfRange(message,6,packetSize+6); // Extract data from the message

        // if came from RRQ:
        // do as usual and send ack.
        if(RrqRequest){
            try {
                dataBuffer.write(data); // Write data to the buffer
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Check if this is the last packet (less than 512 bytes)
            if (packetSize < 512) {
                // End of transmission, write buffered data to file
                writeFileFromBuffer();
                System.out.println("RRQ " + targetFilePath.substring(2) + " complete");
                this.targetFilePath = null;
                this.RrqRequest = false;
                //free keyboardThread when finishing 
                synchronized(threadLock){
                    threadLock.notifyAll();
                }
            }
            // try {appendBytesToFile(data);}
            // catch (IOException e) {e.printStackTrace();}
            // if (data.length < this.bufferSize) { // termination - uploade completed
            //     // handleBcast(new byte[]{1}, this.uploadedFile);
            //     this.fos = null;
            //     this.uploadedFile = null;
            //     RrqRequest = false;
            //     // free keyboardThread
            //     synchronized(threadLock){
            //         threadLock.notifyAll();
            //     }
            // }
        }
        // if came from DIRQ
        else if(DirqRequest){
            //COLLECT IN BUFFER
            String dirqFiles = new String();
            int startIndx = 0;
            // finde file names
            for(int i = 0 ; i < data.length ; i++){
                if (data[i] == 0 ){
                    // extract file name 
                    dirqFiles = dirqFiles + "\n" + new String(data, startIndx, i-startIndx , StandardCharsets.UTF_8);
                    //update indexes
                    startIndx = i + 1; 
                    i = startIndx;
                }
                if (i == data.length-1 ){
                    dirqFiles = dirqFiles + "\n" + new String(data, startIndx, i-startIndx + 1 , StandardCharsets.UTF_8);
                    startIndx = i + 1; 
                    i = startIndx;
                }
            }
            System.out.println(dirqFiles);

            if (data.length < 512){
                //set DIRQREQUEST = false;
                DirqRequest = false;

                //free keyboardThread when finishing 
                synchronized(threadLock){
                    threadLock.notifyAll();
                }
            }
        }
        
    }

    // helper method for handleData()
    private void writeFileFromBuffer() {
        byte[] fileData = dataBuffer.toByteArray(); // Get buffered data as byte array
    
        try {
            // Create the file if it does not exist
            File targetFile = new File(targetFilePath);
    
            // Write buffered data to the file
            try (FileOutputStream fos = new FileOutputStream(targetFile, true)) {
                fos.write(fileData);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                dataBuffer.close(); // Close the buffer
                dataBuffer = new ByteArrayOutputStream(); // Reset the buffer for future use
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    // private void appendBytesToFile(byte[] data) throws IOException {
    //     try {
    //         FileChannel channel = fos.getChannel();
    //         ByteBuffer buffer = ByteBuffer.wrap(data);
    //         channel.write(buffer);
    //     }
    //     catch (IOException e) {e.printStackTrace();}
    // }

    // "Receive Data"
    private byte[] sendAck() {
        byte[] msg = new byte[]{0,4,0,0}; // 0,4 - permanent ACK opcode , 0,0 - default block number
        // int blockNumber = (( short ) ((( short ) message[4]) << 8 | ( short ) ( message[5])));
        if (opcode == 3) { // DATA packet
            // msg[3] = message[5]; // block number is at the 6th position of the message
            msg[2] = message[4];
            msg[3] = message[5];
        }
        // connections.send(connectionId, msg);
        return msg;
    }


    // To send te cliet the next block of data after receiving
    // "receive Ack"
    private byte[] handleAck() {
        // print :  ACK <blockNumber>
        blockNumber = ( short ) ((( short ) message[2]) << 8 | ( short ) ( message[3]) );
        System.out.println("ACK "  + blockNumber );
        byte[] nothingToSend = new byte[] {0};

        if(LogrqRequest){
            isLoggedIn = true;
            synchronized(threadLock){
                threadLock.notifyAll();
            }
            LogrqRequest = false;
            return nothingToSend;
        }
        
        // check if ack came back from DISC request - close the client 
        if(discRequest){
            shouldTerminate = true;
            System.out.println("Disconnecting");
            // free keyboardThread
            synchronized(threadLock){
                    threadLock.notifyAll();
            }
            return nothingToSend;
        }

        // check if the channel knows the last postion
        else if(WrqRequest){
            try {
                long remainingBytes = channel.size() - channel.position();
                if (remainingBytes > 0) {
                    // Read 512 bytes into ByteBuffer
                    ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
                    int bytesRead = channel.read(byteBuffer);

                    // Save the position where we stopped
                    long position = channel.position();

                    // Convert ByteBuffer to byte[]
                    byte[] byteArray = new byte[bytesRead];
                    byteBuffer.flip(); // Prepare buffer for reading
                    byteBuffer.get(byteArray);

                    // Build Data packet
                    List<byte[]> temp = new ArrayList<>();
                    temp.add(new byte[]{0,3});
                    temp.add(new byte[]{(byte) ((bytesRead >> 8) & 0xFF), (byte) (bytesRead & 0xFF)});
                    temp.add(new byte[]{(byte) ((blockNumber+1 >> 8) & 0xFF), (byte) (blockNumber+1 & 0xFF)}); // convert block number to byte
                    temp.add(byteArray);
                    byte[] msg = concatenateByteArrays(temp);

                    // connections.send(connectionId, msg);

                    // Move the position back to the saved position
                    blockNumber = blockNumber + 1;
                    channel.position(position);
                    return msg;
                }

                else {
                    // System.out.println("End of file reached.");
                    fis = null;
                    channel = null;
                    blockNumber = 0;
                    WrqRequest = false;
                    System.out.println("WRQ " + fileName + " Complete");
                    fileName = "";
                    synchronized(threadLock){
                        threadLock.notifyAll();
                    }
                    return nothingToSend;
                }
            }
            catch (IOException e){} //{e.printStackTrace();}
            return nothingToSend;
        }
        else{
            synchronized(threadLock){
                threadLock.notifyAll();
            }
            return nothingToSend;}
    }





    private void printError(){
        int errorNum = ( short ) ((( short ) message [2]) << 8 | ( short ) ( message [3]) );
        String errorMsg = new String(message, 4, message.length-5, StandardCharsets.UTF_8);


        System.out.println("ERROR" + errorNum +" " + errorMsg );
        

        // free keyboardThread
        synchronized(threadLock){
            threadLock.notifyAll();
        }
    }


    private void printBcast() {
        String actiontrnslt = new String();
        int action = message[2];
        if (action == 1){
            actiontrnslt = "add";}
        else{
            actiontrnslt = "delete";
        }
        String fileName = new String(message , 3 , message.length-4, StandardCharsets.UTF_8);
        System.out.println("BCAST " + actiontrnslt +" "+ fileName);
    
        // free keyboardThread
        synchronized(threadLock){
                threadLock.notifyAll();
        }
    }
    

    // finds Null index
    private static int findNullTerminator(byte[] byteArray) {
        for (int i = 2; i < byteArray.length; i++) {
            if (byteArray[i] == 0) {
                return i;
            }
        }
        return byteArray.length; // If no null terminator found, return the length of the array
    }

}


