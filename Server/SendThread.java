package Server;

import Auxiliar.Chunk;
import Auxiliar.Partials;
import Auxiliar.PartitionedFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class SendThread extends Thread {

    private String currentDir;
    //addresses
    private InetAddress mc_address;
    private InetAddress mdr_address;
    private InetAddress mdb_address;

    //ports
    private int mc_port;
    private int mdr_port;
    private int mdb_port;

    //sockets
    private MulticastSocket mc_socket;
    private MulticastSocket mdr_socket;
    private MulticastSocket mdb_socket;

    public SendThread(String mc, int mc_port, String mdb, int mdb_port, String mdr, int mdr_port, String dir)
            throws IOException {

        //Initiation of mc channel
        this.mc_address = InetAddress.getByName(mc);
        this.mc_port = mc_port;
        this.mc_socket = new MulticastSocket(mc_port);
        this.mc_socket.setTimeToLive(1);
        mc_socket.joinGroup(mc_address);

        //Initiation of mdr channel
        this.mdr_address = InetAddress.getByName(mdr);
        this.mdr_port = mdr_port;
        this.mdr_socket = new MulticastSocket(mdr_port);
        this.mdr_socket.setTimeToLive(1);
        mdr_socket.joinGroup(mdr_address);

        //Initiation of mdb channel
        this.mdb_address = InetAddress.getByName(mdb);
        this.mdb_port = mdb_port;
        this.mdb_socket = new MulticastSocket(mdb_port);
        this.mdb_socket.setTimeToLive(1);

        this.currentDir = dir;

    }

    public void run() {
        //read message from console
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        try {
            String message = br.readLine();

            String[] message_args = message.split(" ");

            switch(message_args[0]){
                case "BACKUP": {
                    System.out.println("Backup command prompted");

                    if(message_args.length != 3) {
                        System.out.println("Usage: BACKUP <FILE> <REPLICATION DEGREE>");
                    }
                    else {
                        //send file in chunks to other peers
                        String newMessage = message + " " + Partials.version;
                        String[] split = newMessage.split(" ");

                        String tempVersion = new String();
                        tempVersion = split[2];

                        String tempRep = new String();
                        tempRep = split[3];

                        split[3] = tempVersion;
                        split[2] = tempRep;

                        sendFileChunks(split);
                    }
                    break;
                }
                case "RESTORE": {
                    System.out.println("Restore command prompted");

                    if(message_args.length != 2) {
                        System.out.println("Usage: RESTORE <FILE>");
                    }
                    else {
                        String newMessage = message + " " + Partials.version;
                        String[] split = newMessage.split(" ");

                        //retrieve chunk from other peers
                        retrieveChunks(split);

                    }
                    break;
                }
                case "DELETE": {
                    System.out.println("Delete command prompted");

                    if(message_args.length != 2) {
                        System.out.println("Usage: DELETE <FILE>");
                    }
                    else {
                        //notify other peers that file was deleted
                        String newMessage = message + " " + Partials.version;
                        String[] split = newMessage.split(" ");

                        System.out.println(split[0]+ " " + split[1] + " " + split[2]);

                        notifyFileDeletion(split);
                    }
                    break;
                }
                case "RECLAIM": {
                    System.out.println("Space reclaim command prompted");

                    if(message_args.length != 2) {
                        System.out.println("Usage: DELETE <SPACE>");
                    }
                    else {
                        //notify other peers that file was deleted
                        reclaimSpace(Integer.parseInt(message_args[1].trim()));
                    }
                    break;
                }
                default: {
                    System.out.println("Invalid command, please try again");
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void reclaimSpace(int space) throws IOException {
        int spaceReclaimed = 0;

        while (spaceReclaimed < space) {
            int removed = Partials.removeChunk(currentDir,mc_socket,mc_address,mc_port);

            if(removed == 0)
                break;
            else spaceReclaimed += removed;
        }
    }

    private void notifyFileDeletion(String[] message_args) throws IOException {
        String fileId = message_args[1];

        message_args[1] = encryptFileId(fileId);

        //send DELETE message 5 times to ensure chunk deletion on other peers
        for(int i = 0; i < 5; i++) {
            byte[] buf;
            buf = createDeleteMessage(message_args);

            DatagramPacket packet = new DatagramPacket(buf, buf.length, mc_address, mc_port);
            mc_socket.send(packet);
        }

    }

    private byte[] createDeleteMessage(String[] message_args) {
        String command = "DELETE " + message_args[2] + " " + message_args[1] + " ";
        byte[] commandBytes = command.getBytes();

        //build termination token
        byte[] crlf = Partials.createCRLFToken();

        //header
        byte[] header = new byte[commandBytes.length + crlf.length];
        System.arraycopy(commandBytes, 0, header, 0, commandBytes.length);
        System.arraycopy(crlf, 0, header, commandBytes.length, crlf.length);

        return header;
    }

    private void retrieveChunks(String[] message_args) throws IOException {
        String fileId = message_args[1];

        message_args[1] = encryptFileId(fileId);

        int chunkNo = 0;
        boolean eof=false;

        while (!eof) {

            byte[] buf;
            buf = createRestoreMessage(message_args, String.valueOf(chunkNo));

            DatagramPacket packet = new DatagramPacket(buf, buf.length, mc_address, mc_port);
            mc_socket.send(packet);

            boolean reached=false;

            while(!reached) {
                byte[] receive_buf = new byte[65000];

                DatagramPacket receive_packet = new DatagramPacket(receive_buf, receive_buf.length);
                mdr_socket.receive(receive_packet);

                ArrayList<Object> separated = Partials.parseMessage(receive_packet.getData(), receive_packet.getLength());

                String header = (String) separated.get(0);
                byte[] body = (byte[]) separated.get(1);

                String[] header_parts = header.split(" ");
                if(header_parts != null)
                    if(header_parts[0].equals("CHUNK") &&
                            Integer.parseInt(header_parts[3].trim())==chunkNo) {

                        Partials.appendChunk(body, currentDir, fileId);
                        if(body.length < 64000) {
                            eof=true;
                        }
                        chunkNo++;
                        reached=true;
                    }
            }
        }
        System.out.println("Reached the end of the file");
    }

    private byte[] createRestoreMessage(String[] message_args, String chunkNo) {
        String command = "GETCHUNK " + message_args[2] + " " + message_args[1] + " " + chunkNo + " ";
        byte[] commandBytes = command.getBytes();

        //build termination token
        byte[] crlf = Partials.createCRLFToken();

        //header
        byte[] header = new byte[commandBytes.length + crlf.length];
        System.arraycopy(commandBytes, 0, header, 0, commandBytes.length);
        System.arraycopy(crlf, 0, header, commandBytes.length, crlf.length);

        return header;
    }

    private void sendFileChunks(String[] message_args) throws IOException, InterruptedException {
        String fileId = message_args[1];

        //open file and get chunks
        PartitionedFile fullFile = new PartitionedFile(fileId);
        ArrayList<Chunk> chunks = fullFile.getChunks();

        //encrypt filename before sending the message
        message_args[1] = encryptFileId(fileId);

        //send all chunks
        for(int i = 0; i < chunks.size(); i++) {
            int numberOfStores = 0;
            byte[] buf = new byte[65000];
            int timeout = 500;
            int attempts = 5;
            boolean finished = false;
            boolean innerCircle=true;

            //sends message

            Chunk currentChunk = chunks.get(i);
            sendChunk(currentChunk,message_args);
            System.out.println("Sent PUTCHUNK");

            while(attempts > 0 && !finished) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {

                    while(innerCircle) {
                        mc_socket.setSoTimeout(timeout);
                        mc_socket.receive(packet);
                        if(compareMessage(message_args, buf,String.valueOf(i))){
                            numberOfStores++;
                        }

                        if(numberOfStores >= Integer.parseInt(message_args[3])) {
                            finished=true;
                            innerCircle=false;
                        }
                        System.out.println(numberOfStores);
                    }

                } catch (SocketTimeoutException e) {
                    timeout *= 2;
                    attempts--;
                    sendChunk(currentChunk,message_args);
                    System.out.println("Sent PUTCHUNK");

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("End of backup");


    }

    private boolean compareMessage(String[] message_args, byte[] buf, String chunkNo) {
        String received_header = new String(buf,0,buf.length);
        String[] seperated_header = received_header.split(" ");

        System.out.println("Compare Message");

        if(seperated_header[0].equals("STORED")) {
            if(message_args[2].equals(seperated_header[1]) &&
                    message_args[1].equals(seperated_header[2]) &&
                    chunkNo.equals(seperated_header[3])) {
                return true;
            }
        }

        return false;
    }

    public void sendChunk(Chunk currentChunk, String[] message_args) throws IOException, InterruptedException {
        byte[] buf;

        buf = createBackupMessage(currentChunk, message_args);

        DatagramPacket packet = new DatagramPacket(buf, buf.length, mdb_address, mdb_port);
        mdb_socket.send(packet);
    }

    private static String encryptFileId(String fileId) {
        String output;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(fileId.getBytes());

            byte[] hash = digest.digest();
            BigInteger bigInt = new BigInteger(1, hash);
            output = bigInt.toString(16);
            while ( output.length() < 32 ) {
                output = "0"+output;
            }
        }
        catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }

        return output;
    }

    public byte[] createBackupMessage(Chunk chunk, String[] message_args)
            throws UnsupportedEncodingException {

        String command = "PUTCHUNK " + message_args[2] + " " + message_args[1] + " " +
                chunk.getChunkNumber() + " " + message_args[3] + " ";
        byte[] commandBytes = command.getBytes();

        //build termination token
        byte[] crlf = Partials.createCRLFToken();

        //header
        byte[] header = new byte[commandBytes.length + crlf.length];
        System.arraycopy(commandBytes, 0, header, 0, commandBytes.length);
        System.arraycopy(crlf, 0, header, commandBytes.length, crlf.length);

        //body
        byte[] body = chunk.getBody();
        System.out.println("Body size sender: " + body.length);

        //final message
        byte[] message = new byte[header.length + body.length];
        System.arraycopy(header, 0, message, 0, header.length);
        System.arraycopy(body, 0, message, header.length, body.length);

        return message;
    }

}