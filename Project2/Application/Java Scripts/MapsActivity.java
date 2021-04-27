package omada6.katanemimena.katanemimenaapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.net.URL;
import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final String TAG = "MapsActivity";
    private GoogleMap mMap;
    private ArrayList<POI> pois = new ArrayList<>();
    private ArrayList<Marker> markers = new ArrayList<>();
    private MarkerOptions options = new MarkerOptions();
    private Marker mMarker;
    private static int radius = 5;
    private double longitude,latitude;
    private Bitmap bmp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        Bundle bundle = getIntent().getExtras();
        int top_k = bundle.getInt("top_k");
        radius = bundle.getInt("radius");
        for (int i = 0; i < top_k; i++) {
            POI p = (POI) getIntent().getSerializableExtra("poi" + i);
            pois.add(p);
        }
        latitude = bundle.getDouble("latitude");
        longitude = bundle.getDouble("longitude");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng myLocation = new LatLng(latitude,longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation,5f));

        mMap.animateCamera(CameraUpdateFactory.zoomTo(5f), 2000, null);
        mMap.addCircle(new CircleOptions()
                .center(myLocation)
                .radius(radius * 1000)
                .strokeColor(Color.RED)
                .fillColor(0x220000FF)
                .strokeWidth(1)
        );

        for (POI p : pois) {
            addNewMarker(p);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    private void addNewMarker(POI placeInfo){
        mMap.setInfoWindowAdapter(new CustomInfoWindow(MapsActivity.this,placeInfo.getPhotos()));

        try {
            String snippet = "ID: " + placeInfo.getID() + "\nCategory: "+ placeInfo.getCategory();
            mMarker = mMap.addMarker(new MarkerOptions()
                    .title(placeInfo.getPOI_name())
                    .position(new LatLng(placeInfo.getLatitude(),placeInfo.getLongitude()))
                    .snippet(snippet));
        }catch (NullPointerException e){
            Log.e(TAG, "moveCamera: NullPointerException thrown: " + e.getMessage() );
        }

    }

}
