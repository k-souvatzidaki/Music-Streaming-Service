package com.example.distr2;

import java.math.BigInteger;
import java.security.*;
import java.util.*;
import java.net.*;
import java.io.*;

/** A Broker that accepts song requests from the Consumer
 * and sends requests to the Publishers to get song data */
public class Broker extends Node {

    //2 types of requests to the Zookeeper
    private static final int BROKER_LIST = 1; //request for an updated, synchronised version of each Broker's artists list
    private static final int SEND_BROKER_LIST = 2; //sending a list with artists for THIS broker

    ArrayList<String> registeredUsers; //registered Consumers
    HashMap<Address, ArrayList<String>> publisher_artists; //list with the artists each publisher supports
    ArrayList<String> broker_artists; //list with the artists this broker supports
    Thread pubs, consumers;

    Address address; //to accept Consumer requests
    int publisher_port; //to accept Publisher requests

    //constructor
    public Broker(String ip,int consumer_port, int publisher_port) {
        this.address = new Address(ip,consumer_port);
        this.publisher_port = publisher_port;
        this.registeredUsers = new ArrayList<String>();
        publisher_artists = new HashMap<Address,ArrayList<String>>();
        broker_artists = new ArrayList<String>();
        init();
    }

    @Override
    void init() {
        connect();
    }

