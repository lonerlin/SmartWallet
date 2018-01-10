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
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    Button bSetting;
    Button bConnecting;
    Button bLookingfor;
    String deviceAddress;
    Button bOverrangeWarn;
    Button bWalletAlarm;
    Button bLightTest;
    Button bPhoneBak;
    TextView tvInfo;

    private BTService.ConnectingBinder connectingBinder;

    private ServiceConnection connection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            connectingBinder=(BTService.ConnectingBinder)service;
            connectingBinder.getService().setMessageListener(new MsgListener() {
                @Override
                public void stateChange(int msg) {
                    tvInfo.setText(String.valueOf(msg));
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
        bSetting=(Button) findViewById(R.id.bSetting);
        bConnecting=(Button)findViewById(R.id.bConnecting);
        bLookingfor=(Button)findViewById(R.id.bLookingFor);
        bLookingfor.setOnClickListener(this);
        tvInfo=(TextView)findViewById(R.id.tvInfo);
        bOverrangeWarn=(Button)findViewById(R.id.bOverrangeWarn);
        bOverrangeWarn.setOnClickListener(this);
        bWalletAlarm=(Button)findViewById(R.id.bWalletAlarm);
        bWalletAlarm.setOnClickListener(this);
        bPhoneBak=(Button)findViewById(R.id.bPhoneBak);
        bPhoneBak.setOnClickListener(this);
        deviceAddress=readAddress("address");
        bLightTest=(Button)findViewById(R.id.bLightTest);


        bLightTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


            }
        });

        bindService();

        if(deviceAddress.contains(""))
        {
            startActivityForResult(new Intent(MainActivity.this,DeviceListActivity.class),1);
        }

        bSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startActivityForResult(new Intent(MainActivity.this,DeviceListActivity.class),1);
            }
        });
        bConnecting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("address","Click");
                connectingBinder.Connecting(readAddress("address"));
            }
        });

    }

    void bindService()
    {
        Intent startIntent=new Intent(this,BTService.class);
        startService(startIntent);
        Intent bindIntent=new Intent(this,BTService.class);
        bindService(bindIntent,connection,BIND_AUTO_CREATE);
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

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.bLookingFor:
                if(bLookingfor.getText().toString()==this.getString(R.string.lookingFor))
                {
                    Log.d("MainActivity","Lookingfor");
                    connectingBinder.lookingFor(true);
                    bLookingfor.setText(R.string.stopLookingFor);
                }else
                {
                    connectingBinder.lookingFor(false);
                    bLookingfor.setText(R.string.lookingFor);
                }
                break;
            case R.id.bOverrangeWarn:
                if(bOverrangeWarn.getText().toString()==this.getString(R.string.overrangeWarn))
                {
                    connectingBinder.overrangeWarn(true);
                    bOverrangeWarn.setText(R.string.stopOverrangeWarn);
                }else {
                    connectingBinder.overrangeWarn(false);
                    bOverrangeWarn.setText(R.string.overrangeWarn);
                }
                break;
            case R.id.bWalletAlarm:
                if(bWalletAlarm.getText().toString()==getString(R.string.walletAlarm))
                {
                    connectingBinder.antiTheftWarn(true);
                    bWalletAlarm.setText(R.string.stopWalletAlarm);
                }else{
                    connectingBinder.antiTheftWarn(false);
                    bWalletAlarm.setText(R.string.walletAlarm);
                }
                break;
            case R.id.bPhoneBak:
                if(bPhoneBak.getText().toString()==getString(R.string.phoneBak))
                {
                    connectingBinder.PhoneBak(true);
                    bPhoneBak.setText(R.string.stopPhoneBak);
                }else{
                    connectingBinder.PhoneBak(false);
                    bPhoneBak.setText(R.string.phoneBak);
                }
                break;
            default:
                break;


        }
    }
}
