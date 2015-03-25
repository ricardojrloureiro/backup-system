package Server;

import Auxiliar.Chunk;
import Auxiliar.PartitionedFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Arrays;

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

    public SendThread(String mc, int mc_port, String mdr, int mdr_port, String mdb, int mdb_port)
            throws IOException {

        //Initiation of mc channel
        this.mc_address = InetAddress.getByName(mc);
        this.mc_port = mc_port;
        this.mc_socket = new MulticastSocket(mc_port);
        this.mc_socket.setTimeToLive(1);

        //Initiation of mdb channel
        this.mdr_address = InetAddress.getByName(mdr);
        this.mdr_port = mdr_port;
        this.mdr_socket = new MulticastSocket(mdr_port);
        this.mdr_socket.setTimeToLive(1);

        //Initiation of mdr channel
        this.mdb_address = InetAddress.getByName(mdb);
        this.mdb_port = mdb_port;
        this.mc_socket = new MulticastSocket(mc_port);
        this.mc_socket.setTimeToLive(1);

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
                        sendChunks(message_args);
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

    public void sendChunks(String[] message_args) throws IOException, InterruptedException {

        String version = message_args[1];
        String fileId = message_args[2];
        Integer chunkNo = Integer.parseInt(message_args[3]);
        Integer replicationDeg = Integer.parseInt(message_args[4]);

        PartitionedFile fullFile = new PartitionedFile("C:\\Users\\Tiago\\Desktop\\artifacts.xml");
        ArrayList<Chunk> chunks = fullFile.getChunks();

        byte[] buf = createBackupMessage(chunks.get(chunkNo), message_args);

        DatagramPacket packet = new DatagramPacket(buf, buf.length, mc_address, mc_port);
        mc_socket.send(packet);
    }

    public byte[] createBackupMessage(Chunk chunk, String[] message_args) {

        StringBuilder builder = new StringBuilder();
        for(String s : message_args) {
            builder.append(s);
            builder.append(" ");
        }

        String command = builder.toString(); //join all the arguments of the command into a string
        byte[] commandBytes = command.getBytes();

        //build termination token
        byte[] crfl = new byte[4];
        crfl[0] = 0xD;
        crfl[1] = 0xA;
        crfl[2] = 0xD;
        crfl[3] = 0xA;

        //header
        byte[] header = new byte[commandBytes.length + crfl.length];
        System.arraycopy(commandBytes, 0, header, 0, commandBytes.length);
        System.arraycopy(crfl, 0, header, commandBytes.length, crfl.length);

        //body
        byte[] body = chunk.getBody();

        //final message
        byte[] message = new byte[header.length + body.length];
        System.arraycopy(header, 0, message, 0, header.length);
        System.arraycopy(body, 0, message, header.length, body.length);


        return message;
    }
}