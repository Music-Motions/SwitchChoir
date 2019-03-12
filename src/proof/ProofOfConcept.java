package proof;

import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.imgproc.Imgproc;
import org.opencv.highgui.HighGui;

public class ProofOfConcept {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    static CascadeClassifier classifier;
    static VideoCapture camera;

    public static void main(String[] args) {
//        System.out.println("Hello world " + Core.VERSION);
        classifier = new CascadeClassifier("C:\\Users\\gcper\\MAMS\\Junior Year\\STEM II\\Project\\src\\proof\\haarcascade_mcs_nose.xml");
        camera = new VideoCapture(0);
        camera.read(new Mat());
        try {
            Thread.currentThread().sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (true) {
            Mat image = new Mat();
            camera.read(image);
            Mat gray = new Mat();
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
            MatOfRect noses = new MatOfRect();

            classifier.detectMultiScale(gray, noses);
            for (Rect nose : noses.toArray()) {
                Imgproc.rectangle(image, new Point(nose.x, nose.y),
                        new Point(nose.x + nose.width, nose.y + nose.height),
                        new Scalar(0, 255, 0));
            }
            HighGui.imshow("Noses", image);
            if (HighGui.waitKey(1) == 'a')
                break;
        }
        camera.release();
        HighGui.destroyAllWindows();
    }
}
