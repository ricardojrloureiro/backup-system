package Auxiliar;


public class Chunk {
    private int chunkNumber;
    private byte[] body;

    public Chunk(int id, byte[] body) {
        this.chunkNumber = id;
        this.body = body;
    }

    public int getChunkNumber(){
        return chunkNumber;
    }

    public int getChunkSize() {
        return body.length;
    }

    public byte[] getBody() {
        return body;
    }
}
