package Server;

import Auxiliar.Partials;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

public class MDBReceiverThread extends Thread {

    private String currentDir;
    private InetAddress mc_address;
    private InetAddress mdb_address;
    private InetAddress mdr_address;
    private int mc_port;
    private int mdb_port;
    private int mdr_port;


    private MulticastSocket mc_socket, mdb_socket, mcr_socket;

    public MDBReceiverThread(String mc, int mc_port, String mdb, int mdb_port, String mdr, int mdr_port, String dir) throws IOException {
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
            mdb_socket.joinGroup(mdb_address);
        } catch (IOException e) {
            System.out.println("Could not join mdb group");
            e.printStackTrace();
        }

        byte[] buf = new byte[65000];

        while(true) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                mdb_socket.receive(packet);
            } catch (IOException e) {
                System.out.println("Could not receive from mdb socket");
                e.printStackTrace();
            }


            String header;
            byte[] body;

            ArrayList<Object> splitMessage = Partials.parseMessage(packet.getData(), packet.getLength());

            header = (String) splitMessage.get(0);
            body = (byte[]) splitMessage.get(1);

            //print results
            System.out.println("HEADER: ");
            System.out.println(header);
            System.out.println();

            //split the header to fetch the fileId
            String[] header_args = header.split(" ");

            if (header_args[0].equals("PUTCHUNK")) {
                String fileName = header_args[2];
                try {
                    InetAddress localHost = InetAddress.getLocalHost();
                    InetAddress receivingHost = packet.getAddress();
                    if(!receivingHost.equals(localHost)){
                        if(!Partials.chunkExistsInFile(currentDir, header_args)) {
                            System.out.println("Chunk does not exist");
                            //save file in storage if there is enough available space
                            if (Partials.updateConfFile(currentDir, header_args, body)) {
                                storeChunk(body, header_args, fileName);
                            }
                        } else {
                            Random r = new Random();
                            int delay = r.nextInt(401);
                            try {
                                sleep(delay);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            System.out.println("Sending STORE");
                            sendStoredMessage(header_args);
                        }
                     }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void storeChunk(byte[] body, String[] header_args, String fileName) {
        try {
            saveChunk(body, header_args[3] + "-" + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Random r = new Random();
        int delay = r.nextInt(401);
        try {
            sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Sending STORE");
        sendStoredMessage(header_args);
    }

    private void sendStoredMessage(String[] header_args) {
        StringBuilder builder = new StringBuilder();

        builder.append("STORED " + header_args[1] + " " + header_args[2] + " " + header_args[3] + " ");

        String command = builder.toString(); //join all the arguments of the command into a string
        byte[] commandBytes = command.getBytes();

        //build termination token
        byte[] crlf = Partials.createCRLFToken();

        //message
        byte[] message = new byte[commandBytes.length + crlf.length];
        System.arraycopy(commandBytes, 0, message, 0, commandBytes.length);
        System.arraycopy(crlf, 0, message, commandBytes.length, crlf.length);

        DatagramPacket packet = new DatagramPacket(message, message.length, mc_address, mc_port);
        try {
            mc_socket.send(packet);
        } catch (IOException e) {
            System.out.println("Could not send STORED message on MC socket");
            e.printStackTrace();
        }
    }

    private void saveChunk(byte[] body, String fileName) throws IOException {

        OutputStream out = null;

        try {
            out = new BufferedOutputStream(new FileOutputStream(this.currentDir + "/" + fileName));
            out.write(body);
        } finally {
            if (out != null) out.close();
        }

    }


}
