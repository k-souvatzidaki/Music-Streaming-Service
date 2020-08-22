package com.example.distr2;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.*;
import java.net.*;
import java.io.*;

//the main activity of the app - a list with all the artists
public class MainActivity extends AppCompatActivity implements ListAdapter.OnClickListener {

    //the artist list UI
    RecyclerView view;
    ListAdapter adapter;
    RecyclerView.LayoutManager manager;
    ArrayList<String> artists;

    //first random broker
    Address broker;
    //consumer id
    String consumer_id;
    //the artists of each broker
    HashMap<Address, ArrayList<String>> brokers_artists;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        artists = new ArrayList<String>();
        artists.add("My Playlist"); //add the offline playlist on the list

        //prepare RecyclerView
        view = findViewById(R.id.artistList);
        manager = new LinearLayoutManager(this);
        view.setLayoutManager(manager);

        //check if online
        boolean online = false;
        ConnectivityManager m = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo n = m.getActiveNetworkInfo();
        if (n != null && n.isConnected()) {
            online = true;
        }

        if(online) {
            //create a consumer id
            Random r = new Random();
            consumer_id = Integer.toString(r.nextInt(Integer.MAX_VALUE));
            //ask artists list from a random broker
            new ArtistsRunner().execute();
        }
        else {
            //view the list with just the "My Playlist" option
            adapter = new ListAdapter(artists,MainActivity.this);
            view.setAdapter(adapter);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }


    //list item click listener
    @Override
    public void onItemClick(int pos) {
        //create new activity
        Intent intent = new Intent(this, ArtistSongList.class);

        if(!artists.get(pos).equals("My Playlist")) {
            //find and pass right broker
            Address broker = null;
            for(Address a : brokers_artists.keySet()) {
                if(brokers_artists.get(a).contains(artists.get(pos))) {
                    broker = a; break;
                }
            }
            intent.putExtra("right_broker", broker);
            //pass consumer id
            intent.putExtra("consumer_id", consumer_id);
        }
        //pass the artist name
        intent.putExtra("selected_artist", artists.get(pos));

        startActivityForResult(intent,0);
    }


    //getting the artists list from a random broker
    private class ArtistsRunner extends AsyncTask<String, String, Void> {
        ProgressDialog progressDialog;

        @Override
        protected Void doInBackground(String... params) {
            Socket socket = null;
            ObjectOutputStream out = null;
            ObjectInputStream in = null;
            String response;

            Random rand = new Random();
            broker = Node.brokers_consumer_ports.get(rand.nextInt(Node.brokers_consumer_ports.size())); //first connection with a random broker

            try {
                //create my playlist directory if it doesn't exist
                File directory = new File(getApplicationContext().getFilesDir() + "/downloaded_songs");
                if(!directory.exists()) directory.mkdir();

                socket = new Socket(broker.getIp(), broker.getPort());
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                out.writeObject(consumer_id); out.flush();
                response = (String)in.readObject();

                // if not registered, receive a brokers' artists list
                if(response.equals("NOT_REGISTERED")) {
                    brokers_artists = (HashMap<Address, ArrayList<String>>) in.readObject();
                    //add the artists on the artists arraylist
                    for(Address a : brokers_artists.keySet()) {
                        for(String artist : brokers_artists.get(a)) {
                            if(artist.length() > 1) artists.add(artist);
                        }
                    }
                }

                out.writeObject("DISCONNECT"); out.flush();
                response = (String)in.readObject();
                if(response.equals("OK")) {
                    socket.close();
                    in.close();
                    out.close();
                }
            } catch (ClassNotFoundException | IOException e ) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(MainActivity.this,
                    "Please wait...",
                    "Connecting to server...");
        }

        @Override
        protected void onPostExecute(Void result) {
            progressDialog.dismiss();
            //set the artists list content
            adapter = new ListAdapter(artists,MainActivity.this);
            view.setAdapter(adapter);
        }
    }

}