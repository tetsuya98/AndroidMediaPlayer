package com.josselin.mediaplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.view.View;
import android.widget.Toast;


import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ListView lv;
    Button play;
    Button pause;
    Button stop;
    MediaPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        play = (Button)findViewById(R.id.play);
        pause = (Button)findViewById(R.id.pause);
        stop = (Button)findViewById(R.id.stop);

        /*lv = (ListView)findViewById(R.id.listView);

        ArrayList<String> al = new ArrayList<>();
        al.add("test");
        al.add("test2");
        al.add("test3");
        al.add("test");
        al.add("test2");
        al.add("test3");
        al.add("test");
        al.add("test2");
        al.add("test3");
        al.add("test");
        al.add("test2");
        al.add("test3");
        al.add("test");
        al.add("test2");
        al.add("test3");
        al.add("test");
        al.add("test2");
        al.add("test3");
        ArrayAdapter aa = new ArrayAdapter(this, android.R.layout.simple_list_item_1,al);

        lv.setAdapter(aa);*/
    }

    public void play(View view) {
        if (player == null) {
            player = MediaPlayer.create(this, R.raw.eden);
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    stopPlayer();
                }
            });
        }
        player.start();
    }
    public void pause(View view) {
        if (player != null) {
            player.pause();
        }
    }
    public void stop(View view) {
        stopPlayer();
    }

    public void stopPlayer() {
        if (player != null) {
            player.release();
            player = null;
            Toast.makeText(this, "Music Stopped", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopPlayer();
    }
}
