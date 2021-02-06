package es.upv.alphacodeteam.neoscanner.view.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;

import es.upv.alphacodeteam.neoscanner.R;
import es.upv.alphacodeteam.neoscanner.view.util.Image;
import es.upv.alphacodeteam.neoscanner.view.util.Selectable;


public class MainActivity extends AppCompatActivity implements Selectable {

    private final int CAMERA_PERMISSION_CODE = 90;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

    File dir= null;
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

                        try {
                            dir = File.createTempFile("compressed-", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Image.startModeOfCamera(this, dir);
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

    /**
     * Resolución del activity
     * El intent de obtener una fotografia desde la cámara, devuelve un bitmap, metodos mas sencillos, mejor eficiencia en el ahorro del codigo, mismo funcionamiento.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Image.RESULT_OK) {
            Uri imageSelected = null;
            int origin = -1;
            switch (requestCode) {
                case Image.CAMERA_INTENT_CODE:
                    imageSelected = FileProvider.getUriForFile(this,
                            "com.example.android.fileprovider",
                            dir);
                    origin = Image.GALLERY_INTENT_CODE;
                    break;
                case Image.GALLERY_INTENT_CODE:
                    imageSelected = data.getData();
                    origin = Image.GALLERY_INTENT_CODE;
                    break;
            }
            if (origin != -1) {
                Intent intent = new Intent(MainActivity.this, ImageActivity.class);
                intent.putExtra("origin", origin);
                if (imageSelected != null) {
                    // Llamamos al ImageActivity
                    intent.putExtra("uri", imageSelected);
                    startActivity(intent);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        dir = File.createTempFile("compressed-", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Image.startModeOfCamera(this, dir);
                } else {
                    Toast.makeText(this, "NeoScanner needs camera's permission", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }
}