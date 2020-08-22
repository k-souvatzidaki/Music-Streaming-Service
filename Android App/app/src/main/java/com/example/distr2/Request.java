package com.example.distr2;

import java.io.Serializable;
import java.util.*;

/** A simple request */
public class Request implements Serializable {

    String artist,song_name;
    Address address;
    ArrayList<String> songs;

    public Request(String artist, String song_name) {
        this.artist = artist;
        this.song_name = song_name;
    }

    public Request(Address address, ArrayList<String> songs) {
        this.address = address;
        this.songs = songs;
    }

    /** GETTERS */
    public String getArtist() { return artist; }

    public String getSong_name() { return song_name; }

    public Address getAddress() { return address; }

    public ArrayList<String> getSongs() { return songs; }

    /** SETTERS */

    public void setArtist(String artist) { this.artist = artist; }

    public void setSong_name(String song_name) { this.song_name = song_name; }

    public void setAddress(Address address) { this.address = address; }

    public void setSongs(ArrayList<String> songs) { this.songs = songs; }

}