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
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.CompletableFuture;

import android.Manifest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    EditText e1;

    private TextView AddressText;
    private MyServer myServer;

    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        e1 = findViewById(R.id.etDireccionIP);

        AddressText = findViewById(R.id.addressText);
        myServer = new MyServer(this);

        new Thread(myServer).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                final String localIP = getLocalIpAddress(); // Cambio aquí
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        e1.setText(localIP);
                    }
                });
            }
        }).start();
    }
private String getLocalIpAddress() {
    try {
        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
            NetworkInterface intf = en.nextElement();
            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                InetAddress inetAddress = enumIpAddr.nextElement();
                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                    return inetAddress.getHostAddress();
                }
            }
        }
    } catch (SocketException ex) {
        Log.e("IP Address", ex.toString());
    }
    return null;
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

                    //String message = dis.readUTF();

                    // Mostrar en el TextView
                    handler.post(() -> ((MainActivity) context).AddressText.setText("Usuario Conectado"));

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
        b.execute(e1.getText().toString());
    }

    class BackgroundTask extends AsyncTask<String, Void, String> {
        Socket s;
        DataOutputStream dos;
        DataInputStream dis;
        String ip;
        Handler handler = new Handler();

        private Context context;
        public BackgroundTask(Context context) {
            this.context = context;
        }
        @Override
        protected String doInBackground(String... params) {
            ip = params[0];

            try {
                s = new Socket(ip, 9700);
                dos = new DataOutputStream(s.getOutputStream());
                dis = new DataInputStream(s.getInputStream());

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