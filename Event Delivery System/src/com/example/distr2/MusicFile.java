package com.example.distr2;

import java.io.Serializable;
import java.util.Arrays;

/** A music chunk */
public class MusicFile implements Serializable {

    private String song_name, artist_name, genre, album;
    private byte[] data,image;
    private int chunk_num,data_bytes;

    //constructor
    public MusicFile(String song_name, String artist_name, String genre, String album, byte[] image, byte[] data,int data_bytes, int chunk_num) {
        this.song_name =song_name;
        this.artist_name = artist_name;
        this.genre = genre;
        this.album = album;
        if(image!=null) this.image = Arrays.copyOfRange(image,0,image.length-1);
        else this.image = null;
        this.data_bytes = data_bytes;
        this.data = Arrays.copyOfRange(data,0,data.length-1);
        this.chunk_num = chunk_num;
    }

    /** GETTERS */
    public String getSong_name() { return song_name; }

    public byte[] getImage() { return image; }

    public String getGenre() { return genre; }

    public String getArtist_name() { return artist_name; }

    public String getAlbum() {  return album;  }

    public byte[] getData() { return data; }

    public int getChunk_num() { return this.chunk_num; }

    public int getData_bytes() {  return data_bytes; }

    /** SETTERS */
    public void setData_bytes(int data_bytes) {  this.data_bytes = data_bytes; }

    public void setImage(byte[] image) { this.image = image; }

    public void setSong_name(String song_name) { this.song_name = song_name; }

    public void setArtist_name(String artist_name) { this.artist_name = artist_name; }

    public void setData(byte[] data) { this.data = data; }

    public void setAlbum(String album) { this.album = album; }

    public void setGenre(String genre) { this.genre = genre; }

    public void setChunk_num(int chunk_num) { this.chunk_num = chunk_num; }

    @Override
    public String toString() {
        return "Chunk #"+chunk_num+" of song : "+song_name+","+artist_name+". Size of chunk = "+ data.length;
    }

}
