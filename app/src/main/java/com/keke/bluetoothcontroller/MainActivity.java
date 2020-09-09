package com.keke.bluetoothcontroller;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    static final int TEXT_MSG = 0x00;
    static final int TEXT_INFO = 0x01;
    static final int TEXT_ERROR = 0x02;
    static final int TEXT_SEND = 0x03;

    static final int REQUEST_SETTING = 0x00;

    static final int COUNTER_MAX = 100;

    SharedPreferences mPrefs = null;

    int mLogCounter = 0;

    BluetoothAdapter mBluetoothAdapter = null;

    OperatorView mLeftOperator = null;
    OperatorView mRightOperator = null;
    boolean mLastLeftCursorOn = false;
    boolean mLastRightCursorOn = false;

    TextView mLastCommand = null;
    TextView mLogView = null;

    void addLog(String msg, String color) {
        msg = TextUtils.htmlEncode(msg.replace("\n", "<br>"));
        msg = String.format("%02d: ", mLogCounter) + "<font color='" + color + "'>" + msg + "</font><br>";
        Log.d("addLog", msg);
        mLogCounter = (mLogCounter + 1) % COUNTER_MAX;
        ((Editable)mLogView.getText()).insert(0, Html.fromHtml(msg));
    }

    void showCommand(String cmd) {
        mLastCommand.setText(cmd);
    }

    void addLog(String msg, int level) {
        int color = 0;
        if(level == TEXT_INFO) {
            color = getResources().getColor(R.color.text_color_info);
        }
        if(level == TEXT_ERROR) {
            color = getResources().getColor(R.color.text_color_error);
        }
        if(level == TEXT_MSG) {
            color = getResources().getColor(R.color.text_color_msg);
        }
        if(level == TEXT_SEND) {
            color = getResources().getColor(R.color.text_color_send);
        }
        String colorStr = "#" + Integer.toHexString(color & 0x00ffffff);
        addLog(msg, colorStr);
    }

    boolean connected = false;

    Handler mLogHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            addLog((String)msg.obj, msg.what);
            return true;
        }
    });

    class BluetoothThread extends Thread {
        BluetoothDevice device;
        BluetoothSocket socket;
        BufferedReader reader;
        OutputStreamWriter writer;
        Scanner scanner;

        public BluetoothThread(BluetoothDevice device) {
            this.device = device;
        }

        boolean connect() {
            try{
                String uuid = getResources().getString(R.string.bluetooth_default_uuid);
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString(uuid));
            }
            catch(IOException e) {
                Message msg = mLogHandler.obtainMessage(TEXT_ERROR, getResources().getString(R.string.bluetooth_unable_to_open_socket));
                mLogHandler.sendMessage(msg);
                return false;
            }
            try{
                socket.connect();
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                scanner = new Scanner(reader);
                writer = new OutputStreamWriter(socket.getOutputStream());
            }
            catch(IOException e) {
                Message msg = mLogHandler.obtainMessage(TEXT_ERROR, getResources().getString(R.string.bluetooth_connection_failed));
                mLogHandler.sendMessage(msg);
                try {
                    socket.close();
                }
                catch (IOException t) {  }
                return false;
            }
            return true;
        }

        void receiveData() {
            while(connected) {
                try {
                    Message msg = mLogHandler.obtainMessage(TEXT_MSG, reader.readLine());
                    mLogHandler.sendMessage(msg);
                }
                catch(IOException e) {
                    connected = false;
                    Message msg = mLogHandler.obtainMessage(TEXT_ERROR, getResources().getString(R.string.bluetooth_unable_to_receive));
                    mLogHandler.sendMessage(msg);
                }
            }
        }

        public void run() {
            if(connect()) {
                addLog(getResources().getString(R.string.connection_succeed), TEXT_INFO);
                connected = true;
                receiveData();
            }
        }

        public void send(String cmd) {
            try {
                writer.write(cmd);
                writer.flush();
            }
            catch(IOException e) {
                Message msg = mLogHandler.obtainMessage(TEXT_ERROR, getResources().getString(R.string.bluetooth_unable_to_send));
                mLogHandler.sendMessage(msg);
            }
        }

        public void close() {
            if(connected) {
                try {
                    connected = false;
                    socket.close();
                }
                catch(IOException e) {  }
            }
        }
    }

    BluetoothThread mBtThread = null;

    void sendCommand(String cmd) {
        if(connected) {
            mBtThread.send(cmd + "\r\n");
        }
        else{
            addLog(getResources().getString(R.string.no_connection), TEXT_ERROR);
        }
    }

    Timer mSendTimer = null;

    class SendTask extends TimerTask {
        @Override
        public void run() {
            boolean forceSend = mPrefs.getBoolean("force_send_command", false);
            boolean shouldSendLeft = (mLeftOperator.mCursorOn || mLastLeftCursorOn);
            boolean shouldSendRight = (mRightOperator.mCursorOn || mLastRightCursorOn);
            if(forceSend || shouldSendLeft || shouldSendRight) {
                float rx = 0, ry = 0;
                if(mPrefs.getBoolean("separation_control", true)){
                    if(mPrefs.getBoolean("separation_control", false)) {
                        rx = mLeftOperator.mRX;
                        ry = mRightOperator.mRY;
                    }
                    else{
                        rx = mRightOperator.mRX;
                        ry = mLeftOperator.mRY;
                    }
                }
                else{
                    rx = OperatorView.clip(mLeftOperator.mRX + mRightOperator.mRX, -1, 1);
                    ry = OperatorView.clip(mLeftOperator.mRY + mRightOperator.mRY, -1, 1);
                }
                int quant = Integer.parseInt(mPrefs.getString("quantification", "255"));
                int x = Math.round(quant * rx), y = Math.round(quant * ry);
                String format = mPrefs.getString("command_format", getResources().getString(R.string.command_format_default));
                String cmd = String.format(format, x, y);
                showCommand(cmd);
                if(!forceSend || connected) {
                    sendCommand(cmd);
                }
            }
            mLastLeftCursorOn = mLeftOperator.mCursorOn;
            mLastRightCursorOn = mRightOperator.mCursorOn;
        }
    }

    void setupTimer() {
        if(mSendTimer != null) {
            mSendTimer.cancel();
        }
        mSendTimer = new Timer();
        int freq = Integer.parseInt(mPrefs.getString("frequency", "10"));
        int delay = Math.max(1000 / Math.max(freq, 1), 1);
        mSendTimer.schedule(new SendTask(), 0, delay);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mLeftOperator = findViewById(R.id.left_operator);
        mRightOperator = findViewById(R.id.right_operator);
        mLastCommand = findViewById(R.id.command_edit);
        mLogView = findViewById(R.id.log_view);
        mLogView.setText("", TextView.BufferType.EDITABLE);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null) {
            addLog(getResources().getString(R.string.no_blue_tooth_adapter), TEXT_ERROR);
        }
        else if(!mBluetoothAdapter.isEnabled()){
            startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
        }
    }

    public void onConnectClick(View v) {
        final List<BluetoothDevice> devices = new ArrayList<>(mBluetoothAdapter.getBondedDevices());
        if(devices.size() == 0) {
            addLog(getResources().getString(R.string.bluetooth_no_device), TEXT_INFO);
            return;
        }
        final String[] device_names = new String[devices.size()];
        for(int i = 0; i < devices.size(); i++) {
            device_names[i] = devices.get(i).getName() + "[" + devices.get(i).getAddress() + "]";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.choose_device));
        builder.setItems(device_names, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(mBtThread != null) {
                    mBtThread.close();
                }
                mBtThread = new BluetoothThread(devices.get(i));
                mBtThread.start();
            }
        });
        builder.show();
    }

    public void onCommandClick(View v) {
        final EditText editText = new EditText(this);
        editText.setMaxLines(1);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.send_command));
        builder.setView(editText);
        builder.setPositiveButton(R.string.send_command_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String cmd = editText.getText().toString();
                addLog(cmd, TEXT_SEND);
                sendCommand(cmd);
            }
        });
        builder.setNegativeButton(R.string.send_command_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    public void onSettingClick(View v) {
        mSendTimer.cancel();
        Intent intent = new Intent(this, Settings.class);
        startActivityForResult(intent, REQUEST_SETTING);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSendTimer.cancel();
        Log.d("MainActivity", "cancel timer");
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupTimer();
        Log.d("MainActivity", "setup timer");
    }

    public void fastCommand(String name) {
        String cmd = mPrefs.getString(name, name);
        addLog(cmd, TEXT_SEND);
        sendCommand(cmd);
    }

    public void onL1Click(View v){
        fastCommand("L1");
    }

    public void onL2Click(View v){
        fastCommand("L2");
    }

    public void onR1Click(View v){
        fastCommand("R1");
    }

    public void onR2Click(View v){
        fastCommand("R2");
    }

}
