package com.motions.music.facetracking;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.media.MediaPlayer.OnCompletionListener;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.*;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

/**
 * Our one, main activity
 */
public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    CascadeClassifier classifier;
    NoseThresholder nosePlayer;
    private int width = -1;

    private Button keyCtop, keyCbottom;
    private Button keyDflat;
    private Button keyDtop, keyDbottom;
    private Button keyEflat;
    private Button keyEtop, keyEbottom;
    private Button keyFtop, keyFbottom;
    private Button keyGflat;
    private Button keyGtop, keyGbottom;
    private Button keyAflat;
    private Button keyAtop, keyAbottom;
    private Button keyBflat;
    private Button keyBtop, keyBbottom;
    private Button keyC5top, keyC5bottom;

    private Button exporter = null;
    private Button player = null;
    private Button rec = null;
    private MediaRecorder record = null;
    private MediaPlayer mp = null;
    private String FILE; //File path
    private final int NO_NOTE = -1;

    private int notePlaying = NO_NOTE;

    /**
     * Main and only activity of our app
     * Constructs buttons, mediaplayer, mediarecorder, cascade classifier, and frame listener
     *
     * @param savedInstanceState last state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Ask for permissions
        int ASK_CAMERA = 1298, ASK_RECORD = 2193, ASK_WRITE = 3149, ASK_READ = 312;
        askPermission(Manifest.permission.CAMERA, ASK_CAMERA);
        askPermission(Manifest.permission.RECORD_AUDIO, ASK_RECORD);
        askPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, ASK_WRITE);
        askPermission(Manifest.permission.READ_EXTERNAL_STORAGE, ASK_READ);


        // construct all piano key buttons
        buttonSound();

        // Intent receiver used for graying buttons
        IntentFilter filter = new IntentFilter("com.facetracking.CHANGECOLOR");
        this.registerReceiver(new GrayIntentReceiver(), filter);

        // Exporting button
        exporter = (Button) findViewById(R.id.exporter);//Exporter
        exporter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri selectedUri = Uri.parse(Environment.getExternalStorageDirectory() + "/Music/");
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(selectedUri, "resource/folder");

                if (intent.resolveActivityInfo(getPackageManager(), 0) != null) {
                    // switch to file explorer
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), "Oops! No file explorer found!", Toast.LENGTH_SHORT).show();
                    // if you reach this place, it means there is no any file
                    // explorer app installed on your device
                }
            }

        });

        // Play recording button
        player = (Button) findViewById(R.id.player);
        player.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mp == null) {//Playback
                    try {
                        startPlayback();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.wtf("PlayButton", e.getMessage());
                    }
                } else {//Stop Playback
                    stopPlayback();
                    player.setBackgroundResource(R.drawable.playbutton);
                }
            }
        });

        // Record button
        rec = (Button) findViewById(R.id.rec);//rec button
        rec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (record == null) {
                    try {
                        startRecord();
                    } catch (Exception e) {
                        Log.wtf("RecordButton", e.getMessage());
                    }
                    rec.setBackgroundResource(R.drawable.stopsign);

                } else {//Stop Recording
                    stopRecord();
                    rec.setBackgroundResource(R.drawable.recordbutton);
                }
            }
        });


        // Set up camera view, used to get frames
        CameraBridgeViewBase cameraView = (CameraBridgeViewBase) findViewById(R.id.java_surface_view);
        cameraView.setMaxFrameSize(320, 240);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCameraIndex(1);
        cameraView.setCvCameraViewListener(this);
        cameraView.enableView();


        // Load pre-trained classifier file into code, from raw resources
        InputStream inputStream = getResources().openRawResource(R.raw.haarcascade_mcs_nose);
        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
        File cascadeFile = new File(cascadeDir, "cascade.xml");
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(cascadeFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(9);
        }
        byte[] buffer = new byte[4096];
        int bytesRead;
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize classifier
        classifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
        Log.wtf("Classifier", cascadeFile.getAbsolutePath());
        if (classifier.empty()) {
            Log.wtf("Cascade", "Failed to load cascade classifier");
            Toast.makeText(this, "FAILURE: PLEASE RESTART APP", Toast.LENGTH_LONG).show();
        }

        // Set up listener, thresholding values
        nosePlayer = new NoseThresholder();
        nosePlayer.setNoseListener(new NoseThresholder.NoseListener() {
            @Override
            public void onNoseThresholdPassed(Rect r) {
                int x = width - r.x;
                int note;
                x -= width / 8;
                int w = (width * 6) / 8;
                x = Math.min(Math.max(x, 0), w);

                if (x < w / 8)
                    note = R.raw.keyc;
                else if (x < 2 * w / 8)
                    note = R.raw.keyd;
                else if (x < 3 * w / 8)
                    note = R.raw.keye;
                else if (x < 4 * w / 8)
                    note = R.raw.keyf;
                else if (x < 5 * w / 8)
                    note = R.raw.keyg;
                else if (x < 6 * w / 8)
                    note = R.raw.keya;
                else if (x < 7 * w / 8)
                    note = R.raw.keyb;
                else
                    note = R.raw.keyc5;


                // Gray selected note
                Intent i = new Intent();
                i.setAction("com.facetracking.CHANGECOLOR");
                i.putExtra("note", note);
                sendBroadcast(i);
            }
        });
    }

    public void askPermission(String permission, int permId) {
        // Ask for permission to use camera
        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {

            } else {
                ActivityCompat.requestPermissions(this, new String[]{permission}, permId);
            }
        } else {
            // Permission has already been granted
        }
    }

    /**
     * Receives broadcast containing new note to gray. Thread-safe way to change background color using nose tracking.
     */
    private class GrayIntentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // Gets note to gray
            int note = arg1.getExtras().getInt("note");

            if (notePlaying != note) {
                resetButtonColors(); // reset background colors to rainbow

                notePlaying = note;
                int gray = Color.rgb(192, 192, 192);

                // gray selected note
                if (note == R.raw.keyc) {
                    keyCtop.setBackgroundColor(gray);
                    keyCbottom.setBackgroundColor(gray);
                } else if (note == R.raw.keyd) {
                    keyDtop.setBackgroundColor(gray);
                    keyDbottom.setBackgroundColor(gray);
                } else if (note == R.raw.keye) {
                    keyEtop.setBackgroundColor(gray);
                    keyEbottom.setBackgroundColor(gray);
                } else if (note == R.raw.keyf) {
                    keyFtop.setBackgroundColor(gray);
                    keyFbottom.setBackgroundColor(gray);
                } else if (note == R.raw.keyg) {
                    keyGtop.setBackgroundColor(gray);
                    keyGbottom.setBackgroundColor(gray);
                } else if (note == R.raw.keya) {
                    keyAtop.setBackgroundColor(gray);
                    keyAbottom.setBackgroundColor(gray);
                } else if (note == R.raw.keyb) {
                    keyBtop.setBackgroundColor(gray);
                    keyBbottom.setBackgroundColor(gray);
                } else if (note == R.raw.keyc5) {
                    keyC5top.setBackgroundColor(gray);
                    keyC5bottom.setBackgroundColor(gray);
                }

                // Play selected note
                mp = MediaPlayer.create(MainActivity.this, note);
                mp.start();
                mp.setOnCompletionListener(new OnCompletionListener() { //When sound ends
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.release();//Releases system resources
                        notePlaying = NO_NOTE;
                        resetButtonColors();
                    }
                });
            }

            // does not gray null notes
        }
    }

    /**
     * Assigns callback sounds to all keys. Very long method.
     */
    @SuppressLint("ClickableViewAccessibility")
    public void buttonSound() {

        keyCtop = (Button) this.findViewById(R.id.keyCtop);                           // C
        keyCtop.setSoundEffectsEnabled(false);
        keyCbottom = (Button) this.findViewById(R.id.keyCbottom);
        keyCbottom.setSoundEffectsEnabled(false);
        keyDflat = (Button) this.findViewById(R.id.keyDflat);                        // D flat
        keyDflat.setSoundEffectsEnabled(false);
        keyDtop = (Button) this.findViewById(R.id.keyDtop);                          // D
        keyDtop.setSoundEffectsEnabled(false);
        keyDbottom = (Button) this.findViewById(R.id.keyDbottom);
        keyDbottom.setSoundEffectsEnabled(false);
        keyEflat = (Button) this.findViewById(R.id.keyEflat);                        // E flat
        keyEflat.setSoundEffectsEnabled(false);
        keyEtop = (Button) this.findViewById(R.id.keyEtop);                          // E
        keyEtop.setSoundEffectsEnabled(false);
        keyEbottom = (Button) this.findViewById(R.id.keyEbottom);
        keyEbottom.setSoundEffectsEnabled(false);
        keyFtop = (Button) this.findViewById(R.id.keyFtop);                          // F
        keyFtop.setSoundEffectsEnabled(false);
        keyFbottom = (Button) this.findViewById(R.id.keyFbottom);
        keyFbottom.setSoundEffectsEnabled(false);
        keyGflat = (Button) this.findViewById(R.id.keyGflat);                        // G flat
        keyGflat.setSoundEffectsEnabled(false);
        keyGtop = (Button) this.findViewById(R.id.keyGtop);                          // G
        keyGtop.setSoundEffectsEnabled(false);
        keyGbottom = (Button) this.findViewById(R.id.keyGbottom);
        keyGbottom.setSoundEffectsEnabled(false);
        keyAflat = (Button) this.findViewById(R.id.keyAflat);                        // A flat
        keyAflat.setSoundEffectsEnabled(false);
        keyAbottom = (Button) this.findViewById(R.id.keyAbottom);
        keyAbottom.setSoundEffectsEnabled(false);
        keyAtop = (Button) this.findViewById(R.id.keyAtop);                          // A
        keyAtop.setSoundEffectsEnabled(false);
        keyBflat = (Button) this.findViewById(R.id.keyBflat);
        keyBflat.setSoundEffectsEnabled(false);
        keyBtop = (Button) this.findViewById(R.id.keyBtop);                          // B
        keyBtop.setSoundEffectsEnabled(false);
        keyBbottom = (Button) this.findViewById(R.id.keyBbottom);
        keyBbottom.setSoundEffectsEnabled(false);
        keyC5top = (Button) this.findViewById(R.id.keyC5top);                        // C (high)
        keyC5top.setSoundEffectsEnabled(false);
        keyC5bottom = (Button) this.findViewById(R.id.keyC5bottom);
        keyC5bottom.setSoundEffectsEnabled(false);


        /*
         * Assigns onTouch listener to every button
         */
        keyCtop.setOnTouchListener(new KeyButton(R.raw.keyc));
        keyCbottom.setOnTouchListener(new KeyButton(R.raw.keyc));
        keyDflat.setOnTouchListener(new KeyButton(R.raw.keydb));
        keyDtop.setOnTouchListener(new KeyButton(R.raw.keyd));
        keyDbottom.setOnTouchListener(new KeyButton(R.raw.keyd));
        keyEflat.setOnTouchListener(new KeyButton(R.raw.keye));
        keyEtop.setOnTouchListener(new KeyButton(R.raw.keye));
        keyEbottom.setOnTouchListener(new KeyButton(R.raw.keye));
        keyFtop.setOnTouchListener(new KeyButton(R.raw.keyf));
        keyFbottom.setOnTouchListener(new KeyButton(R.raw.keyf));
        keyGflat.setOnTouchListener(new KeyButton(R.raw.keygb));
        keyGtop.setOnTouchListener(new KeyButton(R.raw.keyg));
        keyGbottom.setOnTouchListener(new KeyButton(R.raw.keyg));
        keyAflat.setOnTouchListener(new KeyButton(R.raw.keyab));
        keyAtop.setOnTouchListener(new KeyButton(R.raw.keya));
        keyAbottom.setOnTouchListener(new KeyButton(R.raw.keya));
        keyBflat.setOnTouchListener(new KeyButton(R.raw.keybb));
        keyBtop.setOnTouchListener(new KeyButton(R.raw.keyb));
        keyBbottom.setOnTouchListener(new KeyButton(R.raw.keyb));
        keyC5top.setOnTouchListener(new KeyButton(R.raw.keyc5));
        keyC5bottom.setOnTouchListener(new KeyButton(R.raw.keyc5));
    }

    /**
     * Custom onTouchListener, plays given note
     */
    private class KeyButton implements View.OnTouchListener {
        int note;

        /**
         * Assigns note to be played when touched
         *
         * @param note id of raw note file (R.raw.NOTENAME)
         */
        public KeyButton(int note) {
            super();
            this.note = note;
        }

        /**
         * Plays note when pressed
         *
         * @param v     current view
         * @param event touch even
         * @return true
         */
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // Broadcasts change note to gray intent
            Intent i = new Intent();
            i.setAction("com.facetracking.CHANGECOLOR");
            i.putExtra("note", note);
            sendBroadcast(i);
            return true;
        }
    }

    /**
     * Starts the recording of the song
     */
    public void startRecord() {
        // Reset recorder if needed
        if (record != null) {
            record.stop();
            record.reset();
            record.release();
        }

        // Get time for filename
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        String month = Integer.toString(1 + calendar.get(Calendar.MONTH));
        String day = Integer.toString(calendar.get(Calendar.DAY_OF_MONTH));
        String hour = Integer.toString(calendar.get(Calendar.HOUR_OF_DAY));
        String min = Integer.toString(calendar.get(Calendar.MINUTE));
        String timestamp = month + "_" + day + "_" + hour + "-" + min;

        // Construct recording file
        FILE = Environment.getExternalStorageDirectory() + "/Music/" + "/" + timestamp + ".3gpp";
        File fileOut = new File(FILE);
        if (fileOut != null)
            fileOut.delete();//Overwrites existing file before recording
        record = new MediaRecorder();
        record.setAudioSource(MediaRecorder.AudioSource.MIC);
        record.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        record.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        record.setOutputFile(FILE);//File path

        // Begin recording
        try {
            record.prepare();
        } catch (IOException e) {
            Log.d("Recorder", "prepare() failed");
        }
        record.start();
    }

    /**
     * Stops recording the song
     */
    public void stopRecord() {
        record.stop();
        record.release();
        record = null;
        mp = null;
    }

    /**
     * Plays the last recorded song in this session
     */
    public void startPlayback() {

        mp = new MediaPlayer();
        Log.d("Suhani", "4d Before try catch");
        try {
            player.setBackgroundResource(R.drawable.stopsign);
            Log.d("Suhani", "4e MediaPlayer setting data source");
            mp.setDataSource(FILE);
            Log.d("Suhani", "4e MediaPlayer prepare");
            mp.prepare();
            Log.d("Suhani", "4e MediaPlayer start");
            mp.start();

        } catch (IOException e) {
            Log.d("Suhani", "4f In the catch exception  " + e.getMessage());
            e.printStackTrace();
            Log.d("Suhani", "4g setDataSource " + e.getMessage());
        }
        Log.d("Suhani", "4h After try catch to prepare");

        Log.d("Suhani", "4l MediaPlayer is starting");
        mp.setOnCompletionListener(new OnCompletionListener() {//When sound ends
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopPlayback();
            }

        });

    }

    /**
     * Stops playing back the recording
     */
    public void stopPlayback() {

        mp.release();//Releases system resources
        mp = null;
        Log.d("Suhani", "6c MediaPlayer is released");
        player.setBackgroundResource(R.drawable.playbutton);


    }

    /**
     * Resets the key colors to rainbow
     */
    public void resetButtonColors() {
        int ORANGE = Color.parseColor("#FF6B20");
        int BLUE = Color.parseColor("#02A9EA");
        int INDIGO = Color.parseColor("#5C6492");
        int VIOLET = Color.parseColor("#B438F4");

        keyCtop.setBackgroundColor(Color.RED);
        keyCbottom.setBackgroundColor(Color.RED);
        keyDtop.setBackgroundColor(ORANGE);
        keyDbottom.setBackgroundColor(ORANGE);
        keyEtop.setBackgroundColor(Color.YELLOW);
        keyEbottom.setBackgroundColor(Color.YELLOW);
        keyFtop.setBackgroundColor(Color.GREEN);
        keyFbottom.setBackgroundColor(Color.GREEN);
        keyGtop.setBackgroundColor(BLUE);
        keyGbottom.setBackgroundColor(BLUE);
        keyAtop.setBackgroundColor(INDIGO);
        keyAbottom.setBackgroundColor(INDIGO);
        keyBtop.setBackgroundColor(VIOLET);
        keyBbottom.setBackgroundColor(VIOLET);
        keyC5top.setBackgroundColor(Color.RED);
        keyC5bottom.setBackgroundColor(Color.RED);
        keyDflat.setBackgroundColor(Color.BLACK);
        keyEflat.setBackgroundColor(Color.BLACK);
        keyGflat.setBackgroundColor(Color.BLACK);
        keyAflat.setBackgroundColor(Color.BLACK);
        keyBflat.setBackgroundColor(Color.BLACK);

    }


    /**
     * Creates frame used for rotating, as image is inputted rotated 90 degrees
     *
     * @param width  -  the width of the frames that will be delivered
     * @param height - the height of the frames that will be delivered
     */
    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.i("CameraView", "started");
        this.width = width;
    }

    @Override
    public void onCameraViewStopped() {

    }

    /**
     * Takes camera frame, runs classifier, calls listener if nose detected
     *
     * @param inputFrame current frame to process
     * @return output frame (unused)
     */
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
//         Normalize image
        Mat frame = inputFrame.gray();
        MatOfRect rects = new MatOfRect();

        // Detect noses
        if (classifier != null)
            classifier.detectMultiScale(frame, rects);
        else Log.wtf("CameraListener", "Null classifier");

        // Sort noses, largest is "real" nose
        Rect[] facesArray = rects.toArray();
        Rect nose = null;
        for (Rect feature : facesArray) {
            if (nose == null)
                nose = feature;
            else if (feature.width * feature.height > nose.width * nose.height)
                nose = feature;
        }

        if (nose != null)
            nosePlayer.listener.onNoseThresholdPassed(nose);
        return frame;
    }

}