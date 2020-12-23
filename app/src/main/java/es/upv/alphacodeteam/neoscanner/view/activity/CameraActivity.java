package es.upv.alphacodeteam.neoscanner.view.activity;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import es.upv.alphacodeteam.neoscanner.R;
import es.upv.alphacodeteam.neoscanner.view.util.CameraManager;

public class CameraActivity extends AppCompatActivity {

    private CameraManager camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        this.camera = new CameraManager(this);

        FrameLayout preview = (FrameLayout) this.findViewById(R.id.camera_preview);
        preview.addView(this.camera.getPreview());
    }


    /**
     * Hide/Show the toolbar. Enable Fullscreen mode for camera preview.
     **/

    @Override
    public void onResume() {
        super.onResume();
        this.getSupportActionBar().hide();
    }

    @Override
    public void onStop() {
        super.onStop();
        this.getSupportActionBar().show();
        this.camera.stop();
    }

}
