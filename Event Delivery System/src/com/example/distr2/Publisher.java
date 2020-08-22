package com.example.distr2;

import java.net.*;
import java.io.*;
import java.util.*;
import com.mpatric.mp3agic.*;

/** A Publisher that stores song data
 * and sends them to the Consumer via the Brokers */
public class Publisher extends Node {

    Address address;
    String mp3_dir;
    ServerSocket server;

    //hash map with artist names as keys and mp3 files as values
    HashMap<String, ArrayList<File>> songs;
    HashMap<String, ArrayList<String>> song_names;

    //constructor
    public Publisher(String ip,int port, String mp3_dir) throws IOException{
        this.address = new Address(ip,port);
        this.mp3_dir = mp3_dir;
        this.init();
    }

    @Override
    void init() throws IOException {
        //reading all files of the directory
        File[] files = new File(this.mp3_dir).listFiles();
        //creating the data hashmaps (song files and titles) and sending them to the brokers
        createHashMap(files);
        sendData();
        connect();
    }


    @Override
    void connect() throws IOException {
        System.out.println("Opening server socket and waiting for requests. . .");
        server = new ServerSocket(address.getPort(),10);
        Socket broker;
        while(true) {
            broker = server.accept();

            //create new thread for this client (broker) request
            new Thread(new Runnable() {
                Socket broker;
                String artist,song;
                public Runnable init(Socket broker) {
                    this.broker = broker;
                    return this;
                }

                @Override
                public void run() {
                    Request artist_song;
                    ObjectInputStream in; ObjectOutputStream out;
                    try {
                        in = new ObjectInputStream(broker.getInputStream());
                        out = new ObjectOutputStream(broker.getOutputStream());
                        artist_song = (Request) in.readObject(); //get the broker request
                        artist = artist_song.getArtist();
                        song = artist_song.getSong_name();

                        /** if the broker asked for an artist, return the song list */
                        if (song.equals("any")) {
                            //send response and the list
                            out.writeObject("LIST"); out.flush();
                            out.writeObject(song_names.get(artist)); out.flush();
                            return;
                        }

                        /** else, the broker asked for a specific song */
                        BufferedInputStream buf; File mp3 = null;
                        for (File f : songs.get(artist)) {
                            if (f.getName().substring(0, f.getName().length() - 4).equals(song)) {
                                 mp3 = f;
                                 break; //found the right mp3 file
                            }
                        }
                        /** if the song doesn't exist */
                        if (mp3 == null) {
                            //send response
                            out.writeObject("NO_SONG"); out.flush();
                            //the song doesn't exist. return the list
                            out.writeObject(song_names.get(artist)); out.flush();
                            return;

                        } else {
                            /** the song exists */
                            //send response
                            out.writeObject("SONG"); out.flush();
                            //chunk song
                            ID3v2 id = null;
                            Mp3File mp3_file = new Mp3File(mp3); id = mp3_file.getId3v2Tag();
                            int chunk_num = 1;
                            int sizeOfChunk = 1024 * 512;// 0.5MB = 512KB
                            byte[] buffer = new byte[sizeOfChunk];
                            MusicFile chunk;

                            String response;
                            //read sizeofchunk bytes and write them to the buffer
                            FileInputStream fis = new FileInputStream(mp3);
                            //chunk the mp3 file and push the chunks
                            int m;
                            for (int i = 0; i < mp3.length(); i += sizeOfChunk) {
                                m = fis.read(buffer);
                                chunk = new MusicFile(song, artist, id.getGenreDescription(), id.getAlbum(), id.getAlbumImage(), buffer,m, chunk_num);
                                push(chunk,out); //PUSH
                                response = (String)in.readObject();
                                System.out.println("Sent chunk #"+chunk_num);
                                chunk_num++;
                                if(response.equals("RECEIVED")) continue;
                            }
                            //when the chunks are sent, notify the broker
                            out.writeObject("DONE"); out.flush();
                        }

                    } catch (IOException | ClassNotFoundException | InvalidDataException | UnsupportedTagException e) {
                        e.printStackTrace();
                    }
                }
            }.init(broker)).start();
        }
    }


    @Override
    void disconnect() throws IOException{
        if(!server.isClosed()) server.close();
    }

    /** Pushing a chunk to the broker that requested it */
    void push(MusicFile chunk, ObjectOutputStream out) throws IOException {
        out.writeObject("SENDING_CHUNK"); out.flush();
        out.writeObject(chunk); out.flush();
    }


    /** Creating a hash map with all the songs for this Publisher */
    void createHashMap(File[] files) {
        this.songs = new HashMap<String,ArrayList<File>>();
        this.song_names = new HashMap<String,ArrayList<String>>();
        String artist = null; Mp3File mp3;

        try {
            for(File f : files) {
                mp3 = new Mp3File(f); ID3v2 id = mp3.getId3v2Tag();
                //read the artist name
                try{
                    artist= id.getArtist();
                }catch (NullPointerException e) {
                    artist = "Unknown";
                }
                if(artist=="" || artist==null) artist = "Unknown";
                //read the song name
                String temp = f.getName().substring(0,f.getName().length()-4);
                //add the song to the hash map
                if(songs.containsKey(artist)) {
                    songs.get(artist).add(f);
                    song_names.get(artist).add(temp);
                }else{
                    songs.put(artist, new ArrayList<File>(Arrays.asList(f)));
                    song_names.put(artist,new ArrayList<String>(Arrays.asList(temp)));
                }
            }
        }catch( IOException | UnsupportedTagException | InvalidDataException e ) { e.printStackTrace(); }

    }

    /** send list of artists to ALL BROKERS */
    void sendData() {
        Socket broker; ObjectOutputStream out; ObjectInputStream in;
        //create a temp ArrayList with the artists
        ArrayList<String> artists = new ArrayList<String>();
        for(String artist : songs.keySet()) artists.add(artist);

        System.out.println("Publisher with IP "+address.getIp()+" and PORT# "+address.getPort()+" sending list with artists to all Brokers. . .");
        try {
            for(Address a : Node.brokers_publisher_ports ){
                broker = new Socket(a.getIp(), a.getPort());
                out = new ObjectOutputStream(broker.getOutputStream());
                in = new ObjectInputStream(broker.getInputStream());

                Request data = new Request(this.address,artists);
                out.writeObject(data); out.flush();
                int response = in.readInt();
                if(response == 1) broker.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
