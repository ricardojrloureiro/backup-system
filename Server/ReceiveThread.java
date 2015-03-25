package Server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Collections;


public class ReceiveThread extends Thread {

    private InetAddress mc_address;
    private InetAddress mcb_address;
    private InetAddress mcr_address;
    private int mc_port;
    private int mcb_port;
    private int mcr_port;

    private boolean state;

    private MulticastSocket mc_socket, mcb_socket, mcr_socket;

    public ReceiveThread(String mc, int mc_port, String mcb, int mcb_port, String mcr, int mcr_port) throws IOException {
        state=true;

        this.mc_address = InetAddress.getByName(mc);
        this.mc_port = mc_port;


        this.mcb_address = InetAddress.getByName(mcb);
        this.mcb_port = mcb_port;


        this.mcr_address = InetAddress.getByName(mcr);
        this.mcr_port = mcr_port;


        //sockets initialization
        this.mc_socket = new MulticastSocket(this.mc_port);
        this.mc_socket.setTimeToLive(1);

        this.mcb_socket = new MulticastSocket(this.mcb_port);
        this.mcb_socket.setTimeToLive(1);

        this.mcr_socket = new MulticastSocket(this.mcr_port);
        this.mcr_socket.setTimeToLive(1);

    }

    public void run() {
        try {
            mc_socket.joinGroup(mc_address);
            mcb_socket.joinGroup(mcb_address);
            mcr_socket.joinGroup(mcr_address);
            
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] buf = new byte[65000];

        while(state) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                mc_socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //parse the message
            String header = null;
            String body = null;

            ArrayList<String> splitMessage = parseMessage(packet.getData(),packet.getLength());

            header = splitMessage.get(0);
            body = splitMessage.get(1);

            //print results
            System.out.println("HEADER: ");
            System.out.println(header);
            //System.out.println("BODY: ");
            //System.out.println(body);

            saveChunk(body);

            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private void saveChunk(String body) {
        new File("teste");
        PrintWriter out = null;

        try {
            out = new PrintWriter("teste");
            out.println(body);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        finally {
            out.close();
        }
    }

    private ArrayList<String> parseMessage(byte[] data, int length) {
        ArrayList<byte[]> header_body = splitMessageData(data,length);

        byte[] header_data = header_body.get(0);
        byte[] body_data = header_body.get(1);

        ArrayList<String> splitMessage = new ArrayList<>();

        splitMessage.add(new String(header_data,0,header_data.length));
        splitMessage.add(new String(body_data,0,body_data.length));

        return splitMessage;
    }

    private ArrayList<byte[]> splitMessageData(byte[] data, int length) {
        ArrayList<byte[]> header_body = new ArrayList<>();

        for(int i = 0; i < data.length; i++) {
            if(data[i] == 0xD) { //first CR char of the separator
                byte[] header = new byte[i];
                byte[] body = new byte[length-i-4];

                System.arraycopy(data, 0, header, 0, i - 1);
                System.arraycopy(data,i+4,body,0,length-i-4); //advance the separator

                header_body.add(header);
                header_body.add(body);

                break;
            }
        }

        return header_body;
    }

}