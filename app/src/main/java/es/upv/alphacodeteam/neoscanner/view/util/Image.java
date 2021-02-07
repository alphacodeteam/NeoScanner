package es.upv.alphacodeteam.neoscanner.view.util;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.FileProvider;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import java.io.FileOutputStream;

import es.upv.alphacodeteam.neoscanner.BuildConfig;
import es.upv.alphacodeteam.neoscanner.model.repository.ResLocalRepo;

import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.contourArea;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.drawContours;
import static org.opencv.imgproc.Imgproc.findContours;
import static org.opencv.imgproc.Imgproc.rectangle;
import static org.opencv.imgproc.Imgproc.threshold;

public class Image {

    public static final int CAMERA_INTENT_CODE = 0, GALLERY_INTENT_CODE = 1, RESULT_OK = -1;
    private static final int MAX_HEIGHT = 500;
    private static final String TAG = "Image";

    /**
     * Inicio del modo de Galería
     *
     * @param activity
     */
    public static void startModeOfGallery(final Activity activity) {
        // Generamos el Intent
        Intent intent = new Intent();
        // Cualquier tipo de imagen
        intent.setType("image/*");
        // Acción de sistema de archivos
        intent.setAction(Intent.ACTION_GET_CONTENT);
        activity.startActivityForResult(intent, GALLERY_INTENT_CODE);
    }

    /**
     * Inicio del modo de Camara
     *
     * @param activity
     */
    public static void startModeOfCamera(final Activity activity, File dir) {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        Uri photoURI = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID +".provider", dir);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        activity.startActivityForResult(cameraIntent, CAMERA_INTENT_CODE);
    }

}
