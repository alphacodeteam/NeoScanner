package es.upv.alphacodeteam.neoscanner.view.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.util.Log;

public class CameraManager {

    private Camera mCamera;
    private CameraPreview mPreview;

    public CameraManager(Context c) {
        if (checkCameraHardware(c)) {

            // Create an instance of Camera
            mCamera = getCameraInstance();

            // Create our Preview view and set it as the content of our activity.
            mPreview = new CameraPreview(c, mCamera);
        }
    }

    /**
     * Camera utils. Moving it nicely.
     **/

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Log.d("TAG", "Tiene camara!");
            return true;
        } else {
            Log.d("TAG", "No tiene camara!");
            return false;
        }
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(0); // attempt to get a Camera instance
        } catch (Exception e) {
            Log.d("TAG", "getCameraInstance: Not Working - " + e);
        }
        return c; // returns null if camera is unavailable
    }

    public CameraPreview getPreview() {
        return mPreview;
    }

    public void stop() {
        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        mCamera.release();
    }
}
