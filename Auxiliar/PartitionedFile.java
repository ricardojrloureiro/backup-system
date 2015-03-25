package Auxiliar;

import java.io.*;
import java.util.ArrayList;

public class PartitionedFile {
    private ArrayList<Chunk> chunks = new ArrayList<>();

    public PartitionedFile(String filename) {
        File f = new File(filename);
        int partCounter = 0;
        int sizeOfFiles = 64000;
        byte[] buffer = new byte[sizeOfFiles];

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f))) {
            int tmp = 0;
            while ((tmp = bis.read(buffer)) > 0) {
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    out.write(buffer, 0, tmp);
                    Chunk tempChunk = new Chunk(partCounter,out.toByteArray());
                    this.chunks.add(tempChunk);
                    partCounter++;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Chunk> getChunks() {
        return this.chunks;
    }
}
