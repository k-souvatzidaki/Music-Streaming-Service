package com.example.distr2;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

//activity with the song list of an artist
public class ArtistSongList extends AppCompatActivity implements ListAdapter.OnClickListener {

    //the artist name
    TextView text_view;
    String artist_name;
    //the song list UI
    RecyclerView view;
    ListAdapter adapter;
    RecyclerView.LayoutManager manager;
    ArrayList<String> song_names;

    //broker address
    Address broker;
    //consumer_id
    String consumer_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_songlist);

        song_names = new ArrayList<String>();

        Intent i = getIntent();
        artist_name = i.getStringExtra("selected_artist");
        //set artist name text
        text_view = findViewById(R.id.artist_name);
        text_view.setText(artist_name);
        //prepare RecyclerView
        view = (RecyclerView)findViewById(R.id.songList);
        manager = new LinearLayoutManager(this);
        view.setLayoutManager(manager);

        if(artist_name.equals("My Playlist")) {
            //read the downloaded song files
            File directory = new File(getApplicationContext().getFilesDir() + "/downloaded_songs");
            File[] downloaded_songs = directory.listFiles();
            ArrayList<String> titles = new ArrayList<String>();
            if(downloaded_songs.length > 0) {
                for(File temp : downloaded_songs) {
                    song_names.add(temp.getName());
                }
                //how songs are previewed
                for(String s : song_names) {
                    titles.add(s.substring(0,s.indexOf('_'))+" - "+ s.substring(s.indexOf('_')+1,s.indexOf(".mp3")));
                }
            }

            adapter = new ListAdapter(titles,ArtistSongList.this);
            view.setAdapter(adapter);
        }
        else{
            //ask the right broker for the artist's song list
            consumer_id = i.getStringExtra("consumer_id");
            broker = (com.example.distr2.Address)i.getSerializableExtra("right_broker");

            new SongsRunner().execute();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }


    //list item click listener
    int position;
    @Override
    public void onItemClick(int pos) {
        position = pos;

        if(!artist_name.equals("My Playlist")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ArtistSongList.this);
            builder.setTitle("Do you want to download the song or just stream?");
            builder.setItems(new String[]{"Download & listen","Just listen"}, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    boolean download;
                    if(which == 0) download = true;
                    else download = false;
                    /// make player and play song!!!!
                    Intent intent = new Intent(ArtistSongList.this, PlayerActivity.class);
                    intent.putExtra("right_broker", broker);
                    //pass the artist name
                    intent.putExtra("selected_artist", artist_name);
                    //pass consumer id
                    intent.putExtra("consumer_id", consumer_id);
                    //pass song name
                    intent.putExtra("selected_song", song_names.get(position));
                    //pass download
                    intent.putExtra("download", download);
                    startActivityForResult(intent,0);
                }
            });
            builder.show();
        } else {
            Intent intent = new Intent(ArtistSongList.this, PlayerActivity.class);
            intent.putExtra("selected_artist", artist_name);
            intent.putExtra("selected_song", song_names.get(position));
            startActivityForResult(intent,0);
        }
    }

    //Getting the artist's song list from the right broker
    private class SongsRunner extends AsyncTask<String, String, Void> {
        ProgressDialog progressDialog;

        @Override
        protected Void doInBackground(String... params) {
            //getting the artist's songs list
            Socket socket = null;
            ObjectOutputStream out = null;
            ObjectInputStream in = null;
            String response;

            try {
                socket = new Socket(broker.getIp(), broker.getPort());
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                out.writeObject(consumer_id); out.flush();
                response = (String)in.readObject();

                if(response.equals("NOT_REGISTERED")) {
                    in.readObject();
                }
                //request the song list
                out.writeObject("ASK_SONG");
                out.writeObject(new Request(artist_name,"any")); out.flush();
                response = (String)in.readObject();
                if(response.equals("LIST")) {
                    ArrayList<String> songs = (ArrayList<String>)in.readObject();
                    for(String s : songs) song_names.add(s);
                }

                socket.close();
                in.close();
                out.close();

            } catch (ClassNotFoundException | IOException e ) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(ArtistSongList.this,
                    "Please wait...",
                    "Connecting to server...");
        }

        @Override
        protected void onPostExecute(Void result) {
            progressDialog.dismiss();
            //set the songs list content
            adapter = new ListAdapter(song_names,ArtistSongList.this);
            view.setAdapter(adapter);
        }
    }

}