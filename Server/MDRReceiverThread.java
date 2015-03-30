package Server;

import Auxiliar.Partials;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

public class MDRReceiverThread extends Thread {

    private BlockingQueue bq;
    private String currentDir;
    private InetAddress mc_address;
    private InetAddress mdb_address;
    private InetAddress mdr_address;
    private int mc_port;
    private int mdb_port;
    private int mdr_port;

    private boolean state;

    private MulticastSocket mc_socket, mdb_socket, mdr_socket;

    private Thread mc_thread;

    public MDRReceiverThread(String mc, int mc_port, String mdb, int mdb_port, String mdr, int mdr_port, String dir, Thread mc_thread, BlockingQueue bq) throws IOException {
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

        this.mdr_socket = new MulticastSocket(this.mdr_port);
        this.mdr_socket.setTimeToLive(1);

        this.currentDir = dir;

        this.mc_thread = mc_thread;
        this.bq = bq;
    }

    public void run() {

        try {
            mdr_socket.joinGroup(mdr_address);
        } catch (IOException e) {
            System.out.println("Could not join mdb group");
            e.printStackTrace();
        }

        byte[] buf = new byte[65000];

        while(state) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                mdr_socket.receive(packet);
            } catch (IOException e) {
                System.out.println("Could not receive from mdr socket");
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

            if (header_args[0].equals("CHUNK")) {

                try {
                    bq.put("do not send");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }

        }
    }
}
