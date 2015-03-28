package Server;

import java.io.IOException;

/**
 * Created by Tiago on 28-03-2015.
 */
public class ReceiverCreator {
    public ReceiverCreator(String mc, int mc_port, String mdb, int mdb_port, String mdr, int mdr_port, String dir) throws IOException {

        new MCReceiverThread(mc,mc_port,mdb,mdb_port,mdr,mdr_port,dir).start();
        new MDRReceiverThread(mc,mc_port,mdb,mdb_port,mdr,mdr_port,dir).start();
        new MDBReceiverThread(mc,mc_port,mdb,mdb_port,mdr,mdr_port,dir).start();

    }
}
