package es.upv.alphacodeteam.neoscanner.view.activity;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ThreadLocalRandom;

import es.upv.alphacodeteam.neoscanner.R;
import es.upv.alphacodeteam.neoscanner.model.repository.ResLocalRepo;
import es.upv.alphacodeteam.neoscanner.view.util.Image;
import es.upv.alphacodeteam.neoscanner.view.util.MyToast;

public class ImageActivity extends AppCompatActivity {

    private ImageView iv_image;

    private Uri uriReceived;
    private int originReceived;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Recuperamos la uri recibida
        this.uriReceived = (Uri) getIntent().getExtras().get("uri");
        Log.d("URI Received: ", uriReceived.toString());
        this.originReceived = (int) getIntent().getExtras().get("origin");
        Log.d("Origin Received: ", "" + originReceived);

        // Seteamos la vista
        setContentView(R.layout.activity_image);

        configView();
    }


    /**
     * Configuración de vista
     */
    private void configView() {
        // TODO: VINCULAR OTROS BOTONES
        // Vinculamos los componentes a la vista
        this.iv_image = findViewById(R.id.imageView);
        // Configuramos la vista
        fillImageIntoImageView(this.uriReceived);
    }

    /**
     * Aplica en el imageView la imagen obtenida en la página principal
     *
     * @param uri
     */
    public void fillImageIntoImageView(final Uri uri) {
        int randNum = 0;
        try {
            switch (originReceived) {
                case Image.CAMERA_INTENT_CODE:
                    break;
                case Image.GALLERY_INTENT_CODE:
                    // Ubicación de la foto tomada desde galeria
                    InputStream originPath = this.getContentResolver().openInputStream(uri);
                    // Generate rand number
                    int min = 0;
                    int max = 10000;
                    randNum = ThreadLocalRandom.current().nextInt(min, max + 1);
                    Log.d("Random Num--> ", "" + randNum);
                    // Path destino
                    File targetFile = ResLocalRepo.createLocalFile(randNum, this);
                    Path targetPath = Paths.get(targetFile.getPath());
                    // Copiamos el fichero resultante en nuestra carpeta local
                    Files.copy(originPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    break;
            }
            // Comprimimos la imagen local
            Bitmap bitmapCompressed = Image.compressBitmap(75, 75, 40, randNum, this);
            // Por último, la añadimos al image view
            iv_image.setImageBitmap(bitmapCompressed);
        } catch (Exception ex) {
            MyToast.showLongMessage(this.getResources().getString(R.string.title_error_occurred), this);
        }
    }

}
