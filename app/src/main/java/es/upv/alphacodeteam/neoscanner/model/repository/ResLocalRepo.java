package es.upv.alphacodeteam.neoscanner.model.repository;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import java.io.File;

import es.upv.alphacodeteam.neoscanner.viewmodel.UriViewModel;

public class ResLocalRepo {

    /**
     * Descarga la imagen local, si no existe, devuelve null
     */
    public static Bitmap getLocalProfileImage(final String id, final Activity activity) throws Exception {
        // Obtenemos el fichero local
        File file = getLocalFile(id, activity);
        if (file != null) {
            // Generamos la uri
            Uri uri = getUriForFile(file, activity);
            // Salvamos los datos con nuestro UriViewModel
            UriViewModel.setUri(uri);
            // Generamos el bitmap
            Bitmap bitmap = getBitmapFromUri(uri, activity);
            return bitmap;
        }
        return null;
    }

    /**
     * Creación de fichero
     */
    public static File createLocalFile(final String id, final Activity activity) {
        // String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()); // Por si queremos añadirle un timeStamp al nombre del fichero
        // Ruta del fichero
        File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File file = new File(storageDir + File.separator + id + ".jpg");
        return file;
    }


    /**
     * Si existe, devuelve el fichero local, sino, devuelve null
     */
    public static File getLocalFile(final String id, final Activity activity) {
        File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String targetPath = storageDir.getAbsolutePath() + File.separator + id + ".jpg";
        File file = new File(targetPath);
        if (file.exists() && file.isFile()) {
            return file;
        }
        return null;
    }

    /**
     * Borrado del fichero especificado
     */
    public static boolean deleteLocalFile(final String id, final Activity activity) {
        // Si ya existe el fichero target, lo borramos (para subida de fotos fallidas)
        File file = getLocalFile(id, activity);
        if (file != null) {
            file.delete();
            return true;
        }
        return false;
    }

    /**
     * Renombra un fichero local
     */
    public static void renameLocalFile(final File file, final String id, final Activity activity) {
        File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String strModifiedPath = storageDir.getAbsolutePath() + File.separator + id + ".jpg";
        // Renombramos el fichero con el path modificado (eliminamos el valor random)
        file.renameTo(new File(strModifiedPath));
    }

    /**
     * Creación de URI para el fichero
     */
    public static Uri getUriForFile(final File file, final Activity activity) {
        return FileProvider.getUriForFile(activity.getApplicationContext(), "es.upv.daghdha.furrycare.fileprovider", file);
    }

    /**
     * Creación de BITMAP desde URI
     */
    public static Bitmap getBitmapFromUri(final Uri uri, final Activity activity) throws Exception {
        Bitmap bitmap;
        if (Build.VERSION.SDK_INT < 28) {
            bitmap = MediaStore.Images.Media.getBitmap(activity.getApplicationContext().getContentResolver(), uri);
        } else {
            bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(activity.getApplicationContext().getContentResolver(), uri));
        }
        return bitmap;
    }

    /**
     * Creación de BITMAP desde FILE
     */
    public static Bitmap getBitmapFromFile(final File file) {
        // Creamos un bitmap a partir de un fichero temporal
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        return bitmap;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creación de fichero temporal (NO USAR)
     */
    public static File createLocalTempFile(final int id, final Activity activity) throws Exception {
        // String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()); // Por si queremos añadirle un timeStamp al nombre del fichero
        // Si ya existe, eliminamos el fichero
        // deleteLocalFile(idEntity, activity);
        // Ruta del fichero
        File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // Generamos el fichero (prefijo, sufijo, path) donde se añade automáticamente un valor random al final (ej->3010399218384) que posteriormente se eliminará
        File file = File.createTempFile("" + id, ".jpg", storageDir);
        return file;
    }

}

