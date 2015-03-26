package Auxiliar;

import java.io.*;

/**
 * Created by Tiago on 26/03/2015.
 */
public class Partials {

    public static byte[] createCRLFToken() {
        byte[] crlf = new byte[4];
        crlf[0] = 0xD;
        crlf[1] = 0xA;
        crlf[2] = 0xD;
        crlf[3] = 0xA;

        return crlf;
    }

    public static void createPeerDirectory(String name, String space) {
        File dir = new File("Peers");

        if(!dir.exists()) {
            try {
                dir.mkdir();
            }
            catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        File peerDir = new File("Peers/"+name);

        if(!peerDir.exists()) {
            try{
                peerDir.mkdir();
                createConfigFile("Peers/"+name, Integer.parseInt(space));
            } catch (SecurityException e) {

            }
        }

    }

    private static void createConfigFile(String dir,int space) {
        File config = new File( dir + "/conf.csv");

        PrintWriter out = null;

        try {
            out = new PrintWriter(config);
            out.println("Version,ChunkNo,FileId,RepDegMin,RepDegAct,currentSpace");
            out.println(",,,,," + space);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        finally {
            out.close();
        }
    }

    public static boolean updateConfFile(String currentDir, String[] header_args, byte[] bytes) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(currentDir + "/conf.csv",true));

            //gets previous avaiable space
            int currentSpace = getCurrentSpace(currentDir);
            int difference = currentSpace-bytes.length;

            if(difference>0){
                //updates file
                writer.write(header_args[1] + "," + header_args[2]+","+header_args[3]+","+header_args[4]+","+"2"+","+String.valueOf(difference));
                writer.newLine();
                writer.close();
                return true;
            }
            else {
                writer.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private static int getCurrentSpace(String currentDir) {
        try {
            BufferedReader input = new BufferedReader(new FileReader(currentDir + "/conf.csv"));

            String last = null, line;

            while ((line = input.readLine()) != null) {
                last = line;
            }

            String[] seperatedLine = last.split(",");

            return Integer.parseInt(seperatedLine[seperatedLine.length-1]);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }
}
