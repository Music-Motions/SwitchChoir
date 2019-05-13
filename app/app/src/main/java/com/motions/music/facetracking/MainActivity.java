package com.motions.music.facetracking;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
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
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    CascadeClassifier classifier;

    Mat mRgbaT;
    NoseThresholder nosePlayer;
    private int width = -1;
    private int height = -1;

    private Button keyCtop = null;
    private Button keyDtop = null;
    private Button keyEtop = null;
    private Button keyFtop = null;
    private Button keyGtop = null;
    private Button keyAtop = null;
    private Button keyBtop = null;
    private Button keyC5top = null;

    private Button keyDflat = null;
    private Button keyEflat = null;
    private Button keyGflat = null;
    private Button keyAflat = null;
    private Button keyBflat = null;


    private Button keyCbottom = null;
    private Button keyDbottom = null;
    private Button keyEbottom = null;
    private Button keyFbottom = null;
    private Button keyGbottom = null;
    private Button keyAbottom = null;
    private Button keyBbottom = null;
    private Button keyC5bottom = null;

    private Button exporter;
    private Button player;
    private Button rec = null;
    private MediaRecorder record;
    private MediaPlayer mp;
    public static final int RECORD_AUDIO = 0;
    private String FILE; //File path
    public static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 0;
    private String time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);


        long timeStamp = System.currentTimeMillis();
        time = Long.toString(System.currentTimeMillis());
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeStamp);

        int mYear = calendar.get(Calendar.YEAR);
        int mMonth = 1 + calendar.get(Calendar.MONTH);
        int mDay = calendar.get(Calendar.DAY_OF_MONTH);
        int mHour = calendar.get(Calendar.HOUR_OF_DAY);
        int mMinute = calendar.get(Calendar.MINUTE);
        int mSecond = calendar.get(Calendar.SECOND);

        String year = Integer.toString(mYear);
        String month = Integer.toString(mMonth);
        String day = Integer.toString(mDay);
        String hour = Integer.toString(mHour);
        String min = Integer.toString(mMinute);
        String sec = Integer.toString(mSecond);
        String timestamp = month + "_" + day + "_" + hour + "-" + min;

        FILE = Environment.getExternalStorageDirectory() + "/Music/" + "/" + timestamp + ".3gpp";

        Log.d("Suhani", "The place is " + Environment.getExternalStorageDirectory().getAbsolutePath());
        buttonSound();

        exporter = (Button) findViewById(R.id.exporter);//Exporter
        exporter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri selectedUri = Uri.parse(Environment.getExternalStorageDirectory() + "/Music/");
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(selectedUri, "resource/folder");

                if (intent.resolveActivityInfo(getPackageManager(), 0) != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), "Oops! No file explorer found!", Toast.LENGTH_SHORT).show();
                    // if you reach this place, it means there is no any file
                    // explorer app installed on your device
                }
            }

        });

        player = (Button) findViewById(R.id.player);
        player.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player.getText().toString().equals("Play")) {//Playback
                    Log.d("Suhani", "3 Before try catch");
                    try {
                        Log.d("Suhani", "4 Start Playback Method");
                        startPlayback();

                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d("Suhani", e.getMessage());

                    }
                    Log.d("Suhani", "5 End in method");
                    player.setText("Stop");
//                    player.setBackgroundResource(R.drawable.stopsign);

                } else {//Stop Playback
                    stopPlayback();
                    player.setText("Play");
                    Log.d("Suhani", "6 Stop Playback Method");

                }
            }
        });

        rec = (Button) findViewById(R.id.rec);//rec button
        rec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rec.getText().toString().equals("Record")) {
                    try {
                        Log.d("Suhani", "1 Start Record Method");
                        startRecord();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    rec.setText("End");

                } else {//Stop Recording
                    Log.d("Suhani", "2 Stop Record Method");
                    stopRecord();
                    rec.setText("Record");

                }

            }

        });


        // Set up camera view, used to get frames
        CameraBridgeViewBase cameraView = (CameraBridgeViewBase) findViewById(R.id.java_surface_view);