    @Override
    void connect() {
        /**CREATING THREADS TO HAVE 2 SERVER SOCKETS UP AND RUNNING.*/

        /** PUBLISHERS */
        //create a socket to accept PUBLISHER requests (for artist lists)
        pubs = new Thread(new Runnable() {
            @Override
            public void run() {
                Socket publisher; ServerSocket publisher_requests;
                try {
                    publisher_requests = new ServerSocket(publisher_port,20);
                    System.out.println("start accepting PUBLISHER requests (up and running server)");
                    while(true) {
                        //accept a list from a publisher
                        publisher = publisher_requests.accept();
                        updatePublishers(publisher);
                        updateBrokerArtists();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        pubs.start();

        /** CONSUMERS */
        //create a socket to accept CONSUMER requests
        consumers = new Thread(new Runnable() {
            @Override
            public void run() {
                Socket consumer; ServerSocket consumer_requests;
                System.out.println("start accepting CONSUMER requests (up and running server)");
                try {
                    consumer_requests = new ServerSocket(address.getPort(), 10);
                    while(true) {
                        //accept a consumer request and run a new thread for it
                        consumer = consumer_requests.accept();

                        new Thread(new Runnable() {
                            Socket consumer;
                            public Runnable init(Socket consumer) {
                                this.consumer = consumer;
                                return this;
                            }

                            @Override
                            public void run() {
                                ObjectInputStream consumer_in,publisher_in; ObjectOutputStream consumer_out,publisher_out;
                                Socket publisher;
                                String response;
                                try{
                                    consumer_in = new ObjectInputStream(consumer.getInputStream());
                                    consumer_out = new ObjectOutputStream(consumer.getOutputStream());

                                    response = (String)consumer_in.readObject();
                                    /** if the Consumer wants to be unregistered*/
                                    if(response.equals("UNREGISTER")) {
                                        response = (String)consumer_in.readObject();
                                        registeredUsers.remove(response);
                                        consumer_out.writeObject("OK"); consumer_out.flush();
                                    }
                                    /** else if the Consumer wants to request a song */
                                    else {
                                        //check if the Consumer is registered and notify them
                                        if(registeredUsers.contains(response /*the consumer id*/)) {
                                            consumer_out.writeObject("REGISTERED"); consumer_out.flush();
                                        } else {
                                            consumer_out.writeObject("NOT_REGISTERED"); consumer_out.flush();
                                            registeredUsers.add(response); //register the Consumer

                                            System.out.println("Sending brokers artists hashmap to consumer #"+response);
                                            HashMap<Address,ArrayList<String>> artists = getBrokersList();
                                            consumer_out.writeObject(artists); consumer_out.flush(); //send the brokers list
                                        }
                                        response = (String)consumer_in.readObject();
                                        //if the Consumer wants to be disconnected
                                        if(response.equals("DISCONNECT")) {
                                            System.out.println("Disconnecting consumer. . .");
                                            consumer_out.writeObject("OK"); consumer_out.flush();
                                            System.out.println("Disconnected.");
                                            return;
                                        } else {
                                            Request artist_song = (Request) consumer_in.readObject();
                                            System.out.println("Got song request "+artist_song.getArtist() + "," + artist_song.getSong_name());
                                            //finding the right publisher
                                            Address pub = null;
                                            for(Address a : publisher_artists.keySet()) {
                                                if(publisher_artists.get(a).contains(artist_song.getArtist())) pub = a;
                                            }
                                            //requesting the song to the publisher
                                            publisher = new Socket(pub.getIp(), pub.getPort());
                                            publisher_out = new ObjectOutputStream(publisher.getOutputStream());
                                            publisher_in = new ObjectInputStream(publisher.getInputStream());
                                            publisher_out.writeObject(artist_song); publisher_out.flush();
                                            //getting response
                                            response = (String)publisher_in.readObject();
                                            consumer_out.writeObject(response); consumer_out.flush();

                                            /** if the requested song doesn't exist, or the Consumer requested an artist*/
                                            if(response.equals("LIST") || response.equals("NO_SONG")) {
                                                ArrayList<String> artist_songs = (ArrayList<String>)publisher_in.readObject();
                                                //sending the list to the Consumer
                                                consumer_out.writeObject(artist_songs); consumer_out.flush();
                                                System.out.println("Sent songs list");
                                            /** if the song exists, receiving chunks and pulling them to the Consumer*/
                                            }else {
                                                ArrayList<MusicFile> chunks = new ArrayList<MusicFile>(); //list of chunks
                                                MusicFile chunk;
                                                //getting chunks while there are more
                                                while(true) {
                                                    response = (String)publisher_in.readObject();
                                                    if(response.equals("DONE")) {
                                                        consumer_out.writeObject("DONE"); consumer_out.flush();
                                                        break;
                                                    }
                                                    chunk = (MusicFile)publisher_in.readObject();
                                                    chunks.add(chunk);
                                                    publisher_out.writeObject("RECEIVED");
                                                    pull(chunk,consumer_out);
                                                    response = (String)consumer_in.readObject();
                                                    if(response.equals("RECEIVED")) continue;

                                                }
                                            }
                                        }
                                    }
                                } catch (IOException | ClassNotFoundException e) {
                                    e.printStackTrace();
                                }
                            }
                        }.init(consumer)).start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        consumers.start();

    }

    void pull(MusicFile chunk,ObjectOutputStream out) throws IOException {

        System.out.println("Pushing chunk to Consumer");
        out.writeObject("SENDING_CHUNK"); out.flush();
        out.writeObject(chunk); out.flush();
        System.out.println("Sent");

    }


    @Override
    void disconnect() {
        pubs.interrupt();
        consumers.interrupt();
    }


    /**update publishers list and send data to zookeeper */
    void updatePublishers(Socket publisher) {
        ObjectInputStream publisher_in;
        ObjectOutputStream publisher_out;
        try {
            publisher_in = new ObjectInputStream(publisher.getInputStream());
            publisher_out = new ObjectOutputStream(publisher.getOutputStream());
            //read list of publisher's artists
            Request data = (Request)publisher_in.readObject();
            publisher_out.writeInt(1); publisher_out.flush(); //send response

            Address address = data.getAddress();
            if(publisher_artists.containsKey(address)) {
                publisher_artists.replace(data.getAddress(),data.getSongs());
            } else {
                publisher_artists.put(data.getAddress(),data.getSongs());
            }
            publisher.close();

        } catch(IOException | ClassNotFoundException e) { e.printStackTrace(); }
    }

    /** update the artists list for this broker and send data to zookeeper */
    void updateBrokerArtists() {
        //current broker hash
        String ip_port = this.address.getIp().concat(Integer.toString(this.address.getPort()));
        BigInteger currBroker = hash(ip_port);
        //getting all brokers hashes
        ArrayList<BigInteger> broker_hashes = new ArrayList<BigInteger>();
        for (Address broker : Node.brokers_consumer_ports) {
            String hash_key = broker.getIp().concat(Integer.toString(broker.getPort()));
            BigInteger hash1 = hash(hash_key);
            broker_hashes.add(hash1);
        }
        //sort hashes
        Collections.sort(broker_hashes);
        //find max hash
        BigInteger maxHash = broker_hashes.get(broker_hashes.size()-1);

        for (Address key : publisher_artists.keySet()) {
            for (String name: publisher_artists.get(key)) { //for each artist
                //get artist name hash and mod
                BigInteger nameHash = hash(name);
                BigInteger mod = nameHash.mod(maxHash);
                //find the broker hash that matches
                for(BigInteger broker_hash : broker_hashes) {
                    if (mod.compareTo(broker_hash) < 0) {
                        if (broker_hash.equals(currBroker)) {
                            if (!broker_artists.contains(name)) broker_artists.add(name);
                        }
                        break;
                    }
                }
            }
        }

        //send broker list to zookeeper
        try {
            Socket zook2 = new Socket(Node.zookeeper.getIp(), Node.zookeeper.getPort());
            ObjectOutputStream zook_out2 = new ObjectOutputStream(zook2.getOutputStream());
            zook_out2.writeInt(SEND_BROKER_LIST); zook_out2.flush();
            Request artists = new Request(this.address,this.broker_artists);
            zook_out2.writeObject(artists); zook_out2.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**ask the Zookeeper for the Broker's list */
    HashMap<Address, ArrayList<String>> getBrokersList() {
        HashMap<Address, ArrayList<String>> brokers = null;
        Socket zookeeper; ObjectOutputStream zookeeper_out; ObjectInputStream zookeeper_in;
        try {
            zookeeper = new Socket(Node.zookeeper.getIp(),Node.zookeeper.getPort());
            zookeeper_out = new ObjectOutputStream(zookeeper.getOutputStream());
            zookeeper_in = new ObjectInputStream(zookeeper.getInputStream());
            zookeeper_out.writeInt(BROKER_LIST); //request number
            zookeeper_out.flush(); //send
            brokers = (HashMap<Address, ArrayList<String>>) zookeeper_in.readObject(); //get the HashMap
            zookeeper.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return brokers;
    }

    /**Hash function (using SHA-1) */
    public BigInteger hash(String value){
        BigInteger hashnum = null;
        try {
            MessageDigest message = MessageDigest.getInstance("SHA-1");
            byte[] messageDigest = message.digest(value.getBytes());

            // convert byte[] to positive integer
            hashnum = new BigInteger(1, messageDigest);
        }
        catch(NoSuchAlgorithmException e){
            e.printStackTrace();
        }
        return hashnum;
    }

}