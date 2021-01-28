package es.upv.alphacodeteam.neoscanner.view.util;

import android.content.Context;
import android.widget.Toast;

public class MyToast {

    public static void showShortMessage(CharSequence message, Context context) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void showLongMessage(CharSequence message, Context context) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

}

