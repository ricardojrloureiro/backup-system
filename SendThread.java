import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

/**
 * Created by Tiago on 23/03/2015.
 */
public class SendThread extends Thread {

    private InetAddress mc_address;
    private int mc_port;
    private MulticastSocket socket;


    public SendThread(String mc, int mc_port) throws IOException {
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

            message = br.readLine();

            //send message to group
            DatagramPacket packet = new DatagramPacket(buf, buf.length, mc_address, mc_port);
            System.out.println("Sent: " + message);
            socket.send(packet);

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }


    }

}
