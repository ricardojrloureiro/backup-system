package Auxiliar;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

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
            out.println("Version,FileId,ChunkNo,RepDegMin,RepDegAct,currentSpace");
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
                writer.write(header_args[1] + "," + header_args[2]+","+header_args[3]+","
                        +header_args[4]+","+"0"+","+String.valueOf(difference));
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

            String[] separatedLine = last.split(",");

            return Integer.parseInt(separatedLine[separatedLine.length-1]);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static void changeRepDegree(String currentDir, String filename, String chunkNo)
            throws IOException {

        BufferedReader input = new BufferedReader(new FileReader(currentDir + "/conf.csv"));
        String line, fullData="";
        String lineToUpdate = null;
        String expectedChunk = chunkNo;

        while((line=input.readLine()) != null) {
            String[] separatedLine = line.split(",");
            if(separatedLine != null) {
                if(separatedLine[1].equals(filename)) {

                    System.out.println("#expected" + chunkNo +  "#" +chunkNo.length() + );
                    System.out.println("#actual" + separatedLine[2] + "#" + separatedLine[2].length());

                    String[] split = line.split(",");
                    split[4] = String.valueOf(Integer.parseInt(split[4])+1);
                    line = "";
                    for(int i=0;i<split.length;i++) {
                        line += split[i];
                        if(i+1 < split.length) {
                            line += ",";
                        }
                    }
                }
                System.out.println("new line is: " + line);
                fullData += line + '\n';
            }
        }
        input.close();

        FileOutputStream fileOut = new FileOutputStream(currentDir + "/conf.csv");
        fileOut.write(fullData.getBytes());
        fileOut.close();
    }

    public static ArrayList<Object> parseMessage(byte[] data, int length) {
        ArrayList<byte[]> header_body = splitMessageData(data,length);

        byte[] header_data = header_body.get(0);
        byte[] body_data = header_body.get(1);

        System.out.println("Body size receiver: " + body_data.length);

        ArrayList<Object> splitMessage = new ArrayList<>();

        splitMessage.add(new String(header_data, 0, header_data.length));
        splitMessage.add(body_data);

        return splitMessage;
    }

    private static ArrayList<byte[]> splitMessageData(byte[] data, int length) {
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

    public static boolean chunkExists(String version, String fileId, String chunkNo, String dir) {

        System.out.println("Inside Chunk Exists");

        System.out.println(version + "SIM");
        System.out.println(fileId + "SIM");
        System.out.println(chunkNo + "SIM");
        System.out.println(dir + "SIM");

        String filePath = dir + "/" + chunkNo + "-" + fileId;
        String[] splitPath = filePath.split(" ");
        for(String s: splitPath) {
            System.out.println(s);
        }
        System.out.println(filePath);

        File f = new File(filePath);
        if(f.exists() && !f.isDirectory()) {
            return true;
        }

        return false;
    }
}
