package Client;

import Server.ReceiveThread;
import Server.SendThread;

import java.io.IOException;

public class Peer {

    public static void main(String[] args) throws IOException {

        if(args.length != 6) {
            System.out.println("Usage: java Peer <Multicast Control Channel>" +
                    " <port> <Muticast Backup Channel> <port> <Muticast Restore Channel> <port>");

            System.exit(1);
        }

        new ReceiveThread(args[0],Integer.parseInt(args[1]),
                args[2],Integer.parseInt(args[3]),
                args[4],Integer.parseInt(args[5])).start();

        new SendThread(args[0],Integer.parseInt(args[1]),
                args[2],Integer.parseInt(args[3]),
                args[4],Integer.parseInt(args[5])).start();
    }

}
