package bgu.spl.net.impl.tftp;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    //TODO: Implement here the TFTP encoder and decoder

    // encoder decoder differentiates between the messages
    // recognize the opcode by the first 2 bytes
    // I think we can assume user input and file names cannot contain 0 byte - 
    // https://moodle.bgu.ac.il/moodle/mod/forum/discuss.php?d=704745#p1063357
    // Also framing needs to be based on the packet length


//-------------------fields-------------------//

    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;
    private int opcode = -1;
    Map<Integer, String> numberToGroupMap = new HashMap<>();
    private boolean hasMapStarted = false;
    private int packetSize = -1;
    byte bytePacketSizeArray[] = new byte[2];
    private int blockNumber = 1;
    byte byteBlockNumberArray[] = new byte[2];

//-------------------methods-------------------//


    @Override
    public byte[] decodeNextByte(byte nextByte) {
        // TODO: implement this

        if (!hasMapStarted) {
            buildOPmapping();
        }

        if(len == 0){
            pushByte(nextByte);
            return null;
        }

        //get the opcode
        else if(len == 1){  
            pushByte(nextByte);
            opcode = (short) ((( short ) bytes [0]) << 8 | ( short ) ( bytes [1]) & 0x00ff);    //create opcode value
            if (opcode == 6 || opcode == 10 ) { // Group B - exactly 2 bytes for DIRQ and DISC
                return popByteArray(); 
            } 
            return null;
        }

        // division to cases starts from the third byte
        else{
            byte[] output = null;

            // opcode is "short" , does "get" know how to do the correct casting?
            if (numberToGroupMap.get(opcode).equals("Group A")) {
                output = decodeGroupA(nextByte);
            }

            else if (numberToGroupMap.get(opcode).equals("Group B")) {
                output = decodeGroupB(nextByte);
            }

            else if (numberToGroupMap.get(opcode).equals("Group C")) {
                // figure out what the size is..
                output = decodeGroupC(nextByte);

            }

            else if (numberToGroupMap.get(opcode).equals("Group D")) {
                output = decodeGroupD(nextByte);

            }
            else if (numberToGroupMap.get(opcode).equals("Group E")) {
                output = decodeGroupE(nextByte);

            }
            return output;
        }
    }
    

    @Override
    public byte[] encode(byte[] message) {
        //TODO: implement this

        return message;
    }

// ----------Our methods---------------------//
    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }
        bytes[len++] = nextByte;
    }

    private byte[] popByteArray() {

        byte[] result = Arrays.copyOf(bytes, bytes.length);

        // reset
        len = 0;
        this.bytes = new byte[1 << 10];

        return result;
    }


    private void buildOPmapping() {

        // OPCODES mapped to pattern for framing purposes
        // Group A - end with 0 byte: actions 1,2,5,7,8,9
        // Group B - packet length is exactly two bytes: actions 6,10
        // Group C - packet length is not constant but given: action 3
        // Group D - packet length is four bytes exactly: action 4

        numberToGroupMap.put(1, "Group A"); // RRQ
        numberToGroupMap.put(2, "Group A"); // WRQ
        numberToGroupMap.put(3, "Group C"); // DATA
        numberToGroupMap.put(4, "Group D"); // ACK
        // numberToGroupMap.put(5, "Group A"); // ERROR
        numberToGroupMap.put(5, "Group B"); // ERROR
        numberToGroupMap.put(7, "Group A"); // LOGRQ
        numberToGroupMap.put(8, "Group A"); // DELRQ
        numberToGroupMap.put(9, "Group E"); // BCAST
        // numberToGroupMap.put(10, "Group B"); // DISC

        hasMapStarted = true;

    }; 


    // Group A - end with 0 byte: actions 1,2,5,7,8,9
    private byte[] decodeGroupA(byte nextByte) {

        if (nextByte == 0) {
            return popByteArray();
        }

        pushByte(nextByte);
        return null; 
    }

    // Group B - ERROR PACKET, NEED TO CHECK THE ERROR NUM .
    private byte[] decodeGroupB(byte nextByte) {
        // 2 bytes have already received
        if(len < 4){
            pushByte(nextByte);
        }
        else{
            pushByte(nextByte);
            if (nextByte == 0) {
                return popByteArray();
            }
        }
        return null;
    }

    // Group C - packet length is not constant but given: action 3
    private byte[] decodeGroupC(byte nextByte) {
        // HOW DO WE CONSIDER THE NUMBER OF BLOCKS SO FAR
        
        if (len == 2) {
            pushByte(nextByte);
            bytePacketSizeArray[0] = nextByte;
            return null;
        }
        else if (len == 3) {
            pushByte(nextByte);
            bytePacketSizeArray[1] = nextByte;
            packetSize = (short) ((( short ) bytePacketSizeArray[0]) << 8 | ( short ) ( bytePacketSizeArray [1]) & 0x00ff);
            // packetSize = ( short ) ((( short ) bytePacketSizeArray [0]) << 8 | ( short ) ( bytePacketSizeArray [1]) );    //create packetsize value 
            return null;
        }

        else if(len == 4){
            pushByte(nextByte);
            byteBlockNumberArray[0] = nextByte;
            return null;
        }
        else if(len == 5){
            pushByte(nextByte);
            byteBlockNumberArray[1] = nextByte;
            // blockNumber = ( short ) ((( short ) byteBlockNumberArray [0]) << 8 | ( short ) ( byteBlockNumberArray [1]) );    //create packetsize value 
            blockNumber = (short) ((( short ) byteBlockNumberArray [0]) << 8 | ( short ) ( byteBlockNumberArray [1]) & 0x00ff);
            if (packetSize == 0) { // EDGE CASE: file content is 0 bytes
                return popByteArray();
            }
            return null;
        }
        //if (DataPacketSize - len==1) - the last packet to get 
        else if (packetSize - (len-6) == 1) {
            // System.out.println("LEN is: " + len);
            // System.out.println("packetSize is: " + packetSize);
            pushByte(nextByte);
            return popByteArray();
        }
        pushByte(nextByte);
        return null; 
    }

    // Group D - packet length is four bytes exactly: action 4
    private byte[] decodeGroupD(byte nextByte) {
        if (len == 3) { // max length is 4 , so the last byte is received when we have already reached 3
            pushByte(nextByte);
            return popByteArray();
        }
        pushByte(nextByte);
        return null;
    }

    // Group E - start testing for 0 bytes after 3rd index
    private byte[] decodeGroupE(byte nextByte) {
        if (len >= 3 && nextByte == 0) {
            return popByteArray();
        }
        pushByte(nextByte);
        return null;
    }

}

