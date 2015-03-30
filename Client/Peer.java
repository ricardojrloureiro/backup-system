package Client;

import Auxiliar.Partials;
import Server.ReceiverCreator;
import Server.SendThread;

import java.io.IOException;

public class Peer {

    private String currentDir;

    public static void main(String[] args) throws IOException {

        if(args.length != 8) {
            System.out.println("Usage: java Peer <Multicast Control Channel>" +
                    " <port> <Muticast Backup Channel> <port> <Muticast Restore Channel> <port> <dir> <space>");

            System.exit(1);
        }

        Partials.createPeerDirectory(args[6],args[7]);

        new SendThread(args[0],Integer.parseInt(args[1]),
                args[2],Integer.parseInt(args[3]),
                args[4],Integer.parseInt(args[5]),"Peers/"+args[6]).start();


        new ReceiverCreator(args[0],Integer.parseInt(args[1]),
                args[2],Integer.parseInt(args[3]),
                args[4],Integer.parseInt(args[5]),"Peers/"+args[6]);
    }

}
