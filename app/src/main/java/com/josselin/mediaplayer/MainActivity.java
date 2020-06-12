package com.josselin.mediaplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Layout;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
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

    private ListView listView; //Liste des musique
    ListAdapter adapter; //adapter de la listView
    ArrayList<String> dataItems = new ArrayList<String>(); //Liste des titres
    ArrayList<String> urlItems = new ArrayList<String>(); //Liste des URLs (download)
    MediaPlayer player; //Player pour musique
    SeekBar seekbar; //Progression de la musique
    String playing = ""; //Id de la musique en cour
    Handler handler;
    Runnable runnable;
    Button pause, forward, rewind; //Bouton pour le player
    public static final int MY_PERMISSION = 1; //Permission de l'application

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
        forward.setEnabled(false); //On n'avance pas quand il n'ya pas de musique
        forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                player.seekTo(player.getCurrentPosition()+5000); //On avance dans la musique
            }
        });
        rewind = findViewById(R.id.rewind);
        rewind.setEnabled(false); //On ne recule pas quand il n'ya pas de musique
        rewind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                player.seekTo(player.getCurrentPosition()-5000); //On recule dans la musique
            }
        });

        seekbar = findViewById(R.id.seekbar);
        seekbar.setEnabled(false); //On ne touche pas la seekbar si il n'y a pas de musique
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) { //Quand la musique est en cours
                if (b) {
                    player.seekTo(progress); //La seekbar avance
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

        //On récupère les titres pour la listView
        String[] dataArray = getResources().getStringArray(R.array.music);
        List<String> dataTemp = Arrays.asList(dataArray);
        dataItems.addAll(dataTemp);

        //On récupère les URLs pour le download
        String[] urlArray = getResources().getStringArray(R.array.download);
        List<String> urlTemp = Arrays.asList(urlArray);
        urlItems.addAll(urlTemp);

        //On crée l'adapter et donc la listView
        adapter = new listAdapter(MainActivity.this, dataItems, urlItems);
        ((listAdapter) adapter).setCustomButtonListener(MainActivity.this);
        listView.setAdapter(adapter);
    }

    @Override
    public void onButtonClickListner(int position, String value, String button, String url) {
        if (button == "play") {
            playPlayer(value); //On lance la musique
        }
        if (button == "download") { //On download la musique
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_DENIED) { //Si on n'a pas les permissions
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSION); //On les demande
            }
                File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+
                        "/MediaPlayer/"); //Dossier des musiques
                try {
                    dir.mkdir(); //Le créer si il n'existe pas
                }catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Cannot create folder !", Toast.LENGTH_SHORT).show();
                }
                File check = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+
                        "/MediaPlayer/" + value + ".mp3");//Musique à telecharger
                if (!check.exists()) { //Si elle n'existe pas déjà
                    new DownloadTask().execute(url); //On telecharge
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
            if (player.isPlaying()) {//SI la musique est en cours
                player.pause(); //On la mets en pause
                pause.setBackground(this.getResources().getDrawable(R.drawable.ic_play_arrow_black_24dp)); //Le bouton devient play
            } else{ //Sinon
                player.start(); //On relance la musique
                pause.setBackground(this.getResources().getDrawable(R.drawable.ic_pause_black_24dp)); //le bouton redevient pause
            }
        }
    }
    @Override
    protected void onStop() {
        super.onStop(); //A la fin de la musique
        if (player != null) {
            player.release(); //On libere le player
            player = null; //On le supprime
            handler.removeCallbacks(runnable); //On supprime le thread de la seekbar
            seekbar.setProgress(0); //On remets la seekbar à 0
            seekbar.setEnabled(false); //On desactive la seekbar
            rewind.setEnabled(false); forward.setEnabled(false); //On desactive les boutons
        }
    }
    public void playPlayer(String id) {
        try{
            if (player == null) { //Si il n'y a pas de musique
                player = new MediaPlayer(); //On crée le player
                player.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath()+
                        "/MediaPlayer/" + id + ".mp3"); //On lui donne la musique
                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        onStop(); //On arrête quand on arrive à la fin
                    }
                });
                playing = id; //On récupère l'id de la musique qu'on va jouer
                player.prepareAsync(); //On prepare le player
            }else{ //Si il y avait une musique
                if (id != playing) { //On regarde si c'est la même, si non
                    onStop(); //On l'arrête
                    playPlayer(id); //On lance la nouvelle
                }
            }
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() { //On attend que la player soit pret
                @Override
                public void onPrepared(MediaPlayer player) { //Quand il est pret
                    rewind.setEnabled(true); forward.setEnabled(true); //On active les boutons
                    seekbar.setEnabled(true); //On active la seekbar
                    seekbar.setMax(player.getDuration()); //On lui donne la durée de la musique
                    player.start(); //On lance la musique
                    playCycle(); //On lance la seekbar
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this, "A Telecharger", Toast.LENGTH_SHORT).show();
        }
    }
    public void pausePlayer() {
        onPause();
    }
    public void playCycle() {
        seekbar.setProgress(player.getCurrentPosition()); //On fait avancer la seekbar avec la musique
        if (player.isPlaying()) { //La musique joue encore
            runnable = new Runnable() {
                @Override
                public void run() {
                    playCycle();
                }
            }; //On relance de manière recursive
            handler.postDelayed(runnable, 1000); //On gere la vitesse d'avancement de la seekbar
        }
    }

    //Download
    class DownloadTask extends AsyncTask<String,Integer,String>
    {
        String name; //Nom de la musique
        double file_size; //Taille du fichier à telecharger

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {

            String path = params[0]; //On recupere l'URL de download
            name = params[0].substring(params[0].lastIndexOf("/") + 1); //On récupère le nom de la musique

            try {
                InputStream is = null; //On recupère le fichier
                OutputStream os = null; //On le mets sur le telephone
                HttpURLConnection connection = null; //Connexion pour le telechargement

                try {
                    URL url = new URL(path); //On crée l'URL
                    connection = (HttpURLConnection) url.openConnection(); //On crée la connexion
                    connection.connect(); //On effectue la connexion

                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) { //Si la connexion echoue
                        return "Server return HTTP " + connection.getResponseCode() + " "
                                + connection.getResponseMessage();
                    }

                    int intputLength = connection.getContentLength(); //On récupère la taille du fichier
                    file_size = intputLength;

                    is = connection.getInputStream(); //On se connecte au fichier
                    File song = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+
                            "/MediaPlayer/" + name); //On dit ou il va être
                    try {
                        song.createNewFile(); //On crée le future fichier
                        os = new FileOutputStream(song); //On crée le chemin pour l'y mettre
                    }catch (Exception e) {
                        e.printStackTrace();
                    }


                    byte[] data = new byte[4096];
                    int count; int total = 0; //Indice pour boucler le temps du telechargement

                    while ((count = is.read(data)) != -1) { //On le telecharger
                        total += count; //Pour boucler le temps du telechargement
                        os.write(data, 0, count); //On ecrit le fichier dans le telephone
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    try { //A la fin du telechargement on supprimer les connexion
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
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String res) {
            super.onPostExecute(res);
            if (res != null) { //Si le telechargement c'est bien fini
                Toast.makeText(getApplicationContext(), "Error: "+res, Toast.LENGTH_SHORT).show();
            }else{ //Sinon
                Toast.makeText(getApplicationContext(), "Downloaded", Toast.LENGTH_SHORT).show();
            }
        }
    }

}


