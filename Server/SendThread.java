package Server;

import Auxiliar.Chunk;
import Auxiliar.Partials;
import Auxiliar.PartitionedFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class SendThread extends Thread {

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

    public SendThread(String mc, int mc_port, String mdb, int mdb_port, String mdr, int mdr_port)
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

        //Initiation of mdb channel
        this.mdb_address = InetAddress.getByName(mdb);
        this.mdb_port = mdb_port;
        this.mdb_socket = new MulticastSocket(mdb_port);
        this.mdb_socket.setTimeToLive(1);

    }

    public void run() {
        //read message from console
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        try {
            String message = br.readLine();

            String[] message_args = message.split(" ");

            switch(message_args[0]){
                case "PUTCHUNK": {
                    System.out.println("Chunk command prompted");

                    if(message_args.length != 5) {
                        System.out.println("Usage: PUTCHUNK <Version> <FileId> <ChunkNo> <ReplicationDeg>");
                    }
                    else {
                        int numberOfStores = 0;
                        byte[] buf = new byte[65000];
                        int timeout = 500;
                        int attempts = 5;
                        boolean finished = false;

                        //encrypt filename before sending the message
                        String fileId = message_args[2];
                        message_args[2] = encryptFileId(fileId);

                        //sends message
                        System.out.println("Sent PUTCHUNK");
                        sendChunks(message_args);

                        while(attempts > 0 && !finished) {
                            DatagramPacket packet = new DatagramPacket(buf, buf.length);
                            try {
                                mc_socket.setSoTimeout(timeout);
                                mc_socket.receive(packet);

                                if(compareMessage(message_args, buf)){
                                    numberOfStores++;
                                }

                                if(numberOfStores >= Integer.parseInt(message_args[4])) {
                                    finished=true;
                                }


                            } catch (SocketTimeoutException e) {
                                timeout *= 2;
                                attempts--;
                                System.out.println("Sent PUTCHUNK");
                                sendChunks(message_args);

                            } catch (IOException e) {
                                e.printStackTrace();
                            }


                        }
                    }

                }
                break;
                default: {
                    System.out.println("Invalid command, please try again");
                } break;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean compareMessage(String[] message_args, byte[] buf) {
        String received_header = new String(buf,0,buf.length);
        String[] seperated_header = received_header.split(" ");

        System.out.println("Compare Message");

        if(seperated_header[0].equals("STORED")) {
            if(message_args[1].equals(seperated_header[1]) &&
                    message_args[2].equals(seperated_header[2]) &&
                    message_args[3].equals(seperated_header[3])) {
                return true;
            }
        }

        return false;
    }

    public void sendChunks(String[] message_args) throws IOException, InterruptedException {

        String fileId = message_args[2];
        Integer chunkNo = Integer.parseInt(message_args[3]);

        PartitionedFile fullFile = new PartitionedFile("C:\\Users\\Tiago\\Desktop\\artifacts.xml");
        ArrayList<Chunk> chunks = fullFile.getChunks();

        byte[] buf = new byte[0];
        try {
            buf = createBackupMessage(chunks.get(chunkNo), message_args);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

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


    public byte[] createBackupMessage(Chunk chunk, String[] message_args) throws UnsupportedEncodingException, NoSuchAlgorithmException {

        StringBuilder builder = new StringBuilder();

        for(String s : message_args) {
            builder.append(s);
            builder.append(" ");
        }

        String command = builder.toString(); //join all the arguments of the command into a string
        byte[] commandBytes = command.getBytes();

        //build termination token
        byte[] crlf = Partials.createCRLFToken();

        //header
        byte[] header = new byte[commandBytes.length + crlf.length];
        System.arraycopy(commandBytes, 0, header, 0, commandBytes.length);
        System.arraycopy(crlf, 0, header, commandBytes.length, crlf.length);

        //body
        byte[] body = chunk.getBody();

        //final message
        byte[] message = new byte[header.length + body.length];
        System.arraycopy(header, 0, message, 0, header.length);
        System.arraycopy(body, 0, message, header.length, body.length);

        return message;
    }

}