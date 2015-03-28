package Server;

import Auxiliar.Partials;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Tiago on 28-03-2015.
 */
public class MCReceiverThread extends Thread {

    private String currentDir;
    private InetAddress mc_address;
    private InetAddress mdb_address;
    private InetAddress mdr_address;
    private int mc_port;
    private int mdb_port;
    private int mdr_port;

    private boolean state;

    private MulticastSocket mc_socket, mdb_socket, mcr_socket;

    public MCReceiverThread(String mc, int mc_port, String mdb, int mdb_port, String mdr, int mdr_port, String dir) throws IOException {
        state=true;

        this.mc_address = InetAddress.getByName(mc);
        this.mc_port = mc_port;


        this.mdb_address = InetAddress.getByName(mdb);
        this.mdb_port = mdb_port;


        this.mdr_address = InetAddress.getByName(mdr);
        this.mdr_port = mdr_port;


        //sockets initialization
        this.mc_socket = new MulticastSocket(this.mc_port);
        this.mc_socket.setTimeToLive(1);

        this.mdb_socket = new MulticastSocket(this.mdb_port);
        this.mdb_socket.setTimeToLive(1);

        this.mcr_socket = new MulticastSocket(this.mdr_port);
        this.mcr_socket.setTimeToLive(1);

        this.currentDir = dir;
    }

    public void run() {
        try {
            mc_socket.joinGroup(mc_address);
        } catch (IOException e) {
            System.out.println("Could not join mdb group");
            e.printStackTrace();
        }

        byte[] buf = new byte[65000];

        while(state) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                mc_socket.receive(packet);
            } catch (IOException e) {
                System.out.println("Could not receive from mdb socket");
                e.printStackTrace();
            }

            String header = null;

            ArrayList<Object> splitMessage = Partials.parseMessage(packet.getData(), packet.getLength());

            header = (String) splitMessage.get(0);

            //print results
            System.out.println("HEADER: ");
            System.out.println(header);
            System.out.println();

            //split the header to fetch the fileId
            String[] header_args = header.split(" ");

            if (header_args[0].equals("STORED")) {
                //try {
                    System.out.println("Incrementing value stored");
                    //Partials.changeRepDegree(currentDir, header_args[2]);
               // } catch (IOException e) {
                   // e.printStackTrace();
                //}
            }
            else if(header_args[0].equals("GETCHUNK")) {
                System.out.println("Inside GETCHUNK");
                System.out.println("CHUNK NO: " + header_args[3] + "SIM");
                if(Partials.chunkExists(header_args[1],header_args[2],header_args[3],currentDir)){
                    System.out.println("Chunk exists.");
                    try {
                        wait(new Random().nextInt(401));
                        System.out.println("SEND CHUNK 1");
                    } catch (InterruptedException e) {
                        System.out.println("SEND CHUNK");
                        //e.printStackTrace();
                    }
                }
            }

        }

    }
}
