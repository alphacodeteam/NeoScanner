package es.upv.alphacodeteam.neoscanner.view.util;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;

import es.upv.alphacodeteam.neoscanner.model.repository.ResLocalRepo;

public class Image {

    public static final int CAMERA_INTENT_CODE = 0, GALLERY_INTENT_CODE = 1, RESULT_OK = -1;

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
     * Compresión del fichero indicado
     *
     * @param width
     * @param height
     * @param quality
     * @param id
     * @param activity
     * @return bitmap
     * @throws Exception
     */
    public static Bitmap compressBitmap(int width, int height, int quality, int id, Activity activity) throws Exception {
        File file = ResLocalRepo.getLocalFile(id, activity);
        Bitmap bitmap = ResLocalRepo.getBitmapFromFile(file);
        FileOutputStream out = new FileOutputStream(file);
        // Dimensiones y filtrado bilineal
        Bitmap.createScaledBitmap(bitmap, width, height, true);
        // Calidad de imagen
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
        out.close();
        return bitmap;
    }

}
