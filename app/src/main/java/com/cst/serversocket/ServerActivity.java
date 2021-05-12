package com.cst.serversocket;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerActivity extends AppCompatActivity {

    private ServerSocket serverSocket;
    private Socket tempClientSocket;
    Thread serverThread = null;
    public static final int SERVER_PORT = 3004;
    
    // For the IP part, is setup on the ClientSocket App which
    // is this device IP itself (the one used this app).

    private LinearLayout msgList;
    private Handler handler;
    private int greenColor;
    private EditText edMessage;
    private Button btnSendPayment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        setTitle("Server");
        greenColor = ContextCompat.getColor(this, R.color.green);
        handler = new Handler();
        msgList = findViewById(R.id.msgList);
        edMessage = findViewById(R.id.edMessage);
        btnSendPayment = findViewById(R.id.send_payment);

        btnSendPayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //TO:DO Send payment request to IM30 & wait for it to receive & response


                Bundle extra = new Bundle();
                String pay_amount = "1000"; //set as the payment value later
                String pay_function = "01";
                String pay_type = "01";
                String pay_camera_mode = "01";
                String pay_print_receipt_id = "Y";

                //Pack Request Message
                extra.putString("pay_amount", pay_amount);
                extra.putString("pay_function", pay_function);
                extra.putString("pay_type", pay_type);
                extra.putString("pay_camera_mode", pay_camera_mode);
                extra.putString("pay_print_receipt_id", pay_print_receipt_id);

                //API to call Payment App
                Intent intent = new Intent("com.revenue.edc.hlb.pro.app2app");
                //Intent = putExtras(extra);   -->in the documentation
                intent = intent.putExtras(extra);
                startActivity(intent);

            }
        });
    }

    public TextView textView(String message, int color) {
        if (null == message || message.trim().isEmpty()) {
            message = "<Empty Message>";
        }
        TextView tv = new TextView(this);
        tv.setTextColor(color);
        tv.setText(message + " [" + getTime() +"]");
        tv.setTextSize(20);
        tv.setPadding(0, 5, 0, 0);
        return tv;
    }

    private String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }

    public void showMessage(final String message, final int color) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                msgList.addView(textView(message, color));
            }
        });
    }

    @SuppressLint("RestrictedApi")
    //@Override
    public void onClick(View view) {
        if (view.getId() == R.id.start_server) {
            msgList.removeAllViews();
            showMessage("Server Started.", Color.BLACK);
            this.serverThread = new Thread(new ServerThread());
            this.serverThread.start();
            return;
        }
        if (view.getId() == R.id.send_data) {
            String msg = edMessage.getText().toString().trim();
            showMessage("Server : " + msg, Color.BLUE);
            sendMessage(msg);
            openApp();
        }
        /*if (view.getId() == R.id.send_payment) {
            Bundle extra = new Bundle();
            String pay_amount = "1000";
            String pay_function = "01";
            String pay_type = "01";
            String pay_camera_mode = "01";
            String pay_print_receipt_id = "Y";

            //Pack Request Message
            extra.putString("pay_amount", pay_amount);
            extra.putString("pay_function", pay_function);
            extra.putString("pay_type", pay_type);
            extra.putString("pay_camera_mode", pay_camera_mode);
            extra.putString("pay_print_receipt_id", pay_print_receipt_id);

            //API to call Payment App
            Intent intent = new Intent("com.revenue.edc.hlb.pro.app2app");
            //Intent = putExtras(extra);   -->in the documentation
            intent.putExtras(extra);
            startActivity(intent);
        }*/
    }

    class ServerThread implements Runnable {

        public void run() {
            Socket socket;
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                // findViewById(R.id.start_server).setVisibility(View.GONE);
            } catch (IOException e) {
                e.printStackTrace();
                showMessage("Error Starting Server : " + e.getMessage(), Color.RED);
            }
            if (null != serverSocket) {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        socket = serverSocket.accept();
                        CommunicationThread commThread = new CommunicationThread(socket);
                        new Thread(commThread).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                        showMessage("Error Communicating to Client :" + e.getMessage(), Color.RED);
                    }
                }
            }
        }
    }

    private void sendMessage(final String message) {
        try {
            if (null != tempClientSocket) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        PrintWriter out = null;
                        try {
                            out = new PrintWriter(new BufferedWriter(
                                    new OutputStreamWriter(tempClientSocket.getOutputStream())),
                                    true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        out.println(message);
                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class CommunicationThread implements Runnable {

        private Socket clientSocket;

        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
            tempClientSocket = clientSocket;
            try {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
                showMessage("Error Connecting to Client!!", Color.RED);
            }
            showMessage("Connected to Client!!", greenColor);
        }

        public void run() {

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String read = input.readLine();
                    if (null == read || "Disconnect".contentEquals(read)) {
                        Thread.interrupted();
                        read = "Client Disconnected";
                        showMessage("Client : " + read, greenColor);
                        break;
                    }
                    showMessage("Client : " + read, greenColor);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

    }

    private void openApp() {
        //Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.revenue.hlb.im30");
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.revenue.edc.hlb.pro.app2app");

        //Uri msg = Uri.parse(edMessage.toString());

        //launchIntent = new Intent(Intent.ACTION_ALL_APPS,msg);

        if (launchIntent != null){
            startActivity(launchIntent);
        } else {
            Toast.makeText(ServerActivity.this, "There is no package", Toast.LENGTH_LONG).show();
        }

        /*if (launchIntent.resolveActivity(getPackageManager()) != null){
            startActivity(launchIntent);
        } else {
            Toast.makeText(ServerActivity.this, "There is no package", Toast.LENGTH_LONG).show();
        }*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != serverThread) {
            sendMessage("Disconnect");
            serverThread.interrupt();
            serverThread = null;
        }
    }



}