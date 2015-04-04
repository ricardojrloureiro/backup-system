package Server;

import Auxiliar.Chunk;
import Auxiliar.Partials;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;

public class MCReceiverThread extends Thread {

    private String currentDir;
    private InetAddress mc_address;
    private InetAddress mdb_address;
    private InetAddress mdr_address;
    private int mc_port;
    private int mdb_port;
    private int mdr_port;

    private boolean state;

    private MulticastSocket mc_socket, mdb_socket, mdr_socket;

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

        this.mdr_socket = new MulticastSocket(this.mdr_port);
        this.mdr_socket.setTimeToLive(1);

        this.currentDir = dir;
    }

    public void run() {
        try {
            mc_socket.joinGroup(mc_address);
            mdr_socket.joinGroup(mdr_address);
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
            System.out.println();
            System.out.println("Receiving message with header: ");
            System.out.println(header);
            System.out.println();

            //split the header to fetch the fileId
            String[] header_args = header.split(" ");

            if (header_args[0].equals("STORED")) {
                try {
                    if(!Partials.gotStored(packet.getAddress().getHostAddress(),header_args,currentDir)){
                        System.out.println("Incrementing chunk replication degree");
                        Partials.changeRepDegree(currentDir, header_args[2], header_args[3]);
                        Partials.addToStoredFile(packet.getAddress().getHostAddress(),header_args,currentDir);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if(header_args[0].equals("GETCHUNK")) {
                if(Partials.chunkExists(header_args[1],header_args[2],header_args[3].trim(),currentDir)) {
                    System.out.println("Chunk exists in this file system.");

                    int random = new Random().nextInt(401);
                    byte[] rbuf = new byte[65000];
                    DatagramPacket rpacket = new DatagramPacket(rbuf, rbuf.length);
                    try {
                        mdr_socket.setSoTimeout(random);
                        String chunkNumberPretended = header_args[3].trim();
                        String fileIdPretended = header_args[2].trim();
                        boolean notReceiving=true;
                        while(notReceiving) {
                            mdr_socket.receive(rpacket);
                            String header_received = (String) Partials.parseMessage(rpacket.getData(),
                                    rpacket.getLength()).get(0);
                            String[] split = header_received.split(" ");
                            if(split[2].equals(fileIdPretended) &&
                                    split[3].equals(chunkNumberPretended)) {
                                System.out.println("Already sent a chunk");
                                notReceiving=false;
                            }
                        }
                    } catch (SocketTimeoutException e) {
                        Chunk toSend = null;
                        try {
                            System.out.println("Sent chunk.");
                            toSend = Partials.getChunkFromFile(header_args[1], header_args[2], header_args[3].trim(), currentDir);
                            sendChunk(toSend, header_args);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    } catch (SocketException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if(header_args[0].equals("DELETE")) {
                System.out.println("Deleting chunks.");
                //delete all chunks from that file
                try {
                    Partials.deleteChunks(header_args[1], header_args[2], currentDir);
                } catch (IOException e) {
                    System.out.println("Could not delete chunks");
                    e.printStackTrace();
                }
            }
            else if(header_args[0].equals("REMOVED")) {
                String fileIdRemoved = header_args[2].trim();
                String chunkNoRemoved = header_args[3].trim();

                try {
                    if(Partials.checkRepDegree(currentDir, fileIdRemoved,chunkNoRemoved))
                        //true if degree is lower then required
                    {
                        try {
                            Chunk toSend = Partials.getChunkFromFile("1",fileIdRemoved,chunkNoRemoved,currentDir);
                            byte[] putchunk = createPutchunkMessage(fileIdRemoved, chunkNoRemoved, toSend);

                            packet = new DatagramPacket(putchunk, putchunk.length, mdb_address, mdb_port);
                            mdb_socket.send(packet);
                            System.out.println("Re-send chunk with the id of #" + chunkNoRemoved);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("No need to re-send chunk");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }

    }

    private void sendChunk(Chunk toSend, String[] header_args) throws IOException {
        byte[] buf;

        buf = createChunkMessage(toSend, header_args);

        DatagramPacket packet = new DatagramPacket(buf, buf.length, mdr_address, mdr_port);
        mdr_socket.send(packet);
    }

    private byte[] createChunkMessage(Chunk chunk, String[] message_args) {

        String command = "CHUNK " + message_args[1] + " " + message_args[2] + " " +
                chunk.getChunkNumber() + " ";
        byte[] commandBytes = command.getBytes();

        //build termination token
        byte[] crlf = Partials.createCRLFToken();

        //header
        byte[] header = new byte[commandBytes.length + crlf.length];
        System.arraycopy(commandBytes, 0, header, 0, commandBytes.length);
        System.arraycopy(crlf, 0, header, commandBytes.length, crlf.length);

        //body
        byte[] body = chunk.getBody();

        //final message
        byte[] message = new byte[header.length + body.length];
        System.arraycopy(header, 0, message, 0, header.length);
        System.arraycopy(body, 0, message, header.length, body.length);

        return message;

    }

    private byte[] createPutchunkMessage(String fileId, String chunkNo,Chunk chunk ){
        String command = "PUTCHUNK " + "1.0 " + fileId + " " + chunkNo + " " +
                chunk.getChunkNumber() + " ";
        byte[] commandBytes = command.getBytes();

        //build termination token
        byte[] crlf = Partials.createCRLFToken();

        //header
        byte[] header = new byte[commandBytes.length + crlf.length];
        System.arraycopy(commandBytes, 0, header, 0, commandBytes.length);
        System.arraycopy(crlf, 0, header, commandBytes.length, crlf.length);

        //body
        byte[] body = chunk.getBody();

        //final message
        byte[] message = new byte[header.length + body.length];
        System.arraycopy(header, 0, message, 0, header.length);
        System.arraycopy(body, 0, message, header.length, body.length);

        return message;
    }

}
