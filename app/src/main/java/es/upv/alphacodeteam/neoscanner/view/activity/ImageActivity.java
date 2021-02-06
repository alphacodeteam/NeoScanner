package es.upv.alphacodeteam.neoscanner.view.activity;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import es.upv.alphacodeteam.neoscanner.R;
import es.upv.alphacodeteam.neoscanner.model.repository.ResLocalRepo;
import es.upv.alphacodeteam.neoscanner.view.util.Image;
import es.upv.alphacodeteam.neoscanner.view.util.MyToast;
import es.upv.alphacodeteam.neoscanner.view.util.QuadrilateralSelectionImageView;

public class ImageActivity extends AppCompatActivity {

    private static final String TAG = "ImageActivity";
    private QuadrilateralSelectionImageView iv_image;

    private int mOriginReceived;
    // From Camera
    private Bitmap mBitmapReceived;
    // From Gallery
    private Uri mUriReceived;

    //Carga el OpenCV
    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Recuperamos los datos recibidos de MainActivity
        mOriginReceived = (int) getIntent().getExtras().get("origin");
        if (mOriginReceived == Image.CAMERA_INTENT_CODE) {
            // TODO: GUARDAR BITMAP RECIBIDO
            mBitmapReceived = (Bitmap) getIntent().getExtras().get("bitmap");
        } else if (mOriginReceived == Image.GALLERY_INTENT_CODE) {
            mUriReceived = (Uri) getIntent().getExtras().get("uri");
        }
        // Seteamos la vista
        setContentView(R.layout.activity_image);
        // Configuramos la vista
        configView();

    }

    /**
     * Configuración de vista
     */
    private void configView() {
        // Vinculamos los componentes a la vista
        this.iv_image = findViewById(R.id.imageView);
        // TODO: ME FALTA VINCULAR DEMÁS BOTONES

        // Aplicamos la imagen
        fillImageIntoImageView();
    }

    /**
     * Aplica en el imageView la imagen obtenida en el MainActivity
     */
    public void fillImageIntoImageView() {
        int randNum = 0;
        try {
            switch (mOriginReceived) {
                case Image.GALLERY_INTENT_CODE:
                    // Ubicación de la foto tomada desde galeria
                    InputStream originPath = this.getContentResolver().openInputStream(mUriReceived);
                    // Generate rand number
                    int min = 0, max = 10000;
                    randNum = ThreadLocalRandom.current().nextInt(min, max + 1);
                    Log.d("Random Num--> ", "" + randNum);
                    // Path destino
                    File targetFile = ResLocalRepo.createLocalFile(randNum, this);
                    Path targetPath = Paths.get(targetFile.getPath());
                    // Copiamos el fichero resultante en nuestra carpeta local
                    Files.copy(originPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    // Comprimimos la imagen local //TODO: Esto habrá que cambiarlo y obtener la foto sin compresión
                    Bitmap bitmapCompressed = Image.compressBitmap(75, 75, 40, randNum, this);
                    // Por último, la añadimos al image view
                    Mat temp = new Mat();
                    Utils.bitmapToMat(bitmapCompressed,temp);
                    //iv_image.setPoints(Image.findPoints(bitmapCompressed));
                    iv_image.setImageBitmap(Image.getResizedBitmap(bitmapCompressed,800));
                    calculateActivity2(bitmapCompressed);
                    break;
            }
        } catch (Exception ex) {
            MyToast.showLongMessage(this.getResources().getString(R.string.title_error_occurred), this);
            Log.e("IMAGEACTIVITY", "fillImageIntoImageView: " + ex.getMessage());
        }
    }

    /**
     *
     */
    private void calculateActivity() {
        List<PointF> points = iv_image.getPoints();

        if (mBitmapReceived != null) {
            Mat orig = new Mat();
            org.opencv.android.Utils.bitmapToMat(mBitmapReceived, orig);

            Mat transformed = Image.perspectiveTransform(orig, points);

            Bitmap mResult = Image.applyThreshold(transformed);
            iv_image.setImageBitmap(mResult);

            orig.release();
            transformed.release();
        }
    }

    /**
     *
     */
    private void calculateActivity2(Bitmap bitmap) {
        if (bitmap != null) {
            Log.d("TAG", "tagatag");
            Mat orig = new Mat();
            org.opencv.android.Utils.bitmapToMat(bitmap, orig);
            Log.d("TAG", "tagataag");
            iv_image.setImageBitmap(bitmap);
            iv_image.setPoints(Image.testGlobal(orig));

            Log.d(TAG, "calculateActivity2: h - " + bitmap.getHeight() + "; w - " + bitmap.getWidth());

            //iv_image.setPoints(Image.testGlobal(orig));

            orig.release();
        }
    }

}
