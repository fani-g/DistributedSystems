package omada6.katanemimena.katanemimenaapp;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements LocationListener{
    private static final String TAG = "MainActivity";
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION,
                                COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final String IP_ADDRESS = "192.168.1.5";//"192.168.43.25"; //"172.16.47.6";
    private static final int c_port = 4001;
    private Button btnSearch;
    private EditText etCategory, etDistance, etTopK, etUserID;
    private Spinner spLocation;
    private static Socket socket;
    private static ObjectInputStream in;
    private static ObjectOutputStream out;
    private static String category;
    private static int top_k, radius, userID, totalUsers;
    private static Intent myIntent;
    private static boolean userFlag, categoryFlag, top_kFlag;
    private static Location currentLocation;
    private static double  latitude , longitude ;
    private Boolean permissionsGranted = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myIntent = new Intent(MainActivity.this, MapsActivity.class);
        btnSearch = (Button)findViewById(R.id.bt_search);
        etCategory = (EditText)findViewById(R.id.et_category);
        etDistance = (EditText)findViewById(R.id.et_distance);
        etTopK = (EditText)findViewById(R.id.et_topk);
        etUserID = (EditText)findViewById(R.id.et_user);
        spLocation = (Spinner)findViewById(R.id.location_spinner);

        getLocationPermission();
        if (permissionsGranted)
            getDeviceLocation();


        Log.d(TAG, "onCreate: what is my location?");

        spLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: {
                        if (currentLocation != null) {
                            latitude = currentLocation.getLatitude();
                            longitude = currentLocation.getLongitude();
                        } else
                            longitude = latitude = 0;
                        Log.d(TAG, "onCreate: location : (" + latitude + ", " + longitude + ")");
                        break;
                    }
                    case 1: {
                        // Athens
                        latitude = 37.9838096;
                        longitude = 23.727538800000048;
                        Log.d(TAG, "onCreate: location : (" + latitude + ", " + longitude + ")");
                        break;
                    }
                    case 2: {
                        //New York
                        latitude = 40.741895;
                        longitude = -73.989308;
                        Log.d(TAG, "onCreate: location : (" + latitude + ", " + longitude + ")");
                        break;
                    }
                    case 3: {
                        //Rome
                        latitude = 41.90270080000001;
                        longitude = 12.496235200000001;
                        Log.d(TAG, "onCreate: location : (" + latitude + ", " + longitude + ")");
                        break;
                    }
                    case 4:{

                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("Title");
                        // I'm using fragment here so I'm using getView() to provide ViewGroup
                        // but you can provide here any other instance of ViewGroup from your Fragment / Activity
                        View viewInflated = LayoutInflater.from(MainActivity.this).inflate(R.layout.input_latlng, (ViewGroup) findViewById(android.R.id.content), false);
                        // Set up the input
                        final EditText lat = (EditText) viewInflated.findViewById(R.id.et_latitude);
                        final EditText lng = (EditText) viewInflated.findViewById(R.id.et_longitude);
                        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                        builder.setView(viewInflated);

                        // Set up the buttons
                        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                latitude = Double.parseDouble(lat.getText().toString());
                                longitude = Double.parseDouble(lng.getText().toString());
                            }
                        });
                        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        builder.show();
                        Log.d(TAG, "onCreate: location : (" + latitude + ", " + longitude + ")");
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (currentLocation!=null) {
                    latitude = currentLocation.getLatitude();
                    longitude = currentLocation.getLongitude();
                }else{
                    if (permissionsGranted) {
                        getDeviceLocation();
                        if (currentLocation!=null) {
                            latitude = currentLocation.getLatitude();
                            longitude = currentLocation.getLongitude();
                        }
                    }
                }
                    longitude = latitude =0;
                Log.d(TAG, "onCreate: location : (" +latitude +", "+ longitude+ ") i am dead ");
            }
        });

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean user, topk ;
                user = topk = true;
                final int a = !etDistance.getText().toString().equals("")?Integer.parseInt(etDistance.getText().toString()) : 0;
                if (a>0)
                    radius = Integer.parseInt(etDistance.getText().toString());
                else
                    radius = 5;

                if (isEmpty(etCategory)) {
                    category = "";
                }else
                    category = etCategory.getText().toString();

                final int b = !etTopK.getText().toString().equals("")?Integer.parseInt(etTopK.getText().toString()) : -1;
                if (b<=0){
                    topk = false;
                    etTopK.setError("Cannot be empty");
                }else{
                    top_k = Integer.parseInt(etTopK.getText().toString());
                }

                final int c = !etUserID.getText().toString().equals("")?Integer.parseInt(etUserID.getText().toString()) : -1;
                if (c<0){
                    user = false;
                    etUserID.setError("Cannot be empty");
                }else{
                    userID = Integer.parseInt(etUserID.getText().toString());
                }
                if ( topk && user) {
                    myIntent.putExtra("radius", radius);
                    sendMessage(v);
                }

                myIntent.putExtra("latitude",latitude);
                myIntent.putExtra("longitude",longitude);
            }
        });
    }
    // TODO: 28/5/2018
    // CHECK IF LOCATION SERVICE IS ENABLED
    private void getDeviceLocation(){
        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try{
            if (permissionsGranted) {
                LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                    final Task location = fusedLocationProviderClient.getLastLocation();
                    location.addOnCompleteListener(new OnCompleteListener() {
                        @Override
                        public void onComplete(@NonNull Task task) {
                            if (location.isSuccessful()) {
                                Log.d(TAG, "onComplete: Found location");
                                currentLocation = (Location) task.getResult();
                                latitude = currentLocation.getLatitude();
                                longitude = currentLocation.getLongitude();
                                Log.d(TAG, "onComplete: Location is: " + latitude + ", " + longitude);

                            } else {
                                Log.d(TAG, "onComplete: Current location is null");
                                Toast.makeText(MainActivity.this, "Unable to get your location", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });
                }else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("GPS not found");  // GPS not found
                    builder.setMessage("You must enable GPS in order to progress"); // Want to enable?
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    });
                    builder.setNegativeButton("Cancel", null);
                    builder.create().show();
                    return;
                }
            }else
                getLocationPermission();
        }catch (SecurityException e){
            Log.d(TAG, "getDeviceLocation: Security Exception" + e.getMessage());
        }
    }

    private void getLocationPermission() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                permissionsGranted= true;
            }else
                ActivityCompat.requestPermissions(this,permissions,MY_PERMISSIONS_REQUEST_LOCATION);
        }else
            ActivityCompat.requestPermissions(this,permissions,MY_PERMISSIONS_REQUEST_LOCATION);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        permissionsGranted =false;
        switch (requestCode){
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0){
                    for (int grantResult : grantResults) {
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            permissionsGranted = false;
                            return;
                        }
                    }
                    permissionsGranted = true;
                }
            }
        }
    }
    @Override
    protected void onPause(){
        super.onPause();
    }
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude","STATUS");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude","ENABLED");
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude","DISABLED");
    }

    public void sendMessage(View v){
        requestHandler request = new requestHandler();
        request.execute(category,String.valueOf(top_k),String.valueOf(userID));
    }
    class requestHandler extends AsyncTask<String,Void,Void>{

        @Override
        protected Void doInBackground(String... params) {
            try {
                //connect to server
                socket = new Socket(IP_ADDRESS,c_port);
                String categ = params[0];
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                //error flags
                userFlag = categoryFlag = false;

                //check if the userID is valid
                totalUsers = in.readInt();
                int user = Integer.parseInt(params[2]);
                out.writeInt(user);
                out.flush();
                int top = Integer.parseInt(params[1]);
                int res = (user > totalUsers ? -1 : 0);
                if (res == 0) {

                    if (categ.equals("")){
                        out.writeUTF("ALL");
                        out.flush();
                    }else {
                        out.writeUTF(categ);
                        out.flush();
                    }

                    out.writeDouble(latitude);
                    out.flush();
                    out.writeDouble(longitude);
                    out.flush();
                    out.writeInt(radius);
                    out.flush();
                    out.writeInt(top);
                    out.flush();
                    if (categ.length()!=0)
                        res = in.readInt();
                    else
                        res = 0;

                    if (res == 0) {

                        top = in.readInt();

                        if (top_k>0) {
                            POI p;

                            myIntent.putExtra("top_k", top);

                            for (int i = 0; i < top; i++) {
                                int id = in.readInt();
                                String name = in.readUTF();
                                double lat = in.readDouble();
                                double lon = in.readDouble();
                                String photos = in.readUTF();
                                String category = in.readUTF();

                                p = new POI(id, lat, lon, photos, name, category);
                                myIntent.putExtra("poi" + i, p);
                            }
                            startActivityForResult(myIntent, 0);
                        }else{
                            top_kFlag = true;
                        }
                    }else {
                        categoryFlag = true;
                    }
                }else {
                    userFlag = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (userFlag){
                etUserID.setError("UserID must be in range of [0, "+ totalUsers+ "].");
                Toast.makeText(MainActivity.this,"User ID not found",Toast.LENGTH_LONG).show();
            }else if(categoryFlag){
                etCategory.setError("Category not in database.");
                Toast.makeText(MainActivity.this,"Could not find such category",Toast.LENGTH_LONG).show();
            }else if(top_kFlag){
                Toast.makeText(MainActivity.this,"Could not find any POIs", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean isEmpty(EditText et){
        return et.getText().toString().trim().length() == 0;
    }

}
