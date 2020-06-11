package com.josselin.mediaplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements listAdapter.customButtonListener {

    private ListView listView;
    ListAdapter adapter;
    ArrayList<String> dataItems = new ArrayList<String>();
    ArrayList<String> urlItems = new ArrayList<String>();
    MediaPlayer player;
    SeekBar seekbar;
    String playing = "";
    Handler handler;
    Runnable runnable;
    Button pause, forward, rewind;
    public static final int MY_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new Handler();

        //Permission
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.INTERNET
                },
                100);

        //Activity Main
        pause = findViewById(R.id.pauseM);
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pausePlayer();
            }
        });
        forward = findViewById(R.id.forward);
        forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                player.seekTo(player.getCurrentPosition()+5000);
            }
        });
        rewind = findViewById(R.id.rewind);
        rewind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                player.seekTo(player.getCurrentPosition()-5000);
            }
        });

        seekbar = findViewById(R.id.seekbar);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                if (b) {
                    player.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        listView = findViewById(R.id.listView);

        String[] dataArray = getResources().getStringArray(R.array.music);
        List<String> dataTemp = Arrays.asList(dataArray);
        dataItems.addAll(dataTemp);

        String[] urlArray = getResources().getStringArray(R.array.download);
        List<String> urlTemp = Arrays.asList(urlArray);
        urlItems.addAll(urlTemp);

        adapter = new listAdapter(MainActivity.this, dataItems, urlItems);
        ((listAdapter) adapter).setCustomButtonListener(MainActivity.this);
        listView.setAdapter(adapter);
    }

    @Override
    public void onButtonClickListner(int position, String value, String button, String url) {
        if (button == "play") {
            playPlayer(value);
        }
        if (button == "download") {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSION);
            }
                File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+
                        "/MediaPlayer/");
                try {
                    dir.mkdir();
                }catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Cannot create folder !", Toast.LENGTH_SHORT).show();
                }
                File check = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+
                        "/MediaPlayer/" + value + ".mp3");
                if (!check.exists()) {
                    new DownloadTask().execute(url);
                }else{
                    Toast.makeText(this, "File already exist !", Toast.LENGTH_SHORT).show();
                }

        }

    }

    //Permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                }else {
                    Toast.makeText(this, "Permession Denied !", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    //MediaPlayer
    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.start();
            playCycle();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            if (player.isPlaying()) {
                player.pause();
                pause.setBackground(this.getResources().getDrawable(R.drawable.ic_play_arrow_black_24dp));
            } else{
                player.start();
                pause.setBackground(this.getResources().getDrawable(R.drawable.ic_pause_black_24dp));
            }
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
            handler.removeCallbacks(runnable);
            seekbar.setProgress(0);
        }
    }
    public void playPlayer(String id) {
        try{
            if (player == null) {
                player = new MediaPlayer();
                player.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath()+
                        "/MediaPlayer/" + id + ".mp3");
                player.prepare();
                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        onStop();
                    }
                });
                playing = id;
                seekbar.setMax(player.getDuration());

            }else{
                if (id != playing) {
                    onStop();
                    playPlayer(id);
                }
            }
            player.start();
            playCycle();
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this, "A Telecharger", Toast.LENGTH_SHORT).show();
        }

    }
    public void pausePlayer() {
        onPause();
    }
    public void playCycle() {
        seekbar.setProgress(player.getCurrentPosition());

        if (player.isPlaying()) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    playCycle();
                }
            };
            handler.postDelayed(runnable, 1000);
        }
    }

    //Download
    class DownloadTask extends AsyncTask<String,Integer,String>
    {
        String name;
        double file_size;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {

            String path = params[0];
            name = params[0].substring(params[0].lastIndexOf("/") + 1);

            try {
                InputStream is = null;
                OutputStream os = null;
                HttpURLConnection connection = null;

                try {
                    URL url = new URL(path);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        return "Server return HTTP " + connection.getResponseCode() + " "
                                + connection.getResponseMessage();
                    }

                    int intputLength = connection.getContentLength();
                    file_size = intputLength;

                    is = connection.getInputStream();
                    File song = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+
                            "/MediaPlayer/" + name);
                    try {
                        song.createNewFile();
                        os = new FileOutputStream(song);
                    }catch (Exception e) {
                        e.printStackTrace();
                    }


                    byte[] data = new byte[4096];
                    int count; int total = 0;

                    while ((count = is.read(data)) != -1) {
                        if (isCancelled()) {
                            return null;
                        }
                        total += count;
                        if (intputLength > 0) {
                            publishProgress((int)total + 100 / intputLength);
                        }

                        os.write(data, 0, count);
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    try {
                        if (os != null) {
                            os.close();
                        }
                        if (is != null) {
                            is.close();
                        }
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (connection != null) {
                        connection.disconnect();;
                    }
                }
            }finally {
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            //super.onProgressUpdate(values);
            /*progressDialog.setIndeterminate(false);
            progressDialog.setMax(100);
            progressDialog.setProgress(values[0]);
            progressDialog.setMessage("File size : " + new DecimalFormat("##.##").format(file_size / 1000000) + "MB");*/
        }

        @Override
        protected void onPostExecute(String res) {
            super.onPostExecute(res);
            if (res != null) {
                Toast.makeText(getApplicationContext(), "Error: "+res, Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(getApplicationContext(), "Downloaded", Toast.LENGTH_SHORT).show();
            }
        }
    }

}


