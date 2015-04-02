package Server;

import java.io.IOException;

public class ReceiverCreator {
    public ReceiverCreator(String mc, int mc_port, String mdb, int mdb_port, String mdr,
                           int mdr_port, String dir) throws IOException {



        //creates the 3 channels required for the different sockets
        Thread mc_thread = new MCReceiverThread(mc,mc_port,mdb,mdb_port,mdr,mdr_port,dir);
        mc_thread.start();
        //new MDRReceiverThread(mc,mc_port,mdb,mdb_port,mdr,mdr_port,dir,mc_thread).start();
        new MDBReceiverThread(mc,mc_port,mdb,mdb_port,mdr,mdr_port,dir).start();
    }
}
