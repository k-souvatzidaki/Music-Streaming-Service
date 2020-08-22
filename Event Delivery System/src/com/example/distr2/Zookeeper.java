package com.example.distr2;

import java.io.*;
import java.net.*;
import java.util.*;

/** Storing a synchronised list with brokers' artists
 * All brokers speak to the zookeeper to get the synchronised data */
public class Zookeeper extends Node {

    //list with the Brokers' IP and PORT#s , and the artists each one supports
    HashMap<Address, ArrayList<String>> brokers_artists;

    ServerSocket broker_requests;
    //constructor
    public Zookeeper() {
        brokers_artists = new HashMap<Address, ArrayList<String>>();
        init();
    }

    @Override
    void init() { connect(); }

    @Override
    void connect() {
        //create a Socket to accept broker requests
        try {
            broker_requests = new ServerSocket(Node.zookeeper.getPort(),10);
            Socket broker;
            while(true) {
                broker = broker_requests.accept();
                //new thread for each broker request
                new Thread(new Runnable() {
                    Socket broker;
                    public Runnable init(Socket broker) {
                        this.broker = broker;
                        return this;
                    }

                    @Override
                    public void run() {
                        ObjectInputStream in; ObjectOutputStream out;
                        try {
                            in = new ObjectInputStream(broker.getInputStream());
                            out = new ObjectOutputStream(broker.getOutputStream());
                            int request_type = in.readInt();
                            if(request_type == 1) {
                                out.writeObject(getBrokers_artists()); out.flush();
                            } else if (request_type == 2) {
                                Request artists = (Request)in.readObject();
                                updateBrokers(artists);
                            }
                        } catch (IOException | ClassNotFoundException ex) {
                            ex.printStackTrace();
                        }
                    }
                }.init(broker)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    void disconnect() throws IOException {
        if(!broker_requests.isClosed()) broker_requests.close();
    }

    /** updating the brokers list - synchronised */
    synchronized void updateBrokers(Request data) {
        System.out.println("Updating the synchronised Brokers list.");
        Address address = data.getAddress();
        if(brokers_artists.containsKey(address)) {
            brokers_artists.replace(address,data.getSongs());
        } else {
            brokers_artists.put(address,data.getSongs());
        }
    }

    /** getter (synchronised)*/
    synchronized HashMap<Address, ArrayList<String>> getBrokers_artists() { return this.brokers_artists; }

    //main
    public static void main(String [] args) {
        System.out.println("Starting the Zookeeper");
        new Zookeeper();
    }

}