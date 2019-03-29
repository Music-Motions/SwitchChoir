package com.motions.music.facetracking;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
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
    TextView output;
    boolean breakLoop = false;
    Thread thread;
    Handler handler;
    View v;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        System.out.println("Hello world " + Core.VERSION);
        thread = Thread.currentThread();

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast toast = Toast.makeText(MainActivity.this, Core.VERSION, Toast.LENGTH_LONG);
                toast.show();
                breakLoop = true;
                Log.i("Callback", "running button");
            }
        });
        CameraBridgeViewBase cameraView = (CameraBridgeViewBase) findViewById(R.id.java_surface_view);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCameraIndex(1);
        cameraView.setCvCameraViewListener(this);
        cameraView.enableView();

        output = findViewById(R.id.nb_noses);

        BaseLoaderCallback callback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS:
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
//                            classifier = null;
                        }
                        break;
                    default:
                        Log.wtf("FRICK", "FAILED LOADING CASCADE");
                        break;
                }
                Log.i("Loader", "loaded");
            }
        };
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.i("CameraView", "started");
        handler = new Handler();
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.gray();
        Mat rgb = inputFrame.rgba();
//        Core.flip(frame, frame, 1);
        MatOfRect rects = new MatOfRect();
        if (classifier != null)
            classifier.detectMultiScale(frame, rects);
        else Log.wtf("CameraListener", "Null classifier");
        final Rect[] r = rects.toArray();
        handler.post(new Runnable() {
            @Override
            public void run() {
                output.setText(Integer.toString(r.length));
            }
        });
        Rect[] facesArray = rects.toArray();
        for (int i = 0; i < facesArray.length; i++)
            Imgproc.rectangle(rgb, facesArray[i].tl(), facesArray[i].br(), new Scalar(100), 3);
//        Log.i("Running", "Running");
        return rgb;
    }

}
