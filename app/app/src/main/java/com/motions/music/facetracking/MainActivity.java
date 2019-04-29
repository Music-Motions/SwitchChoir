package com.motions.music.facetracking;

import android.content.Context;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.media.MediaPlayer.OnCompletionListener;

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
    private int height = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                int note;
                double angle = Math.toDegrees(Math.atan((double) (x - width / 2) / (double) (y - height / 2)));
//                Log.wtf("NoseListener", "Nose found at: " + Integer.toString(r.x));
                if (337.5 < angle || angle < 22.5)
                    note = R.raw.pianoc;
                else if (22.5 <= angle && angle < 67.5)
                    note = R.raw.pianod;
                else if (67.5 <= angle && angle < 112.5)
                    note = R.raw.pianoe;
                else if (112.5 <= angle && angle < 157.5)
                    note = R.raw.pianof;
                else if (157.5 <= angle && angle < 202.5)
                    note = R.raw.pianog;
                else if (202.5 <= angle && angle < 247.5)
                    note = R.raw.pianoa;
                else if (247.5 <= angle && angle < 292.5)
                    note = R.raw.pianob;
                else // needs to be higher C
                    note = R.raw.pianoc;
//
//
//                if (x < width / 2)
//                    note = R.raw.pianoa;
//                else
//                    note = R.raw.pianog;
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
        Core.rotate(frame, mRgbaT, Core.ROTATE_90_COUNTERCLOCKWISE);
        Imgproc.resize(mRgbaT, frame, frame.size(), 0, 0, 0);
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
        return new Mat();
    }

}
