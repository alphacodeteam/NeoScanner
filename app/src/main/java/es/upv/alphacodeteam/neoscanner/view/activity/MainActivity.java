package es.upv.alphacodeteam.neoscanner.view.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import es.upv.alphacodeteam.neoscanner.R;
import es.upv.alphacodeteam.neoscanner.view.util.Image;
import es.upv.alphacodeteam.neoscanner.view.util.QuadrilateralSelectionImageView;
import es.upv.alphacodeteam.neoscanner.view.util.Selectable;
import org.opencv.android.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import static org.opencv.core.CvType.CV_32SC1;
import static org.opencv.core.CvType.CV_8U;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.RETR_TREE;


public class MainActivity extends AppCompatActivity implements Selectable {

    private int originActionImage;
    private final int CAMERA_PERMISSION_CODE = 90;
    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSelectionImageView = (QuadrilateralSelectionImageView) findViewById(R.id.polygonView);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> this.selectAcquisitionType());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    // ******************************************************************************************************************************************************************************* //
    // ******************************************************************************** IMAGE ACQUISITION **************************************************************************** //

    /**
     * Selección del modo de adquisición de imagen (FLOATING ACTION BUTTON)
     */
    @Override
    public void selectAcquisitionType() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.acquisition_type);
        builder.setItems(R.array.acquisition_type, (dialog, position) -> {
            switch (position) {
                case 0:
                    // AQUÍ ABRIR CÁMARA, UNA VEZ OBTENIDA LA FOTO, SE ABRIRÁ EN LA NUEVA ACTIVITY (ImageActivity) para su procesado
                    //Intent i = new Intent(this, CameraActivity.class);
                    //startActivity(i);
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        // You can use the API that requires the permission.
                        Image.startModeOfCamera(this);
                    } else {
                        Toast.makeText(this, "!", Toast.LENGTH_SHORT).show();
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                    }
                    break;
                case 1:
                    // AQUÍ ABRIR EXPLORADOR DE ARCHIVOS (galeria), UNA VEZ SELECCIONADA LA FOTO, SE ABRIRÁ EN LA NUEVA ACTIVITY (ImageActivity) para su procesado
                    Image.startModeOfGallery(this);
                    break;
                default:
            }
        });
        builder.show();
    }

    QuadrilateralSelectionImageView mSelectionImageView;

    /**
     * Resolución del activity
     * El intent de obtener una fotografia desde la cámara, devuelve un bitmap, metodos mas sencillos, mejor eficiencia en el ahorro del codigo, mismo funcionamiento.
     * TODO: El contenido de ImageCaputer se moverá a la actividad de procesado de imagen.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uriSelectedImage = null;
        Bitmap imageCaptured = null;

        if(requestCode == Image.GALLERY_INTENT_CODE && resultCode == RESULT_OK)
            uriSelectedImage = data.getData();

        if (requestCode == Image.CAMERA_INTENT_CODE)
            imageCaptured = (Bitmap) data.getExtras().get("data");

            if (uriSelectedImage != null) {
                // Guardamos el tipo de origen (Cámara o Galeria)
                this.originActionImage = requestCode;
                // TODO: Obtenemos la imagen desde uriSelectedImage
            } else if(imageCaptured != null){

                mBitmap = imageCaptured;
                mSelectionImageView.setImageBitmap(mBitmap);
                List<PointF> points = Image.findPoints(mBitmap);
                mSelectionImageView.setPoints(points);
               // calculateActivity(imageCaptured);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION_CODE:
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Image.startModeOfCamera(this);
                } else {
                    Toast.makeText(this, "NeoScanner needs camera's permission", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }

    /**
     * TODO: Estas lineas siguientes se moveran a la actividad de procesado de la imagen
     */

    Bitmap mBitmap;
    Bitmap mResult;

    private void calculateActivity(Bitmap original) {
        mBitmap = original;
        List<PointF> points = mSelectionImageView.getPoints();

        if (mBitmap != null) {
            Mat orig = new Mat();
            org.opencv.android.Utils.bitmapToMat(mBitmap, orig);

            Mat transformed = Image.perspectiveTransform(orig, points);
            mResult = Image.applyThreshold(transformed);

            ((ImageView) findViewById(R.id.imageView)).setImageBitmap(mResult);

            orig.release();
            transformed.release();
        }
    }

}