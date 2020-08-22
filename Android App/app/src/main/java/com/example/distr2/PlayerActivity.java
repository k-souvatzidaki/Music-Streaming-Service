package com.example.distr2;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import com.mpatric.mp3agic.*;

//the player activity
public class PlayerActivity extends AppCompatActivity {

    //UI
    TextView text_view1, text_view2;
    ImageButton play;
    ImageView cover;
    //using 2 players for gapless audio
    MediaPlayer player = null, player2 = null;
    byte[] image;
    String artist_name; String song_name;
    boolean isPlaying,download; int next; int player_playing;
    ArrayList<File> temp_files;

    //broker address
    Address broker;
    //consumer_id
    String consumer_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player);

        Intent i = getIntent();
        artist_name = i.getStringExtra("selected_artist");
        song_name = i.getStringExtra("selected_song");
        //set artist name and song name texts
        text_view1 = findViewById(R.id.artist_title);
        text_view2 = findViewById(R.id.song_title);
        //play button
        play = findViewById(R.id.play);
        //album cover
        cover = findViewById(R.id.cover_image);

        //prepare offline player
        if(artist_name.equals("My Playlist")) {
            text_view1.setText(song_name.substring(0, song_name.indexOf('_')));
            text_view2.setText(song_name.substring(song_name.indexOf('_') + 1, song_name.indexOf(".mp3")));
            player = new MediaPlayer();
            try {
                String path = getApplicationContext().getFilesDir() + "/downloaded_songs/" + song_name;
                player.setDataSource(path);
                player.prepare();

                Mp3File mp3_file = new Mp3File(new File(path));
                ID3v2 id = mp3_file.getId3v2Tag();
                image = id.getAlbumImage();
                if(image!=null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
                    cover.setImageBitmap(bitmap);
                }

            } catch (IOException | UnsupportedTagException | InvalidDataException e) {
                e.printStackTrace();
            }
            //play-pause button listener - changes the UI
            play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isPlaying) {
                        player.pause();
                        play.setBackgroundResource(R.drawable.teal_play);
                        isPlaying = false;
                    } else {
                        player.start();
                        play.setBackgroundResource(R.drawable.teal_pause);
                        isPlaying = true;
                    }
                }
            });
            isPlaying = true;
            player.start();

        //start streaming song with 2 players if online
        }else {
            text_view1.setText(artist_name);
            text_view2.setText(song_name);

            consumer_id = i.getStringExtra("consumer_id");
            broker = (com.example.distr2.Address)i.getSerializableExtra("right_broker");
            download = i.getBooleanExtra("download",false);

            player = new MediaPlayer();
            player2 = new MediaPlayer();

            temp_files = new ArrayList<File>();

            //on completion listeners for the 2 players
            //when a player completes, the other one starts playing
            //and the next chunk is prepared (if there is more)
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    player_playing = 2;
                    next++;
                    if(next < temp_files.size()) {
                        try {
                            player.reset();
                            player.setDataSource(temp_files.get(next).getPath());
                            player.prepare();
                            player2.setNextMediaPlayer(player);
                        }catch(IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else  {
                        player.stop();
                    }
                }
            });
            player2.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    player_playing = 1;
                    next++;
                    if(next < temp_files.size()) {
                        try {
                            player2.reset();
                            player2.setDataSource(temp_files.get(next).getPath());
                            player2.prepare();
                            player.setNextMediaPlayer(player2);
                        }catch(IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        player2.stop();
                    }
                }
            });

            play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(isPlaying) {
                        if(player_playing == 1) player.pause();
                        if(player_playing == 2) player2.pause();
                        play.setBackgroundResource(R.drawable.teal_play);
                        isPlaying = false;
                    }else {
                        if(player_playing == 1) player.start();
                        if(player_playing == 2) player2.start();
                        play.setBackgroundResource(R.drawable.teal_pause);
                        isPlaying = true;
                    }
                }
            });
            new StreamingRunner().execute();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    //if the activity is closed, stop the player
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(player!=null) {
            if(player.isPlaying()) player.stop();
        }
        if(player2!=null) {
            if(player2.isPlaying()) player2.stop();
        }
    }


    //stream and download song chunks
    private class StreamingRunner extends AsyncTask<String, String, Void> {
        FileOutputStream fos = null;

        @Override
        protected Void doInBackground(String... params) {
            //getting the song chunks
            Socket socket;
            ObjectOutputStream out;
            ObjectInputStream in;
            String response;

            try {
                //prepare file to download song
                if(download) {
                    File directory = new File(getApplicationContext().getFilesDir() + "/downloaded_songs");
                    if(!directory.exists()) directory.mkdir();
                    File file = new File(directory, artist_name+"_"+song_name+".mp3");
                    fos = new FileOutputStream(file);
                }

                socket = new Socket(broker.getIp(), broker.getPort());
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                out.writeObject(consumer_id); out.flush();
                response = (String)in.readObject();
                //request the song list
                out.writeObject("ASK_SONG");
                out.writeObject(new Request(artist_name,song_name)); out.flush();
                response = (String)in.readObject();

                if(response.equals("SONG")) {
                   MusicFile chunk;
                    while(true) {
                        response = (String)in.readObject();
                        if(response.equals("DONE")) break;
                        chunk = (MusicFile)in.readObject();

                        //save chunk to temp file in cache
                        File outputFile = File.createTempFile("chunk_"+chunk.getChunk_num(), ".mp3");
                        temp_files.add(outputFile);
                        FileOutputStream stream = new FileOutputStream(outputFile);
                        stream.write(chunk.getData(),0,chunk.getData_bytes()-1);
                        stream.close();
                        //and download it if asked
                        if(download) {
                            fos.write(chunk.getData(),0,chunk.getData_bytes()-1);
                        }

                        //set the cover image
                        if(chunk.getChunk_num() == 1) {
                           image = chunk.getImage();
                           publishProgress();
                        }

                        //start playing the first chunk
                        if(chunk.getChunk_num() == 2) {
                            player_playing = 1;
                            isPlaying = true;
                            player.setDataSource(temp_files.get(0).getPath());
                            player.prepare();
                            player.start();
                            //and prepare the second chunk
                            next = 1;
                            player2.setDataSource(temp_files.get(1).getPath());
                            player2.prepare();
                            player.setNextMediaPlayer(player2);
                        }
                        out.writeObject("RECEIVED");
                    }
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
        protected void onProgressUpdate(String...values) {
            if(image!=null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
                cover.setImageBitmap(bitmap);
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            if(download) {
                //close stream
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Toast.makeText(PlayerActivity.this, "Download completed!", Toast.LENGTH_LONG).show();
            }
        }
    }

}