//        cameraView
        cameraView.setMaxFrameSize(320, 240);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCameraIndex(1);
        cameraView.setCvCameraViewListener(this);
        cameraView.enableView();


        // Load classifier into code
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
        classifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
        Log.wtf("Classifier", cascadeFile.getAbsolutePath());
        if (classifier.empty()) {
            Log.wtf("Cascade Error", "Failed to load cascade classifier");
            Toast.makeText(this, "Cascade failure", Toast.LENGTH_LONG);
        }

        // Set up listener
        nosePlayer = new NoseThresholder();

        nosePlayer.setNoseListener(new NoseThresholder.NoseListener() {
            @Override
            public void onNoseThresholdPassed(Rect r) {
                int x, y;
                x = r.x;
                y = r.y;
                int note;
                double angle = Math.toDegrees(Math.atan((double) (x - width / 2) / (double) (y - height / 2)));
//                Log.wtf("NoseListener", "Nose found at: " + Integer.toString(r.x));
                String name;
                if (x < width / 8)
                    note = R.raw.keyc;
                else if (x < 2 * width / 8)
                    note = R.raw.keyd;
                else if (x < 3 * width / 8)
                    note = R.raw.keye;
                else if (x < 4 * width / 8)
                    note = R.raw.keyf;
                else if (x < 5 * width / 8)
                    note = R.raw.keyg;
                else if (x < 6 * width / 8)
                    note = R.raw.keya;
                else if (x < 7 * width)
                    note = R.raw.keyb;
                else
                    note = R.raw.keyc5;
//                if (337.5 < angle || angle < 22.5) {
//                    note = R.raw.keyc;
//                    name = "low c";
//                } else if (22.5 <= angle && angle < 67.5) {
//                    note = R.raw.keyd;
//                    name = "d";
//                } else if (67.5 <= angle && angle < 112.5) {
//                    note = R.raw.keye;
//                    name = "e";
//                } else if (112.5 <= angle && angle < 157.5) {
//                    note = R.raw.keyf;
//                    name = "f";
//                } else if (157.5 <= angle && angle < 202.5) {
//                    note = R.raw.keyg;
//                    name = "g";
//                } else if (202.5 <= angle && angle < 247.5) {
//                    note = R.raw.keya;
//                    name = "a";
//                } else if (247.5 <= angle && angle < 292.5) {
//                    note = R.raw.keyb;
//                    name = "b";
//                } else {// needs to be higher C
//                    note = R.raw.keyc5;
//                    name = "c";
//                }
//                Log.wtf("Face", name + " " + angle);

                mp = MediaPlayer.create(MainActivity.this, note);
                mp.start();
                mp.setOnCompletionListener(new OnCompletionListener() { //When sound ends
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.release();//Releases system resources
                    }
                });
            }
        });

        Log.wtf("OnCreate", "Done");
    }

    public void buttonSound() {//assigns sounds for all buttons

        keyCtop = (Button) this.findViewById(R.id.keyCtop);
        Log.wtf("Yikes", keyCtop.toString());
        Log.wtf("Yikes", Integer.toString(R.id.keyCtop));
        keyCtop.setSoundEffectsEnabled(false);

        keyDflat = (Button) this.findViewById(R.id.keyDflat);
        keyDflat.setSoundEffectsEnabled(false);

        keyDtop = (Button) this.findViewById(R.id.keyDtop);
        keyDtop.setSoundEffectsEnabled(false);

        keyEflat = (Button) this.findViewById(R.id.keyEflat);
        keyEflat.setSoundEffectsEnabled(false);

        keyEtop = (Button) this.findViewById(R.id.keyEtop);
        keyEtop.setSoundEffectsEnabled(false);

        keyFtop = (Button) this.findViewById(R.id.keyFtop);
        keyFtop.setSoundEffectsEnabled(false);

        keyGflat = (Button) this.findViewById(R.id.keyGflat);
        keyGflat.setSoundEffectsEnabled(false);

        keyGtop = (Button) this.findViewById(R.id.keyGtop);
        keyGtop.setSoundEffectsEnabled(false);

        keyAflat = (Button) this.findViewById(R.id.keyAflat);
        keyAflat.setSoundEffectsEnabled(false);

        keyAtop = (Button) this.findViewById(R.id.keyAtop);
        keyAtop.setSoundEffectsEnabled(false);

        keyBflat = (Button) this.findViewById(R.id.keyBflat);
        keyBflat.setSoundEffectsEnabled(false);

        keyBtop = (Button) this.findViewById(R.id.keyBtop);
        keyBtop.setSoundEffectsEnabled(false);

        keyC5top = (Button) this.findViewById(R.id.keyC5top);
        keyC5top.setSoundEffectsEnabled(false);


        //bottom parts of the keys initialized
        keyCbottom = (Button) this.findViewById(R.id.keyCbottom);
        keyCbottom.setSoundEffectsEnabled(false);

        keyDbottom = (Button) this.findViewById(R.id.keyDbottom);
        keyDbottom.setSoundEffectsEnabled(false);

        keyEbottom = (Button) this.findViewById(R.id.keyEbottom);
        keyEbottom.setSoundEffectsEnabled(false);

        keyFbottom = (Button) this.findViewById(R.id.keyFbottom);
        keyFbottom.setSoundEffectsEnabled(false);

        keyGbottom = (Button) this.findViewById(R.id.keyGbottom);
        keyGbottom.setSoundEffectsEnabled(false);

        keyAbottom = (Button) this.findViewById(R.id.keyAbottom);
        keyAbottom.setSoundEffectsEnabled(false);

        keyBbottom = (Button) this.findViewById(R.id.keyBbottom);
        keyBbottom.setSoundEffectsEnabled(false);

        keyC5bottom = (Button) this.findViewById(R.id.keyC5bottom);
        keyC5bottom.setSoundEffectsEnabled(false);

//giving the notes sounds
        keyCtop.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.keya);
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
        keyCbottom.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.keyc);
                if (mp == null) {
                    Log.d("Suhani", "null");
                } else {
                    mp.start();
                    mp.setOnCompletionListener(new OnCompletionListener() {//When sound ends
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            mp.release();//Releases system resources

                        }
                    });
                }
                return true;
            }


        });

        keyDflat.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.keydb);
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

        keyDtop.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.keyd);
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
        keyDbottom.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.keyd);
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

        keyEflat.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.keyeb);
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

        keyEtop.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.keye);
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
        keyEbottom.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.keye);
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

        keyFtop.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.keyf);
                mp.start();
                mp.setOnCompletionListener(new OnCompletionListener() {//When sound ends
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Log.d("Suhani", "Player: MediaPlayer released");
                        mp.release();//Releases system resources

                    }
                });
                return true;


            }

        });
        keyFbottom.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.keyf);
                mp.start();
                mp.setOnCompletionListener(new OnCompletionListener() {//When sound ends
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Log.d("Suhani", "Player: MediaPlayer released");
                        mp.release();//Releases system resources

                    }
                });
                return true;


            }

        });

        keyGflat.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.keygb);
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

        keyGtop.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.keyg);
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


        keyGbottom.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.keyg);
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
        keyAflat.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.keyab);
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
        keyAtop.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.keya);
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

        keyAbottom.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.keya);
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
        keyBflat.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.keybb);
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
        keyBtop.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.keyb);
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
        keyBbottom.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.keyb);
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
        keyC5top.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.keyc5);
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
        keyC5bottom.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.keyc5);
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


    public void startRecord() throws Exception {//Throws exceptions
        if (record != null) {
            Log.d("Suhani", "1a Record Stop");
            record.stop();
            Log.d("Suhani", "1b Record Reset");
            record.reset();
            Log.d("Suhani", "1c Record Release");
            record.release();

        }
        File fileOut = new File(FILE);
        if (fileOut != null) {
            fileOut.delete();//Overwrites existing file before recording
            Log.d("Suhani", "If file is not null");
        }
        record = new MediaRecorder();
        record.setAudioSource(MediaRecorder.AudioSource.MIC);
        record.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        record.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        record.setOutputFile(FILE);//File path

        try {
            record.prepare();
        } catch (IOException e) {
            Log.d("Suhani", " prepare() failed");
        }
        record.start();
    }


    public void stopRecord() {
        Log.d("Suhani", "2a Record is stopped");
        record.stop();
        Log.d("Suhani", "2b Record is reset");
        Log.d("Suhani", "2c Record is released");
        record.release();

        record = null;
    }

    public void startPlayback() {

        mp = new MediaPlayer();
        Log.d("Suhani", "4d Before try catch");
        try {
            Log.d("Suhani", "4e MediaPlayer setting data source");
            mp.setDataSource(FILE);
            mp.prepare();
            mp.start();
        } catch (IOException e) {
            Log.d("Suhani", "4f In the catch exception  " + e.getMessage());
            e.printStackTrace();
            Log.d("Suhani", "4g setDataSource " + e.getMessage());
        }
        Log.d("Suhani", "4h After try catch to prepare");

        Log.d("Suhani", "4l MediaPlayer is starting");


    }

    public void stopPlayback() {

        mp.release();//Releases system resources
        mp = null;
        Log.d("Suhani", "6c MediaPlayer is released");


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
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
        this.width = width;
        this.height = height;
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
//        Core.rotate(frame, mRgbaT, Core.ROTATE_90_COUNTERCLOCKWISE);
//        Imgproc.resize(mRgbaT, frame, frame.size(), 0, 0, 0);
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
            Imgproc.rectangle(frame, feature.tl(), feature.br(), new Scalar(100), 3);

        }

        if (nose != null)
            nosePlayer.listener.onNoseThresholdPassed(nose);
//        return new Mat();
        return frame;
    }

}