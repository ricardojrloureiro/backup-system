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

            switch(message){
                case "PUTCHUNK": {
                    System.out.println("Chunk command prompted");
                    sendChunks();
                }
                break;

                default: {
                    System.out.println("Default command prompted");
                } break;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendChunks() throws IOException, InterruptedException {
        PartitionedFile fullFile = new PartitionedFile("/Users/ricardo/Desktop/melo.png");
        ArrayList<Chunk> chunks = fullFile.getChunks();

        for (int i = 0; i < chunks.size(); i++) {
            // ADD HEADER - tem de se criar a mensagem e passar para bytes e adicionar ao buf
            byte[] buf = chunks.get(i).getBody();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, mc_address, mc_port);
            mc_socket.send(packet);
            sleep(2000); // sleeps after sending one chunk
        }
    }

    public void header() {

    }
}