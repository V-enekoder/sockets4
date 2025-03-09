package com.example.sockets4;

import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.widget.EditText;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

import android.Manifest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    EditText e1, e2;

    private TextView AddressText;
    private Button LocationButton;
    private LocationRequest locationRequest;
    private MyServer myServer;

    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        e1 = findViewById(R.id.etDireccionIP);
        e2 = findViewById(R.id.etData);

        AddressText = findViewById(R.id.addressText);
        //LocationButton = findViewById(R.id.locationButton);
/*
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);

        LocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED){
                        if(isGPSEnabled()){
                            LocationServices.getFusedLocationProviderClient(MainActivity.this)
                                    .requestLocationUpdates(locationRequest, new LocationCallback() {
                                        @Override
                                        public void onLocationResult(@NonNull LocationResult locationResult) {
                                            super.onLocationResult(locationResult);

                                            LocationServices.getFusedLocationProviderClient(MainActivity.this)
                                                    .removeLocationUpdates(this);

                                            if (locationResult != null && locationResult.getLocations().size() > 0){
                                                int index = locationResult.getLocations().size() - 1;
                                                double latitude = locationResult.getLocations().get(index).getLatitude();
                                                double longitude = locationResult.getLocations().get(index).getLongitude();

                                                AddressText.setText("Latitude: "+ latitude + "\n" + "Longitude: "+ longitude);
                                            }
                                        }
                                    }, Looper.getMainLooper());
                        }else{
                            turnOnGPS();
                        }
                    }
                    else{
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    }
                }
            }
        });
*/
        myServer = new MyServer(this);

        new Thread(myServer).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                final String publicIP = getPublicIP();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        AddressText.setText("IP pública del servidor: " + publicIP);
                    }
                });
            }
        }).start();
    }

    private void turnOnGPS() {

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(getApplicationContext())
                .checkLocationSettings(builder.build());

        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {

                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    Toast.makeText(MainActivity.this, "GPS is already tured on", Toast.LENGTH_SHORT).show();

                } catch (ApiException e) {

                    switch (e.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:

                            try {
                                ResolvableApiException resolvableApiException = (ResolvableApiException)e;
                                resolvableApiException.startResolutionForResult(MainActivity.this,2);
                            } catch (IntentSender.SendIntentException ex) {
                                ex.printStackTrace();
                            }
                            break;

                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            //Device does not have location
                            break;
                    }
                }
            }
        });
    }

    private boolean isGPSEnabled() {
        LocationManager locationManager = null;
        boolean isEnabled = false;

        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }

        isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return isEnabled;

    }

    private String getPublicIP() {
        String ip = null;
        try {
            URL url = new URL("https://api.ipify.org");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            ip = response.toString();  // La IP pública obtenida
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ip;
    }

    class MyServer implements Runnable {
        ServerSocket ss;
        Socket mySocket;
        DataInputStream dis;
        DataOutputStream dos;
        Handler handler = new Handler();
        private Context context;

        private LocationRequest locationRequest;

        private String lastKnownLocation = "Ubicación no disponible";

        public MyServer(Context context) {
            this.context = context;
            locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(5000);
            locationRequest.setFastestInterval(2000);

            updateLocation();
        }

        @Override
        public void run() {
            try {
                ss = new ServerSocket(9700);
                while (true) {
                    mySocket = ss.accept();
                    dis = new DataInputStream(mySocket.getInputStream());
                    dos = new DataOutputStream(mySocket.getOutputStream());

                    String message = dis.readUTF();

                    // Mostrar en el TextView
                    handler.post(() -> ((MainActivity) context).AddressText.setText("Solicitud recibida"));

                    dos.writeUTF("El servidor se encuentra en: " + lastKnownLocation);

                    dis.close();
                    dos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void updateLocation() {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                LocationServices.getFusedLocationProviderClient(context)
                        .requestLocationUpdates(locationRequest, new LocationCallback() {
                            @Override
                            public void onLocationResult(@NonNull LocationResult locationResult) {
                                super.onLocationResult(locationResult);

                                LocationServices.getFusedLocationProviderClient(context)
                                        .removeLocationUpdates(this);

                                if (locationResult != null && locationResult.getLocations().size() > 0) {
                                    int index = locationResult.getLocations().size() - 1;
                                    double latitude = locationResult.getLocations().get(index).getLatitude();
                                    double longitude = locationResult.getLocations().get(index).getLongitude();

                                    lastKnownLocation = "Lat: " + latitude + ", Lng: " + longitude;
                                }
                            }
                        }, Looper.getMainLooper());
            }
        }
    }

    public void onClickSend(View v) {
        BackgroundTask b = new BackgroundTask(this);
        b.execute(e1.getText().toString(), e2.getText().toString());
    }

    class BackgroundTask extends AsyncTask<String, Void, String> {
        Socket s;
        DataOutputStream dos;
        DataInputStream dis;
        String ip, message;
        Handler handler = new Handler();

        private Context context;
        public BackgroundTask(Context context) {
            this.context = context;
        }
        @Override
        protected String doInBackground(String... params) {
            ip = params[0];
            message = params[1];

            try {
                s = new Socket(ip, 9700);
                dos = new DataOutputStream(s.getOutputStream());
                dis = new DataInputStream(s.getInputStream());

                dos.writeUTF(message);
                dos.flush();  // Asegurar que el mensaje se envía antes de esperar respuesta

                // Esperar respuesta del servidor
                String serverResponse = dis.readUTF();
                handler.post(() -> ((MainActivity) context).AddressText.setText(serverResponse));

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (dis != null) dis.close();
                    if (dos != null) dos.close();
                    if (s != null) s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }
    }
}