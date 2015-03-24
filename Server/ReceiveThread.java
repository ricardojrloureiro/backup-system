package Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;


public class ReceiveThread extends Thread {

    private InetAddress mc_address;
    private InetAddress mcb_address;
    private InetAddress mcr_address;
    private int mc_port;
    private int mcb_port;
    private int mcr_port;

    private MulticastSocket mc_socket, mcb_socket, mcr_socket;

    public ReceiveThread(String mc, int mc_port, String mcb, int mcb_port, String mcr, int mcr_port) throws IOException {
        System.out.println("Receive thread info " + "mc:" + mc + ", port:" + mc_port);

        this.mc_address = InetAddress.getByName(mc);
        this.mc_port = mc_port;

        /*this.mcb_address = InetAddress.getByName(mcb);
        this.mcb_port = mcb_port;

        this.mcr_address = InetAddress.getByName(mcr);
        this.mcr_port = mcr_port;*/

        //sockets initialization
        this.mc_socket = new MulticastSocket(this.mc_port);
        this.mc_socket.setTimeToLive(1);

      /*  this.mcb_socket = new MulticastSocket(this.mcb_port);
        this.mcb_socket.setTimeToLive(1);

        this.mcr_socket = new MulticastSocket(this.mcr_port);
        this.mcr_socket.setTimeToLive(1);*/

    }

    public void run() {

        try {
            mc_socket.joinGroup(mc_address);
           /* mcb_socket.joinGroup(mcb_address);
            mcr_socket.joinGroup(mcr_address); */

            byte[] buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            mc_socket.receive(packet);

            String received = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Received: " + received);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}