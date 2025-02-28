package com.example.sockets4;

import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.sockets4.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    EditText e1,e2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        e1 = (EditText) findViewById(R.id.etDireccionIP);
        e2 = (EditText) findViewById(R.id.etData);

        Thread myThread = new Thread(new MyServer());
        myThread.start();
    }


    class MyServer implements Runnable{
        ServerSocket ss;
        Socket mySocket;
        DataInputStream dis;

        String message;
        Handler handler = new Handler();


        @Override
        public void run() {
            try {
                ss = new ServerSocket(9700);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"Waiting for Clients",Toast.LENGTH_SHORT).show();
                    }
                });
                while(true){
                    mySocket = ss.accept();
                    dis = new DataInputStream(mySocket.getInputStream());
                    message = dis.readUTF();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Message received from client" + message,Toast.LENGTH_SHORT).show();
                        }
                    });


                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public void onClickSend(View v){
        BackgroundTask b = new BackgroundTask();
        b.execute(e1.getText().toString(),e2.getText().toString());
    }

    class BackgroundTask extends AsyncTask<String,Void,String>{
        Socket s;
        DataOutputStream dos;
        String ip, message;
        @Override
        protected String doInBackground(String... params) {
            ip = params[0];
            message = params[1];

            try {
                s = new Socket(ip,9700);
                dos = new DataOutputStream(s.getOutputStream());
                dos.writeUTF(message);

                dos.close();

                s.close();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return null;
        }
    }

}