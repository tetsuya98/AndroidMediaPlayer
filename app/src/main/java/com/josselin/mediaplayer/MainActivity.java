package com.josselin.mediaplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements listAdapter.customButtonListener {

    private ListView listView;
    ListAdapter adapter;
    ArrayList<String> dataItems = new ArrayList<String>();
    ArrayList<String> urlItems = new ArrayList<String>();
    MediaPlayer player;
    public static final int MY_PERMISSION = 1;
    String img_url = "https://home.mis.u-picardie.fr/~ionica/musique/DeathGrips.mp3";
    //String img_url = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c2/Adler.jpg/547px-Adler.jpg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Permission
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.INTERNET
                },
                100);

        String[] dataArray = getResources().getStringArray(R.array.music);
        String[] urlArray = getResources().getStringArray(R.array.download);
        List<String> dataTemp = Arrays.asList(dataArray);
        List<String> urlTemp = Arrays.asList(urlArray);
        dataItems.addAll(dataTemp);
        urlItems.addAll(urlTemp);
        listView = findViewById(R.id.listView);
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
                new DownloadTask().execute(img_url);
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
    public void playPlayer(String id) {
        try{
            if (player == null) {
                int resID = getResources().getIdentifier(id, "raw", getPackageName());
                player = MediaPlayer.create(this, resID);
                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        stopPlayer();
                    }
                });
            }
            player.start();
        }catch (Exception e){
            Toast.makeText(this, "A Telecharger", Toast.LENGTH_SHORT).show();
        }

    }
    public void pausePlayer() {
        if (player != null) {
            player.pause();
        }
    }
    public void stopPlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    //Toolbar
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_pause:
                pausePlayer();
                return true;
            case R.id.action_stop:
                stopPlayer();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Download
    class DownloadTask extends AsyncTask<String,Integer,String>
    {
        ProgressDialog progressDialog;
        String name;
        double file_size;

        @Override
        protected void onPreExecute() {
            //super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setTitle("Downloading...");
            progressDialog.setMessage("File size: 0 MB");
            progressDialog.setIndeterminate(true);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setCancelable(true);

            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    Toast.makeText(getApplicationContext(), "Download cancelled !", Toast.LENGTH_SHORT).show();

                    File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+
                            "/MediaPlayer/" + name);
                    try {
                        dir.delete();
                    }catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            });
            progressDialog.show();
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
                    System.out.println("-------" + name);
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

            /*int total=0; int count=0;
            String path = params[0];
            int file_length = 0;
            try {
                URL url = new URL(path);
                URLConnection uc = url.openConnection();
                uc.connect();
                file_length = uc.getContentLength();
                File folder = new File("sdcard/mediaplayer");
                if (!folder.exists()) {
                    folder.mkdir();
                }
                System.out.println("-------------------------------------------"+params[0]);

                File input_file = new File(folder,"image.mp3");
                InputStream is = new BufferedInputStream(url.openStream(), 8192);
                byte[] data = new byte[1024];
                OutputStream os = new FileOutputStream(input_file);
                while((count=is.read(data))!=-1){
                    total += count;
                    os.write(data, 0, count);
                    //int progress = (int)total + 100 / file_length;
                    //publishProgress(progress);
                }
                is.close(); os.close();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            //super.onProgressUpdate(values);
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(100);
            progressDialog.setProgress(values[0]);
            progressDialog.setMessage("File size : " + new DecimalFormat("##.##").format(file_size / 1000000) + "MB");
        }

        @Override
        protected void onPostExecute(String res) {
            //super.onPostExecute(aVoid);
            progressDialog.dismiss();
            if (res != null) {
                Toast.makeText(getApplicationContext(), "Error: "+res, Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(getApplicationContext(), "Downloaded", Toast.LENGTH_SHORT).show();
            }
        }
    }

}


