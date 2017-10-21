package org.stas.tank.tankremote;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;


import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import Urrcp.*;
import io.github.controlwear.virtual.joystick.android.JoystickView;

public class MainActivity extends AppCompatActivity {

    private TextView TextViewLeft;
    private TextView TextViewRight;

    boolean is_connected = false;
    ControlThread mThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //final SeekBar sb1 = findViewById();
        //sb1.setProgress(3);

        TextViewLeft = (TextView) findViewById(R.id.textViewLeft);
        TextViewRight = (TextView) findViewById(R.id.textViewRight);

        JoystickView joystickLeft = (JoystickView) findViewById(R.id.joystickView_left);
        joystickLeft.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                double left_ax;
                double right_ax;
                double degree1;

                degree1 = Math.toRadians(angle);

                // Math.round(Math.sin(degree1)*strength) - up = + ; down = -;
                // Math.round(Math.cos(degree1)*strength) - left = -; right = +;
                left_ax = Math.round(Math.sin(degree1)*strength)+Math.round(Math.cos(degree1)*strength);
                right_ax = Math.round(Math.sin(degree1)*strength)-Math.round(Math.cos(degree1)*strength);


                TextViewLeft.setText(String.format("Left: %s", left_ax));
                TextViewRight.setText(String.format("Right: %s", right_ax));

                Map< String, String > msg_left = new HashMap< String, String >();
                msg_left.put("control_type", "speed");
                msg_left.put("device_id", "device_left");
                msg_left.put("value",String.format("%s", left_ax));

                Map< String, String > msg_right = new HashMap< String, String >();
                msg_right.put("control_type", "speed");
                msg_right.put("device_id", "device_right");
                msg_right.put("value",String.format("%s", right_ax));

                if (is_connected && mThread.isConnected)
                {
                    Message msg1 = Message.obtain();
                    msg1.obj = msg_left;
                    mThread.mHandler.sendMessage(msg1);
                    Message msg2 = Message.obtain();
                    msg2.obj = msg_right;
                    mThread.mHandler.sendMessage(msg2);
                }

            }
        });
    }

    public void s1_changed(SeekBar SeekBar, int progress,
                                  boolean fromUser){
        Log.d("Seek progress", Integer.toString(progress));
    }

    public void connect(View view) throws TException {
        if (is_connected == false) {
            is_connected = true;
            mThread = new ControlThread();
            EditText editText = (EditText) findViewById(R.id.editText);
            mThread.serverAddress = editText.getText().toString();
            mThread.start();
        } else {
            Log.e("Connect", "Already connected");
        }



    }

    public void click_ping(View view) {
        Log.d("Ping", "Button clicked");
        Message msg = Message.obtain();
        msg.obj =  "ping";
        mThread.mHandler.sendMessage(msg);
        Log.d("Ping", "Possible message sent");
    }

    public class ControlThread extends Thread {

        String serverAddress = "localhost";
        Integer serverPort = 9090;
        boolean isConnected = false;

        public Handler mHandler;
        Urrcp.Client client;

        public void run() {
            Log.d("Run Thread executed","Thrift go live");
            TTransport transport;
            transport = new TSocket(serverAddress, serverPort);
            try {
                transport.open();
                isConnected=true;
            } catch (TTransportException e) {
                e.printStackTrace();
            }


            TProtocol protocol = new TBinaryProtocol(transport);
            client = new Urrcp.Client(protocol);
            try {
                client.ping();
            } catch (TException e) {
                e.printStackTrace();
            }
            Looper.prepare();
            mHandler = new Handler() {
                public void handleMessage(Message msg) {
                    Log.d("Handler", "message received");
                    try {
                        if (msg.obj instanceof Map) {
                            String control_type;
                            control_type = ((Map<String, String>) msg.obj).get("control_type");
                            if (Objects.equals(control_type, "speed")) {
                                Log.d(((Map<String, String>) msg.obj).get("device_id"), ((Map<String, String>) msg.obj).get("value"));
                                String device_id = ((Map<String, String>) msg.obj).get("device_id");
                                Double value = Double.parseDouble(((Map<String, String>) msg.obj).get("value"));
                                client.set_device_speed(device_id,value);
                            }
                        }
                    } catch (Exception x) {
                        Log.e("ERROR", x.toString());
                    }
                }
            };
            Looper.loop();

        }

    }


}
