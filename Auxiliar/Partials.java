package Auxiliar;

import javax.annotation.processing.SupportedSourceVersion;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

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
            out.println("Version,FileId,ChunkNo,RepDegMin,RepDegAct,chunkSize,currentSpace");
            out.println(",,,,,," + space);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        finally {
            out.close();
        }
    }

    public static boolean updateConfFile(String currentDir, String[] header_args, byte[] bytes) {

        trimArray(header_args);

        BufferedWriter writer;

        try {
            writer = new BufferedWriter(new FileWriter(currentDir + "/conf.csv",true));

            //gets previous available space
            int currentSpace = getCurrentSpace(currentDir);
            int difference = currentSpace-bytes.length;

            System.out.println("DIFFERENCE: " + difference);

            if(difference>0){
                //updates file
                writer.write(header_args[1] + "," + header_args[2]+ "," + header_args[3]+ "," +
                             header_args[4] + "," +      "0"      + "," + bytes.length + "," + String.valueOf(difference));
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

            String[] separatedLine = last.split(",");

            input.close();

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
        ArrayList<byte[]> header_body = splitMessageData(data,length);

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
        System.out.println("File Path: " + filePath);
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
        System.out.println("Changing conf file");
        while((line=input.readLine()) != null) {

            String[] separatedLine = line.split(",");

            if (separatedLine != null) {

                if (separatedLine[1].equals(fileId) && separatedLine[0].equals(version)) {
                    System.out.println("Entered if");
                    String[] split = line.split(",");

                    toAdd += Integer.parseInt(split[5]);
                    System.out.println("to add: " + toAdd);

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

    public static String[] removeChunk(String currentDir) throws IOException {

        int lineNo = getChunkWithHighDeg(currentDir);
        System.out.println("lineNo: " + lineNo);

        BufferedReader input = new BufferedReader(new FileReader(currentDir + "/conf.csv"));
        String line, fullData="";
        int toAdd = 0, currentLine = 0, prevSpace = 0;
        String[] lineToReturn = new String[6];

        while((line=input.readLine()) != null) {

            String[] separatedLine = line.split(",");

            if (separatedLine != null) {

                if (currentLine == lineNo) {

                    String[] split = line.split(",");
                    System.arraycopy(split,0,lineToReturn,0,split.length);
                    toAdd += prevSpace - Integer.parseInt(split[5]);
                    prevSpace = Integer.parseInt(split[5]);

                    System.out.println("Delete.");
                    deleteChunk(split[2].trim(), split[1].trim(), currentDir.trim());

                    line = "";

                } else {

                    String[] split = line.split(",");

                    if (!split[5].equals("currentSpace") && !split[5].equals("")){//first two lines
                        split[5] = String.valueOf(Integer.parseInt(split[5]) + toAdd);
                        System.out.println("total size: " + split[5]);
                        prevSpace = Integer.parseInt(split[5]);
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


        return lineToReturn;
    }

    private static int getChunkWithHighDeg(String currentDir) throws IOException {

        BufferedReader input = new BufferedReader(new FileReader(currentDir + "/conf.csv"));
        String line;
        int maxDeg = -9999;
        int lineNo = 0;
        int lineMax = 0;

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

        return lineMax;
    }

    public static boolean checkRepDegree(String currentDir, String fileIdRemoved, String chunkNoRemoved)
            throws IOException {
        BufferedReader input = new BufferedReader(new FileReader(currentDir + "/conf.csv"));
        String line, fullData="";
        String lineToUpdate = null;
        boolean value=false;
        String expectedChunk = chunkNoRemoved.trim();

        while((line=input.readLine()) != null) {
            String[] separatedLine = line.split(",");
            if(separatedLine != null) {
                if(separatedLine[1].equals(fileIdRemoved)
                        && expectedChunk.equals(separatedLine[2])) {
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
}