// package bgu.spl.net.impl.tftp;

// import java.nio.charset.StandardCharsets;
// import java.util.Arrays;
// import java.util.HashMap;
// import java.util.Map;

// import bgu.spl.net.api.MessageEncoderDecoder;

// public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
//     //TODO: Implement here the TFTP encoder and decoder

//     // encoder decoder differentiates between the messages
//     // recognize the opcode by the first 2 bytes
//     // I think we can assume user input and file names cannot contain 0 byte - 
//     // https://moodle.bgu.ac.il/moodle/mod/forum/discuss.php?d=704745#p1063357
//     // Also framing needs to be based on the packet length


// //-------------------fields-------------------//

//     private byte[] bytes = new byte[1 << 10]; //start with 1k
//     private int len = 0;
//     private int opcode = -1;
//     Map<Integer, String> numberToGroupMap = new HashMap<>();
//     private boolean hasMapStarted = false;
//     private int packetSize = -1;
//     byte bytePacketSizeArray[] = new byte[2];
//     private int blockNumber = -1;
//     byte byteBlockNumberArray[] = new byte[2];

// //-------------------methods------------//


//     @Override
//     public byte[] decodeNextByte(byte nextByte) {
//         // TODO: implement this

//         if (!hasMapStarted) {
//             buildOPmapping();
//         }

//         if(len == 0){
//             pushByte(nextByte);
//             return null;
//         }

//         //get the opcode
//         else if(len == 1){  
//             pushByte(nextByte);
//             opcode = (short) ((( short ) bytes [0]) << 8 | ( short ) ( bytes [1]) & 0x00ff);    //create opcode value
//             if (opcode == 6 || opcode == 10 ) { // Group B - exactly 2 bytes for DIRQ and DISC
//                 return popByteArray(); 
//             } 
//             return null;
//         }

//         // division to cases starts from the third byte
//         else{
//             byte[] output = null;

//             // opcode is "short" , does "get" know how to do the correct casting?
//             if (numberToGroupMap.get(opcode).equals("Group A")) {
//                 output = decodeGroupA(nextByte);
//             }

//             // else if (numberToGroupMap.get(opcode).equals("Group B")) {
//             //     output = decodeGroupB();
//             // }

//             else if (numberToGroupMap.get(opcode).equals("Group C")) {
//                 // figure out what the size is..
//                 output = decodeGroupC(nextByte);

//             }

