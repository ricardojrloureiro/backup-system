package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class SendThread extends Thread {

    private InetAddress mc_address;
    private int mc_port;
    private MulticastSocket socket;


    public SendThread(String mc, int mc_port) throws IOException {
        System.out.println("Send Thread info " + "mc:" + mc + ", port:" + mc_port);
        this.mc_address = InetAddress.getByName(mc);
        this.mc_port = mc_port;
        this.socket = new MulticastSocket(mc_port);
        this.socket.setTimeToLive(1);
    }

    public void run() {

        //read message from console
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        String message = null;
        byte[] buf = new byte[256];

        try {
            System.out.print("Message: ");
            message = br.readLine();
            buf = message.getBytes();

            //send message to group
            DatagramPacket packet = new DatagramPacket(buf, buf.length, mc_address, mc_port);
            socket.send(packet);
            System.out.println("Sent: " + message);

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

}