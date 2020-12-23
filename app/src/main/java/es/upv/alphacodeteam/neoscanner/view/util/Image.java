package es.upv.alphacodeteam.neoscanner.view.util;

import android.app.Activity;
import android.content.Intent;

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

}
