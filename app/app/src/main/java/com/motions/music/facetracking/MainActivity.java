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
import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.Core;
import org.opencv.core.*;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

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

    Mat mRgba;
    Mat mRgbaF;
    Mat mRgbaT;


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
//        Core.setErrorVerbosity(false);
//        Core.BUI
        VideoCapture c = new VideoCapture(1);
//        c.set(Highgui.CV_CAP)
        CameraBridgeViewBase cameraView = (CameraBridgeViewBase) findViewById(R.id.java_surface_view);

        cameraView.setMaxFrameSize(200, 150);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCameraIndex(1);

        cameraView.enableFpsMeter();
        cameraView.setCvCameraViewListener(this);
        cameraView.enableView();


        output = findViewById(R.id.nb_noses);

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
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.i("CameraView", "started");
        handler = new Handler();
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.gray();
        Mat mRgba = inputFrame.rgba();
        Core.rotate(mRgba, mRgbaT, Core.ROTATE_90_COUNTERCLOCKWISE);
        Size size = new Size(frame.size().width, frame.size().height);
        Imgproc.resize(mRgbaT, mRgba, size, 0,0, 0);
//        Core.flip(mRgbaF, mRgba, 1 );
        MatOfRect rects = new MatOfRect();
        if (classifier != null)
            classifier.detectMultiScale(frame, rects);
//        else Log.wtf("CameraListener", "Null classifier");
        final Rect[] r = rects.toArray();
        handler.post(new Runnable() {
            @Override
            public void run() {
                output.setText(Integer.toString(r.length));
            }
        });
        Rect[] facesArray = rects.toArray();
        for (int i = 0; i < facesArray.length; i++)
            Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), new Scalar(100), 3);
//        Log.i("Running", "Running");
        return mRgba;
    }

}
