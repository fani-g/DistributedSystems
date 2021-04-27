package omada6.katanemimena.katanemimenaapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;


public class CustomInfoWindow implements GoogleMap.InfoWindowAdapter {
    private static final String TAG = "CustomInfoWindow";
    private final View window;
    private Context context;
    private String photo;
    private ImageView img;
    public CustomInfoWindow(Context context,String photo) {
        this.context = context;
        this.photo = photo;
        window = LayoutInflater.from(context).inflate(R.layout.custom_makrer_info,null);
    }

    private void renderWindow(Marker marker,final View view){
        String title = marker.getTitle();
        TextView tv_Title = (TextView)view.findViewById(R.id.marker_title);

        if (!title.equals("")){
            tv_Title.setText(title);
        }
        String snippet = marker.getSnippet();
        TextView tv_Snippet = (TextView)view.findViewById(R.id.marker_snippet);

        if (!snippet.equals("")){
            tv_Snippet.setText(snippet);
        }
        img = (ImageView)view.findViewById(R.id.marker_image);
        if (photo.length() == 0) {
            img.setVisibility(View.GONE);
        }
        else {
            Picasso.get().load(photo)
                    .into(img, new MarkerCallback(marker));

        }
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        renderWindow(marker,window);
        return window;
    }

    static class MarkerCallback implements Callback {
        Marker marker=null;

        MarkerCallback(Marker marker) {
            this.marker=marker;
        }

        @Override
        public void onSuccess() {
            if (marker != null && marker.isInfoWindowShown()) {
                marker.showInfoWindow();
            }
        }

        @Override
        public void onError(Exception e) {
            Log.e(getClass().getSimpleName(), "Error loading thumbnail!");
        }
    }
}