//             else if (numberToGroupMap.get(opcode).equals("Group D")) {
//                 output = decodeGroupD(nextByte);

//             }

//             return output;
//         }
//     }
    

//     @Override
//     public byte[] encode(byte[] message) {
//         //TODO: implement this

//         return message;
//     }

// // ----------Our methods---------------------//
//     private void pushByte(byte nextByte) {
//         if (len >= bytes.length) {
//             bytes = Arrays.copyOf(bytes, len * 2);
//         }
//         bytes[len++] = nextByte;
//     }

//     private byte[] popByteArray() {

//         byte[] result = Arrays.copyOf(bytes, bytes.length);

//         // reset
//         len = 0;
//         this.bytes = new byte[1 << 10];

//         return result;
//     }


//     private void buildOPmapping() {

//         // OPCODES mapped to pattern for framing purposes
//         // Group A - end with 0 byte: actions 1,2,5,7,8,9
//         // Group B - packet length is exactly two bytes: actions 6,10
//         // Group C - packet length is not constant but given: action 3
//         // Group D - packet length is four bytes exactly: action 4

//         numberToGroupMap.put(1, "Group A"); // RRQ
//         numberToGroupMap.put(2, "Group A"); // WRQ
//         numberToGroupMap.put(3, "Group C"); // DATA
//         numberToGroupMap.put(4, "Group D"); // ACK
//         numberToGroupMap.put(5, "Group A"); // ERROR
//         // numberToGroupMap.put(6, "Group B"); // DIRQ
//         numberToGroupMap.put(7, "Group A"); // LOGRQ
//         numberToGroupMap.put(8, "Group A"); // DELRQ
//         numberToGroupMap.put(9, "Group A"); // BCAST
//         // numberToGroupMap.put(10, "Group B"); // DISC

//         hasMapStarted = true;

//     }; 


//     // Group A - end with 0 byte: actions 1,2,5,7,8,9
//     private byte[] decodeGroupA(byte nextByte) {

//         if (nextByte == 0) {
//             return popByteArray();
//         }

//         pushByte(nextByte);
//         return null; 
//     }

//     // Group B - packet length is exactly two bytes: actions 6,10
//     // private byte[] decodeGroupB() {
//     //     // 2 bytes have already received
//     //     return popByteArray();
//     // }

//     // Group C - packet length is not constant but given: action 3
//     private byte[] decodeGroupC(byte nextByte) {
//         // HOW DO WE CONSIDER THE NUMBER OF BLOCKS SO FAR
        
//         if (len == 2) {
//             pushByte(nextByte);
//             bytePacketSizeArray[0] = nextByte;
//             return null;
//         }
//         else if (len == 3) {
//             pushByte(nextByte);
//             bytePacketSizeArray[1] = nextByte;

//             packetSize = (short) ((( short ) bytePacketSizeArray[0]) << 8 | ( short ) ( bytePacketSizeArray [1]) & 0x00ff);
//             // packetSize = ( short ) ((( short ) bytePacketSizeArray [0]) << 8 | ( short ) ( bytePacketSizeArray [1]) );    //create packetsize value 
//             return null;
//         }

//         else if(len == 4){
//             pushByte(nextByte);
//             byteBlockNumberArray[0] = nextByte;
//             return null;
//         }
//         else if(len == 5){
//             pushByte(nextByte);
//             byteBlockNumberArray[1] = nextByte;
//             // blockNumber = ( short ) ((( short ) byteBlockNumberArray [0]) << 8 | ( short ) ( byteBlockNumberArray [1]) );    //create packetsize value 
//             blockNumber = (short) ((( short ) byteBlockNumberArray [0]) << 8 | ( short ) ( byteBlockNumberArray [1]) & 0x00ff);
//             return null;
//         }
//         //if (DataPacketSize - len==0) - beacuse packet size doesnt include the non data bytes 
//         if (packetSize - (len-6) == 0) { 
//             return popByteArray();
//         }
//         else if (packetSize - (len-6) == 1) {
//             System.out.println("LEN is: " + len);
//             System.out.println("packetSize is: " + packetSize);
//             pushByte(nextByte);
//             return popByteArray();
//         }
//         pushByte(nextByte);
//         return null; 
//     }

//     // Group D - packet length is four bytes exactly: action 4
//     private byte[] decodeGroupD(byte nextByte) {
//         if (len == 3) { // max length is 4 , so the last byte is received when we have already reached 3
//             pushByte(nextByte);
//             return popByteArray();
//         }
//         pushByte(nextByte);
//         return null;
//     }

// }