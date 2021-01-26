package es.upv.alphacodeteam.neoscanner.view.activity;

import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import es.upv.alphacodeteam.neoscanner.R;
import es.upv.alphacodeteam.neoscanner.view.util.CameraManager;

public class CameraActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    private CameraBridgeViewBase cameraView = null;
    private static boolean initOpenCV = false;

    static { initOpenCV = OpenCVLoader.initDebug(); }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        cameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (initOpenCV) { cameraView.enableView(); }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Release the camera.
        if (cameraView != null) {
            cameraView.disableView();
            cameraView = null;
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) { }

    @Override
    public void onCameraViewStopped() { }
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Mat src = inputFrame.gray(); // convertir a escala de grises
        Mat cannyEdges = new Mat();  // objeto para almacenar el resultado

        // aplicar el algoritmo canny para detectar los bordes
        Imgproc.Canny(src, cannyEdges, 10, 100);

        // devolver el objeto Mat procesado
        return cannyEdges;
    }
}