package es.upv.alphacodeteam.neoscanner.viewmodel;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class UriViewModel extends ViewModel {

    private static MutableLiveData<Uri> uriLiveData;

    public static LiveData<Uri> getUri() {
        checkUriInstance();
        return uriLiveData;
    }

    public static void setUri(Uri uri) {
        checkUriInstance();
        uriLiveData.setValue(uri);
    }

    private static void checkUriInstance() {
        if (uriLiveData == null) {
            uriLiveData = new MutableLiveData<>();
        }
    }
}
