package nhlcgz.com.smartwallet;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{





    String deviceAddress;



    ImageButton iBluetooth;
    ImageButton iConnecting;
    boolean isConnecting=false;
    ImageButton iLookingFor;
    boolean isLookingFor=false;
    ImageButton iOverrangeWarn;
    boolean isOverrangeWarn=false;
    ImageButton iAntiTheftWarn;
    boolean isAntiTheftWarn=false;
    ImageButton iPhoneBak;
    boolean isPhoneBak=false;


    private BTService.ConnectingBinder connectingBinder;

    private ServiceConnection connection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            connectingBinder=(BTService.ConnectingBinder)service;
            connectingBinder.getService().setMessageListener(new MsgListener() {
                @Override
                public void stateChange(int msg) {

                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceAddress=readAddress("address");
        Log.d("Address",deviceAddress);
        iConnecting=(ImageButton)findViewById(R.id.ibConnecting);
        iConnecting.setOnClickListener(this);
        iBluetooth=(ImageButton)findViewById(R.id.ibBluetooth);
        iBluetooth.setOnClickListener(this);
        iPhoneBak=(ImageButton)findViewById(R.id.ibPhoneBak);
        iPhoneBak.setOnClickListener(this);
        iAntiTheftWarn=(ImageButton)findViewById(R.id.ibWalletAlarm);
        iAntiTheftWarn.setOnClickListener(this);
        iLookingFor=(ImageButton)findViewById(R.id.ibLookFor);
        iLookingFor.setOnClickListener(this);
        iOverrangeWarn=(ImageButton)findViewById(R.id.ibOverrangeWarn);
        iOverrangeWarn.setOnClickListener(this);

        if(deviceAddress=="")
        {
            startActivityForResult(new Intent(MainActivity.this,DeviceListActivity.class),1);
        }
        bindService();
        ButtonEnable(false);




    }
    Intent startIntent;
    Intent bindIntent;
    void bindService()
    {
       // Intent startIntent=new Intent(this,BTService.class);
        startIntent=new Intent(this,BTService.class);
        startService(startIntent);
        //Intent bindIntent=new Intent(this,BTService.class);
        bindIntent=new Intent(this,BTService.class);
        bindService(bindIntent,connection,BIND_AUTO_CREATE);
    }

    @Override
    public void onBackPressed() {
        if(isConnecting)
        {

        }else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        String result=data.getExtras().getString("result");
        Log.d("address",result);
        if(result!="")
        {
            saveAddress("address",result);
            Log.d("address","Save!");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void saveAddress(String key, String value)
    {
        SharedPreferences.Editor editor=getSharedPreferences("SmartWallet",MODE_WORLD_WRITEABLE).edit();
        editor.putString(key,value);
        editor.commit();
    }
    private String readAddress(String key)
    {
        SharedPreferences sharedPreferences=getSharedPreferences("SmartWallet",MODE_WORLD_READABLE);
        return sharedPreferences.getString(key,"");
    }
    private void ButtonEnable(boolean Enable)
    {
        if(Enable)
        {
            iLookingFor.setEnabled(true);
            iAntiTheftWarn.setEnabled(true);
            iOverrangeWarn.setEnabled(true);
            iPhoneBak.setEnabled(true);
        }else
        {
            iLookingFor.setEnabled(false);
            iAntiTheftWarn.setEnabled(false);
            iOverrangeWarn.setEnabled(false);
            iPhoneBak.setEnabled(false);
        }

    }
    @Override
    public void onClick(View v) {
        switch (v.getId())
        {


            case R.id.ibBluetooth:
                startActivityForResult(new Intent(MainActivity.this,DeviceListActivity.class),1);
                break;
            case R.id.ibConnecting:
                if(isConnecting)
                {
                    isConnecting=false;
                    iConnecting.setImageResource(R.drawable.start);
                    ButtonEnable(false);
                    connectingBinder.disConnected();
                    stopService(bindIntent);
                    stopService(startIntent);
                }else
                {
                    bindService();
                    isConnecting=true;
                    iConnecting.setImageResource(R.drawable.stop);
                    connectingBinder.Connecting(readAddress("address"));
                    ButtonEnable(true);
                }
                break;
            case R.id.ibLookFor:
                if(isLookingFor)
                    iLookingFor.setImageResource(R.drawable.lookingforw);
                else
                    iLookingFor.setImageResource(R.drawable.lookingfory);
                isLookingFor=!isLookingFor;
                connectingBinder.lookingFor(isLookingFor);
                break;
            case R.id.ibOverrangeWarn:
                if(isOverrangeWarn)
                    iOverrangeWarn.setImageResource(R.drawable.linkw);
                else
                    iOverrangeWarn.setImageResource(R.drawable.linky);
                isOverrangeWarn=!isOverrangeWarn;
                connectingBinder.overrangeWarn(isOverrangeWarn);
                break;
            case R.id.ibWalletAlarm:
                if(isAntiTheftWarn)
                    iAntiTheftWarn.setImageResource(R.drawable.warningw);
                else
                    iAntiTheftWarn.setImageResource(R.drawable.warningy);
                isAntiTheftWarn=!isAntiTheftWarn;
                connectingBinder.antiTheftWarn(isAntiTheftWarn);
                break;
            case R.id.ibPhoneBak:
                if(isPhoneBak)
                    iPhoneBak.setImageResource(R.drawable.dogw);
                else
                    iPhoneBak.setImageResource(R.drawable.dogy);
                isPhoneBak=!isPhoneBak;
                connectingBinder.PhoneBak(isPhoneBak);
                break;
            default:
                break;


        }
    }
}
