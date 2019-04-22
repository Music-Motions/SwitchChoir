package com.motions.music.facetracking;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;

public class UIandRecord extends AppCompatActivity {

    private ImageButton keyC = null;
    private ImageButton keyD = null;
    private ImageButton keyE = null;
    private ImageButton keyF = null;
    private ImageButton keyG = null;

    private Button rec = null;
    private MediaRecorder record;
    private MediaPlayer mp;
    public static final int RECORD_AUDIO = 0;
    private String FILE; //File path
    public static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 0;
    String filename = "myfile";
    String fileContents = "Hello world!";
    FileOutputStream outputStream;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        FILE = Environment.getExternalStorageDirectory() + "/tempRecord.3gpp";
        Log.d("Suhani", "The place is " + Environment.getExternalStorageDirectory().getPath());
        buttonSound();

        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(fileContents.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        rec = (Button)findViewById(R.id.rec);//rec button
        rec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rec.getText().toString().equals("Record")) {
                    try {
                        startRecord();
                        Log.d("Suhani", "Start Record Method");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
//                    rec.setText(R.string.end);
                    rec.setText("End");

                }
                else if (rec.getText().toString().equals("End")) { //Stop Recording
                    stopRecord();
                    Log.d("Suhani", "Stop Record Method");
                    rec.setText("Play");
//                    rec.setText("Play");
                }
                else if (rec.getText().toString().equals("Play")) {//Playback
                    Log.d("Suhani", "Before try catch");
                    try {
                        startPlayback();
                        Log.d("Suhani", "Start Playback Method");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Log.d("Suhani", "End in method");
                    rec.setText("Stop");

                }
                else {//Stop Playback
                    stopPlayback();
                    Log.d("Suhani", "Stop Playback Method");
                    rec.setText("Record");
                }

            }

        });


    }

    public void buttonSound(){//assigns sounds for all buttons

        keyC = (ImageButton) this.findViewById(R.id.keyC);
        keyC.setSoundEffectsEnabled(false);

        keyD = (ImageButton) this.findViewById(R.id.keyD);
        keyD.setSoundEffectsEnabled(false);

        keyE = (ImageButton) this.findViewById(R.id.keyE);
        keyE.setSoundEffectsEnabled(false);

        keyF = (ImageButton) this.findViewById(R.id.keyF);
        keyF.setSoundEffectsEnabled(false);

        keyG = (ImageButton) this.findViewById(R.id.keyG);
        keyG.setSoundEffectsEnabled(false);




        keyC.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                mp = MediaPlayer.create(UIandRecord.this, R.raw.pianoc);
                mp.start();
                mp.setOnCompletionListener(new OnCompletionListener() {//When sound ends
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.release();//Releases system resources

                    }
                });
                return true;
            }


        });

        keyD.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                mp = MediaPlayer.create(UIandRecord.this, R.raw.pianod);
                mp.start();
                mp.setOnCompletionListener(new OnCompletionListener() {//When sound ends
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.release();//Releases system resources

                    }
                });
                return true;


            }

        });
        keyE.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                mp = MediaPlayer.create(UIandRecord.this, R.raw.pianoe);
                mp.start();
                mp.setOnCompletionListener(new OnCompletionListener() {//When sound ends
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.release();//Releases system resources

                    }
                });
                return true;


            }

        });
        keyF.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                mp = MediaPlayer.create(UIandRecord.this, R.raw.pianof);
                mp.start();
                mp.setOnCompletionListener(new OnCompletionListener() {//When sound ends
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.release();//Releases system resources

                    }
                });
                return true;


            }

        });
        keyG.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                mp = MediaPlayer.create(UIandRecord.this, R.raw.pianog);
                mp.start();
                mp.setOnCompletionListener(new OnCompletionListener() {//When sound ends
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.release();//Releases system resources

                    }
                });
                return true;


            }

        });


    }

    public void startRecord() throws Exception{//Throws exceptions
        if (record!=null) {
            record.reset();
            record.release();
            Log.d("Suhani", "If record is not null");
        }
        File fileOut = new File(FILE);
        if (fileOut!=null) {
            fileOut.delete();//Overwrites existing file before recording
            Log.d("Suhani", "If file is not null");
        }
        record = new MediaRecorder();
        record.setAudioSource(MediaRecorder.AudioSource.MIC);
        record.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        record.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        record.setOutputFile(FILE);//File path
        Log.d("Suhani", "Set output file position");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.RECORD_AUDIO }, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
            Log.d("Suhani", "Requesting Permissions");
        } else {
            record.prepare();
            Log.d("Suhani", "Record is prepared");
            record.start();
            Log.d("Suhani", "Record has started");
        }

    }
    public void stopRecord() {
        record.stop();
        Log.d("Suhani", "Record is stopped");
        record.reset();
        Log.d("Suhani", "Record is reset");
        record.release();
        Log.d("Suhani", "Record is released");
    }
    public void startPlayback() throws Exception{
        if(mp!=null) {
            mp.stop();
            mp.release();
            Log.d("Suhani", "MediaPlayer is not null");
        }
        mp = new MediaPlayer();
        mp.setDataSource(FILE);
        Log.d("Suhani", "Data source is set");
        mp.prepare();
        Log.d("Suhani", "MediaPlayer is prepared");
        mp.start();
        Log.d("Suhani", "MediaPlayer is started");
        mp.setOnCompletionListener(new OnCompletionListener() {//When sound ends
            @Override
            public void onCompletion(MediaPlayer play) {
                play.release();//Releases system resources

            }
        });
    }
    public void stopPlayback() {

    }
}
