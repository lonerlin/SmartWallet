package nhlcgz.com.smartwallet;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class BTService extends Service {

    //region Defined
    Handler bluetoothIn;
    final int handlerState = 0;
    final int ORDER = 1;
    final int WARMING = 2;
    final int OVERRANGE = 1;
    boolean walletIsAlarm = false;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // String for MAC address
    private String MAC_ADDRESS = "";

    private StringBuilder recDataString = new StringBuilder();
    private BluetoothAdapter btAdapter = null;
    private boolean stopThread;
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private boolean antiTheftWarning = false;
    private BluetoothSocket bluetoothSocket;
    private Timer timerOverrange;
    private Timer timerPhoneBak;
    private int failedCount = 0;
    private LightSensorManager lightSensorManager = LightSensorManager.getInstance();
    private ConnectingThread mConnectingThread;
    private ConnectedThread mConnectedThread;

    private MsgListener msgListener;
    private ConnectingBinder connectingBinder = new ConnectingBinder();
    //endregion

    /*
    *0 进入工作状态
    *1 正常的状态监测
    * 2 开启防盗
    *3 取消防盗和警报
    * 4 寻找钱包
    * 5 取消寻找
    * 6 进入休眠状态
    * */


    class ConnectingBinder extends Binder {

        public void Connecting(String address) {
            MAC_ADDRESS = address;
            Log.d("Binder", MAC_ADDRESS);
            buildHandle();
            checkBTState();
            // mConnectedThread= new ConnectedThread(bluetoothSocket);
            //mConnectedThread.start();
        }
        public void disConnected()
        {
            write(6);
            stopThread=true;
            mConnectedThread.closeStreams();
            mConnectingThread.closeSocket();
            BTService.this.stopSelf();
        }
        public void lookingFor(boolean isLooking) {
            if (isLooking) {
                write(4);
            } else {
                write(5);
            }
        }

        public BTService getService() {
            return BTService.this;
        }

        public void overrangeWarn(boolean isWorking) {
            if (isWorking) {
                failedCount = 0;
                timerOverrange = new Timer();
                timerOverrange.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        write(1);
                        failedCount--;
                        if (failedCount < -5) {
                            warn(R.raw.bell);
                        }
                        //msgListener.stateChange(failedCount);
                    }
                }, 1000, 1000);
            } else {
                stopWarning();
                timerOverrange.cancel();
            }
        }

        public void antiTheftWarn(boolean isWorking) {

            if (isWorking) {
                antiTheftWarning = false;
                write(2);
            } else {
                antiTheftWarning=true;
                write(3);
                stopWarning();
            }
        }

        /*region PhoneBak Plus*/
        public void PhoneBak(boolean isWorking) {
            if (isWorking) {
                lightSensorManager.start(BTService.this);
                timerPhoneBak = new Timer();
                timerPhoneBak.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (lightSensorManager.getLux() > 8) {
                            warn(R.raw.warning);
                            write(4);
                        }
                    }
                }, 10000, 1000);
            } else {
                timerPhoneBak.cancel();
                lightSensorManager.stop();
                stopWarning();
                write(5);
            }

        }
        //endregion
    }


    public BTService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return connectingBinder;
    }

    public void setMessageListener(MsgListener messageListener) {
        this.msgListener = messageListener;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        startingForeground();
        mediaPlayer = MediaPlayer.create(this, R.raw.bell);
        vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
    }


    //开启
    private void startingForeground() {
        PendingIntent pendingintent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);
        Notification notification = new Notification.Builder(this)
                .setContentTitle("Smart Wallet")
                .setContentText("Smart Wallet on high alert")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingintent)
                .build();
        Log.d("Timer", "startForeground");
        startForeground(1, notification);
    }

    //region Play and stop the warning function
    /*播放警告声音*/
    private void warn(int beep) {

        mediaPlayer.reset();
        mediaPlayer = MediaPlayer.create(this, beep);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();


        long[] pattern = {100, 400, 100, 400};   // 停止 开启 停止 开启
        vibrator.vibrate(pattern, 1);           //重复两次上面的pattern 如果只想震动一次，index设为-1

    }

    /*播放警告声音停止*/
    private void stopWarning() {
        if ((mediaPlayer != null)) {
            mediaPlayer.stop();
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }
    //endregion

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    private void buildHandle() {
        Log.d("BT SERVICE", "SERVICE STARTED");
        bluetoothIn = new Handler() {

            public void handleMessage(android.os.Message msg) {
                Log.d("DEBUG", "handleMessage");
                if (msg.what == handlerState) {                                     //if message is what we want
                    String readMessage = (String) msg.obj;                                                                // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);//`enter code here`
                    Log.d("RECORDED", recDataString.toString());
                    // Do stuff here with your data, like adding it to the database
                }
                if (msg.what == ORDER) {
                    int order = (int) msg.arg1;
                    Log.d("ORDER", String.valueOf(order));
                    if (order == OVERRANGE) {
                        failedCount++;
                    } else if (order == WARMING) {
                        if (!antiTheftWarning) {
                            Log.d("antiThifWarning", "Call");
                            antiTheftWarning = true;
                            warn(R.raw.warning);
                        }

                    }


                }
                recDataString.delete(0, recDataString.length());                    //clear all string data
            }

        };


    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        if (btAdapter == null) {
            Log.d("BT SERVICE", "BLUETOOTH NOT SUPPORTED BY DEVICE, STOPPING SERVICE");
            stopSelf();
        } else {
            if (btAdapter.isEnabled()) {
                Log.d("DEBUG BT", "BT ENABLED! BT ADDRESS : " + btAdapter.getAddress() + " , BT NAME : " + btAdapter.getName());
                try {
                    BluetoothDevice device = btAdapter.getRemoteDevice(MAC_ADDRESS);
                    Log.d("DEBUG BT", "ATTEMPTING TO CONNECT TO REMOTE DEVICE : " + MAC_ADDRESS);
                    mConnectingThread = new ConnectingThread(device);
                    mConnectingThread.start();
                } catch (IllegalArgumentException e) {
                    Log.d("DEBUG BT", "PROBLEM WITH MAC ADDRESS : " + e.toString());
                    Log.d("BT SEVICE", "ILLEGAL MAC ADDRESS, STOPPING SERVICE");
                    stopSelf();
                }
            } else {
                Log.d("BT SERVICE", "BLUETOOTH NOT ON, STOPPING SERVICE");
                stopSelf();
            }
        }
    }

    //Send message to Wallet
    public void write(int input) {
        //converts entered String into bytes
        try {
            bluetoothSocket.getOutputStream().write(input);                //write bytes over BT connection via outstream
        } catch (IOException e) {
            //if you cannot write, close the application
            Log.d("DEBUG BT", "UNABLE TO READ/WRITE " + e.toString());
            Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
            //stopSelf();
        }
    }

    // New Class for Connecting Thread
    private class ConnectingThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectingThread(BluetoothDevice device) {
            Log.d("DEBUG BT", "IN CONNECTING THREAD");
            mmDevice = device;
            BluetoothSocket temp = null;
            Log.d("DEBUG BT", "MAC ADDRESS : " + MAC_ADDRESS);
            Log.d("DEBUG BT", "BT UUID : " + BTMODULEUUID);
            try {
                temp = mmDevice.createRfcommSocketToServiceRecord(BTMODULEUUID);
                Log.d("DEBUG BT", "SOCKET CREATED : " + temp.toString());
            } catch (IOException e) {
                Log.d("DEBUG BT", "SOCKET CREATION FAILED :" + e.toString());
                Log.d("BT SERVICE", "SOCKET CREATION FAILED, STOPPING SERVICE");
                stopSelf();
            }
            bluetoothSocket = temp;
            mmSocket = temp;
        }

        @Override
        public void run() {
            super.run();
            Log.d("DEBUG BT", "IN CONNECTING THREAD RUN");
            // Establish the Bluetooth socket connection.
            // Cancelling discovery as it may slow down connection
            btAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
                Log.d("DEBUG BT", "BT SOCKET CONNECTED");
                mConnectedThread = new ConnectedThread(mmSocket);
                mConnectedThread.start();
                Log.d("DEBUG BT", "CONNECTED THREAD STARTED");
                //I send a character when resuming.beginning transmission to check device is connected
                //If it is not an exception will be thrown in the write method and finish() will be called
                mConnectedThread.write("x");
            } catch (IOException e) {
                try {
                    Log.d("DEBUG BT", "SOCKET CONNECTION FAILED : " + e.toString());
                    Log.d("BT SERVICE", "SOCKET CONNECTION FAILED, STOPPING SERVICE");
                    mmSocket.close();
                    stopSelf();
                } catch (IOException e2) {
                    Log.d("DEBUG BT", "SOCKET CLOSING FAILED :" + e2.toString());
                    Log.d("BT SERVICE", "SOCKET CLOSING FAILED, STOPPING SERVICE");
                    stopSelf();
                    //insert code to deal with this
                }
            } catch (IllegalStateException e) {
                Log.d("DEBUG BT", "CONNECTED THREAD START FAILED : " + e.toString());
                Log.d("BT SERVICE", "CONNECTED THREAD START FAILED, STOPPING SERVICE");
                stopSelf();
            }

        }

        public void closeSocket() {
            try {
                //Don't leave Bluetooth sockets open when leaving activity
                mmSocket.close();
            } catch (IOException e2) {
                //insert code to deal with this
                Log.d("DEBUG BT", e2.toString());
                Log.d("BT SERVICE", "SOCKET CLOSING FAILED, STOPPING SERVICE");
                stopSelf();
            }
        }
    }

    // New Class for Connected Thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            Log.d("DEBUG BT", "IN CONNECTED THREAD");
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {

                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.d("DEBUG BT", e.toString());
                Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
                stopSelf();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.d("DEBUG BT", "IN CONNECTED THREAD RUN");
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true && !stopThread) {
                try {
                    //bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    // String readMessage = new String(buffer, 0, bytes);
                    // Log.d("DEBUG BT PART", "CONNECTED THREAD " + readMessage);
                    // Send the obtained bytes to the UI Activity via handler
                    //bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                    bytes = mmInStream.read();
                    Log.d("INSTREAM", String.valueOf(bytes));
                    bluetoothIn.obtainMessage(1, bytes, -1, null).sendToTarget();
                } catch (IOException e) {
                    Log.d("DEBUG BT", e.toString());
                    Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
                    stopSelf();
                    break;
                }
            }
        }

        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Log.d("DEBUG BT", "UNABLE TO READ/WRITE " + e.toString());
                Log.d("BT SERVICE", "UNABLE TO READ/WRITE, STOPPING SERVICE");
                //stopSelf();
            }
        }

        public void closeStreams() {
            try {
                //Don't leave Bluetooth sockets open when leaving activity
                mmInStream.close();
                mmOutStream.close();
            } catch (IOException e2) {
                //insert code to deal with this
                Log.d("DEBUG BT", e2.toString());
                Log.d("BT SERVICE", "STREAM CLOSING FAILED, STOPPING SERVICE");
                stopSelf();
            }
        }
    }

}
