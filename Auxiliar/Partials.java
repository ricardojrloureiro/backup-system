package Auxiliar;

import javax.annotation.processing.SupportedSourceVersion;
import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class Partials {

    public static String version = "1.0";

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
            out.println("Version,FileId,ChunkNo,RepDegMin,RepDegAct,chunkSize,currentSpace");
            out.println(",,,,,," + space);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        finally {
            out.close();
        }
    }

    public static boolean updateConfFile(String currentDir, String[] header_args, byte[] bytes) throws IOException {

        trimArray(header_args);
        currentDir = currentDir.trim();

        BufferedWriter writer;


        try {
            writer = new BufferedWriter(new FileWriter(currentDir + "/conf.csv", true));

            //gets previous available space
            int currentSpace = getCurrentSpace(currentDir);
            int difference = currentSpace - bytes.length;

            if (difference > 0) {
                //updates file
                writer.write(header_args[1] + "," + header_args[2] + "," + header_args[3] + "," +
                        header_args[4] + "," + "0" + "," + bytes.length + "," + String.valueOf(difference));
                writer.newLine();
                writer.close();
                return true;
            } else {
                writer.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


        return false;
    }

    public static boolean chunkExistsInFile(String currentDir, String[] header_args) throws IOException {
        currentDir = currentDir.trim();
        trimArray(header_args);

        BufferedReader input = new BufferedReader(new FileReader(currentDir + "/conf.csv"));
        String line;

        while((line=input.readLine()) != null) {
            String[] separatedLine = line.split(",");
            if(separatedLine != null) {
                if(separatedLine[1].equals(header_args[2]) && separatedLine[2].equals(header_args[3])) {
                    input.close();
                    return true;
                }
            }
        }
        input.close();

        return false;
    }

    private static void trimArray(String[] array) {
        for(int i = 0; i < array.length; i++) {
            array[i] = array[i].trim();
        }
    }

    private static int getCurrentSpace(String currentDir) {
        try {
            BufferedReader input = new BufferedReader(new FileReader(currentDir + "/conf.csv"));

            String last = null, line;

            while ((line = input.readLine()) != null) {
                last = line;
            }

            if(last!=null) {
                String[] separatedLine = last.split(",");

                input.close();

                return Integer.parseInt(separatedLine[separatedLine.length-1]);
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static void changeRepDegree(String currentDir, String filename, String chunkNo)
            throws IOException {

        currentDir = currentDir.trim();
        filename = filename.trim();
        chunkNo = chunkNo.trim();

        BufferedReader input = new BufferedReader(new FileReader(currentDir + "/conf.csv"));
        String line, fullData="";
        String expectedChunk = chunkNo.trim();

        while((line=input.readLine()) != null) {
            String[] separatedLine = line.split(",");
            if(separatedLine != null) {
                if(separatedLine[1].equals(filename) && expectedChunk.equals(separatedLine[2])) {
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
                fullData += line + '\n';
            }
        }
        input.close();

        FileOutputStream fileOut = new FileOutputStream(currentDir + "/conf.csv");
        fileOut.write(fullData.getBytes());
        fileOut.close();
    }

    public static ArrayList<Object> parseMessage(byte[] data, int length) {
        ArrayList<byte[]> header_body = splitMessageData(data, length);

        byte[] header_data = header_body.get(0);
        byte[] body_data = header_body.get(1);

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

        String filePath = (dir + "/" + chunkNo + "-" + fileId).trim();
        File f = new File(filePath);
        if(f.exists() && !f.isDirectory()) {
            return true;
        }
        return false;
    }

    public static Chunk getChunkFromFile(String version, String fileId, String chunkNo, String dir) throws IOException {
        String filePath = (dir + "/" + chunkNo + "-" + fileId).trim();

        byte[] content = Files.readAllBytes(Paths.get(filePath));

        return new Chunk(Integer.parseInt(chunkNo),content);
    }

    public static void appendChunk(byte[] buf, String dir, String fileName) throws IOException {
        String filePath = (dir + "/" + fileName).trim();
        File f = new File(filePath);

        if(!f.exists()) {
            f.createNewFile();
        }

        FileOutputStream fileWriter = new FileOutputStream(filePath,true);
        fileWriter.write(buf);
        fileWriter.close();

    }

    public static void deleteChunks(String version, String fileId, String dir) throws IOException {

        version = version.trim();
        fileId = fileId.trim();
        dir = dir.trim();

        BufferedReader input = new BufferedReader(new FileReader(dir + "/conf.csv"));
        String line, fullData="";
        int toAdd = 0;
        while((line=input.readLine()) != null) {

            String[] separatedLine = line.split(",");

            if (separatedLine != null) {

                if (separatedLine[1].equals(fileId) && separatedLine[0].equals(version)) {
                    String[] split = line.split(",");

                    toAdd += Integer.parseInt(split[5]);

                    deleteChunk(split[2].trim(), split[1].trim(), dir.trim());

                    line = "";

                } else {

                    String[] split = line.split(",");

                    if (!split[6].equals("currentSpace") && !split[6].equals("")){//first two lines
                        split[6] = String.valueOf(Integer.parseInt(split[6]) + toAdd);
                    }

                    line = "";

                    for (int i = 0; i < split.length; i++) {
                        line += split[i];
                        if (i + 1 < split.length) {
                            line += ",";
                        }
                    }

                }

                    if (!line.equals(""))
                        fullData += line + '\n';

            }
        }
        input.close();

        FileOutputStream fileOut = new FileOutputStream(dir + "/conf.csv");
        fileOut.write(fullData.getBytes());
        fileOut.close();
    }

    private static void deleteChunk(String chunkNo, String fileId, String dir) {
        System.out.println("Deleting chunk");
        File f = new File(dir + "/" + chunkNo + "-" + fileId);

        if(f.delete()) {
            System.out.println("File " + dir + "/" + chunkNo + "-" + fileId + " deleted successfully");
        }
        else {
            System.out.println("Could not delete file " + dir + "/" + chunkNo + "-" + fileId);
        }
    }

    public static int removeChunk(String currentDir, MulticastSocket mc_socket, InetAddress mc_address, int mc_port) throws IOException {

        currentDir = currentDir.trim();
        int chunkSize = 64000;

        int lineNo = getChunkWithHighDeg(currentDir);
        if(lineNo != -1) {

            BufferedReader input = new BufferedReader(new FileReader(currentDir + "/conf.csv"));
            String line, fullData = "";
            int toAdd = 0, currentLine = 0;
            String[] lineToReturn = new String[7];


            while ((line = input.readLine()) != null) {

                String[] separatedLine = line.split(",");

                if (separatedLine != null) {

                    if (currentLine == lineNo) {

                        String[] split = line.split(",");
                        System.arraycopy(split, 0, lineToReturn, 0, split.length);
                        toAdd += Integer.parseInt(split[5]);
                        chunkSize = toAdd;
                        deleteChunk(split[2].trim(), split[1].trim(), currentDir.trim());
                        sendRemovedMessage(split, mc_socket, mc_address, mc_port);

                        line = "";

                    } else {

                        String[] split = line.split(",");

                        if (!split[6].equals("currentSpace") && !split[6].equals("")) {//first two lines
                            split[6] = String.valueOf(Integer.parseInt(split[6]) + toAdd);
                        }

                        line = "";

                        for (int i = 0; i < split.length; i++) {
                            line += split[i];
                            if (i + 1 < split.length) {
                                line += ",";
                            }
                        }

                    }

                    if (!line.equals(""))
                        fullData += line + '\n';

                }
                currentLine++;
            }
            input.close();

            FileOutputStream fileOut = new FileOutputStream(currentDir + "/conf.csv");
            fileOut.write(fullData.getBytes());
            fileOut.close();
        }

        return chunkSize;
    }

    private static int getChunkWithHighDeg(String currentDir) throws IOException {

        currentDir = currentDir.trim();

        BufferedReader input = new BufferedReader(new FileReader(currentDir + "/conf.csv"));
        String line;
        int maxDeg = -9999;
        int lineNo = 0;
        int lineMax = 2;

        while((line=input.readLine()) != null) {

            String[] separatedLine = line.split(",");

            if (separatedLine != null) {

                if (!separatedLine[4].equals("RepDegAct") && !separatedLine[4].equals("")){//first two lines
                    int deg = Integer.parseInt(separatedLine[4]) - Integer.parseInt(separatedLine[3]);
                    if(deg > maxDeg){
                        maxDeg = deg;
                        lineMax = lineNo;
                    }
                }
            }
            lineNo++;
        }
        input.close();

        if(lineNo < 2)
            return -1;

        return lineMax;
    }

    public static boolean checkRepDegree(String currentDir, String fileIdRemoved, String chunkNoRemoved) throws IOException {

        currentDir = currentDir.trim();
        fileIdRemoved = fileIdRemoved.trim();
        chunkNoRemoved = chunkNoRemoved.trim();

        BufferedReader input = new BufferedReader(new FileReader(currentDir + "/conf.csv"));
        String line, fullData="";
        boolean value = false;
        String expectedChunk = chunkNoRemoved.trim();

        while((line=input.readLine()) != null) {
            String[] separatedLine = line.split(",");
            if(separatedLine != null) {
                if(separatedLine[1].equals(fileIdRemoved) && expectedChunk.equals(separatedLine[2])) {

                    String[] split = line.split(",");
                    split[4] = String.valueOf(Integer.parseInt(split[4])-1);
                    line = "";
                    for(int i=0;i<split.length;i++) {
                        line += split[i];
                        if(i+1 < split.length) {
                            line += ",";
                        }
                    }

                    if(Integer.parseInt(split[3])<Integer.parseInt(split[4])) {
                        value=true;
                    }else {
                        value=false;
                    }

                }
                fullData += line + '\n';
            }
        }
        input.close();
        FileOutputStream fileOut = new FileOutputStream(currentDir + "/conf.csv");
        fileOut.write(fullData.getBytes());
        fileOut.close();

        return value;
    }

    private static void sendRemovedMessage(String[] removed, MulticastSocket mc_socket, InetAddress mc_address, int mc_port) {
        System.out.println("Sending removed message.");
        StringBuilder builder = new StringBuilder();

        builder.append("REMOVED " + removed[0] + " " + removed[1] + " " + removed[2] + " ");

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
            System.out.println("Sent REMOVED");
        } catch (IOException e) {
            System.out.println("Could not send REMOVED message on MC socket");
            e.printStackTrace();
        }
    }

    public static void createPeerStoredFile(String name) {
        File stored = new File( "Peers/" + name + "/stores.csv");

        PrintWriter out = null;

        try {
            out = new PrintWriter(stored);
            out.println("IP,ChunkNo,FileId");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        finally {
            out.close();
        }
    }

    public static boolean gotStored(String address, String[] header_args, String currentDir) throws IOException {
        currentDir = currentDir.trim();
        trimArray(header_args);

        BufferedReader input = new BufferedReader(new FileReader(currentDir + "/stores.csv"));
        String line;

        while((line=input.readLine()) != null) {
            String[] separatedLine = line.split(",");
            if(separatedLine != null) {
                if(separatedLine[0].equals(address) && separatedLine[1].equals(header_args[3]) && separatedLine[2].equals(header_args[2])) {
                    input.close();
                    return true;
                }
            }
        }
        input.close();

        return false;
    }

    public static void addToStoredFile(String address, String[] header_args, String currentDir) {
        trimArray(header_args);
        currentDir = currentDir.trim();

        BufferedWriter writer;

        try {
            writer = new BufferedWriter(new FileWriter(currentDir + "/stores.csv", true));

            writer.write(address + "," + header_args[3] + "," + header_args[2]);
            writer.newLine();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
