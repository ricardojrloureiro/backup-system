package Server;

import Auxiliar.Partials;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;


public class ReceiveThread extends Thread {

    private String currentDir;
    private InetAddress mc_address;
    private InetAddress mdb_address;
    private InetAddress mcr_address;
    private int mc_port;
    private int mdb_port;
    private int mcr_port;

    private boolean state;

    private MulticastSocket mc_socket, mdb_socket, mcr_socket;

    public ReceiveThread(String mc, int mc_port, String mdb, int mdb_port, String mcr, int mcr_port, String dir)
            throws IOException {
        state=true;

        this.mc_address = InetAddress.getByName(mc);
        this.mc_port = mc_port;


        this.mdb_address = InetAddress.getByName(mdb);
        this.mdb_port = mdb_port;


        this.mcr_address = InetAddress.getByName(mcr);
        this.mcr_port = mcr_port;


        //sockets initialization
        this.mc_socket = new MulticastSocket(this.mc_port);
        this.mc_socket.setTimeToLive(1);

        this.mdb_socket = new MulticastSocket(this.mdb_port);
        this.mdb_socket.setTimeToLive(1);

        this.mcr_socket = new MulticastSocket(this.mcr_port);
        this.mcr_socket.setTimeToLive(1);

        this.currentDir = dir;

    }

    public void run() {
        try {
            mc_socket.joinGroup(mc_address);
            mdb_socket.joinGroup(mdb_address);
            mcr_socket.joinGroup(mcr_address);
            
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] buf = new byte[65000];

        while(state) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                mdb_socket.receive(packet);
                mc_socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if(packet.getAddress().equals(InetAddress.getLocalHost())) {
                    System.out.println("� meu!");
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }


            String header = null;
            byte[] body = null;

            ArrayList<Object> splitMessage = parseMessage(packet.getData(), packet.getLength());

            header = (String) splitMessage.get(0);
            body = (byte[]) splitMessage.get(1);

            //print results
            System.out.println("HEADER: ");
            System.out.println(header);

            //split the header to fetch the fileId
            String[] header_args = header.split(" ");

            if (header_args[0].equals("PUTCHUNK")) {
                String fileName = header_args[2];

                //save file in storage if there is enough available space
                if(Partials.updateConfFile(currentDir, header_args, body)) {
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

            } else if (header_args[0].equals("STORED")) {
                try {
                    System.out.println("Incrementing value stored");
                    Partials.changeRepDegree(currentDir, header_args[2]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

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

    private ArrayList<Object> parseMessage(byte[] data, int length) {
        ArrayList<byte[]> header_body = splitMessageData(data,length);

        byte[] header_data = header_body.get(0);
        byte[] body_data = header_body.get(1);

        System.out.println("Body size receiver: " + body_data.length);

        ArrayList<Object> splitMessage = new ArrayList<>();

        splitMessage.add(new String(header_data,0,header_data.length));
        splitMessage.add(body_data);

        return splitMessage;
    }

    private ArrayList<byte[]> splitMessageData(byte[] data, int length) {
        ArrayList<byte[]> header_body = new ArrayList<>();

        for(int i = 0; i < data.length; i++) {
            if(data[i] == 0xD && data[i+1] == 0xA) { //first CR char of the separator
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