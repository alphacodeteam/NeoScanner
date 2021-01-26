package es.upv.alphacodeteam.neoscanner.view.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;

import es.upv.alphacodeteam.neoscanner.R;
import es.upv.alphacodeteam.neoscanner.view.util.Image;
import es.upv.alphacodeteam.neoscanner.view.util.Selectable;

public class MainActivity extends AppCompatActivity implements Selectable {

    private int originActionImage;

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
                    Intent i = new Intent(this, CameraActivity.class);
                    startActivity(i);
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
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uriSelectedImage = null;
        if (resultCode == Image.RESULT_OK) {
            switch (requestCode) {
                case Image.CAMERA_INTENT_CODE:
                    // TODO: Obtenemos la uri guardada del viewmodel para obtener la imagen capturada
                    break;
                case Image.GALLERY_INTENT_CODE:
                    uriSelectedImage = data.getData();
                    break;
            }
            if (uriSelectedImage != null) {
                // Guardamos el tipo de origen (Cámara o Galeria)
                this.originActionImage = requestCode;
                // TODO: Obtenemos la imagen desde uriSelectedImage
            }
        }
    }
}