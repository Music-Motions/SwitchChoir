package com.motions.music.facetracking;

import android.content.Context;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.*;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    CascadeClassifier classifier;

    Mat mRgbaT;
    NoseThresholder nosePlayer;
    private MediaPlayer mp;
    private int width = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Button ensuring OpenCV installed
//        findViewById(R.id.rec).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Toast toast = Toast.makeText(MainActivity.this, Core.VERSION, Toast.LENGTH_LONG);
//                toast.show();
//                Log.i("Callback", "running button");
//            }
//        });

        // Set up camera view, used to get frames
        CameraBridgeViewBase cameraView = (CameraBridgeViewBase) findViewById(R.id.java_surface_view);
//        cameraView.setMaxFrameSize(200, 150);
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
        }

        // Set up listener
        nosePlayer = new NoseThresholder();

        nosePlayer.setNoseListener(new NoseThresholder.NoseListener() {
            @Override
            public void onNoseThresholdPassed(Rect r) {
                int x, y;
                x = r.x;
                y = r.y;
                Log.wtf("NoseListener", "Nose found at: "+Integer.toString(r.x));
                int note;
                if (x < width/2)
                    note = R.raw.pianoa;
                else
                    note = R.raw.pianog;
                mp = MediaPlayer.create(MainActivity.this, note);
                mp.start();
                mp.setOnCompletionListener(new OnCompletionListener() {//When sound ends
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.release();//Releases system resources
                    }
                });
            }
        });

        Log.wtf("OnCreate", "Done");
    }

    /**
     * Creates frame used for rotating, as image is inputted rotated 90 degrees
     * @param width -  the width of the frames that will be delivered
     * @param height - the height of the frames that will be delivered
     */
    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.wtf("CameraView", "started");
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
        this.width = width;
    }

    @Override
    public void onCameraViewStopped() {

    }
    /**
     * Takes camera frame, runs classifier, calls listener if nose detected
     * @param inputFrame current frame to process
     * @return output frame (unused)
     */
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
//        Log.wtf("Grant", Integer.toString(inputFrame.gray().width()));
//         Normalize image
        Mat frame = inputFrame.gray();
        Core.rotate(frame, mRgbaT, Core.ROTATE_90_COUNTERCLOCKWISE);
        Imgproc.resize(mRgbaT, frame, frame.size(), 0,0, 0);
        MatOfRect rects = new MatOfRect();

        // Detect noses
        if (classifier != null)
            classifier.detectMultiScale(frame, rects);
        else Log.wtf("CameraListener", "Null classifier");

        // Sort noses, largest is "real" nose
        Rect[] facesArray = rects.toArray();
        Rect nose = null;
        for (int i = 0; i < facesArray.length; i++) {
            if (nose == null)
                nose = facesArray[i];
            else if (facesArray[i].width * facesArray[i].height > nose.width*nose.height)
                nose = facesArray[i];
        }

        // Call listener with nose
//        Log.wtf("frame", Integer.toString(frame.width()) +" "+ Integer.toString(frame.height()));
        if (nose != null) {
            nosePlayer.listener.onNoseThresholdPassed(nose);
//            Log.wtf("Nose","yay");
        } else {
//            Log.wtf("No nose", Integer.toString(frame.width()));
        }
        return frame;
    }

}
