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

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    Button bSetting;
    Button bConnecting;
    Button bLookingfor;
    String deviceAddress;
    private BTService.ConnectingBinder connectingBinder;

    private ServiceConnection connection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            connectingBinder=(BTService.ConnectingBinder)service;
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
        deviceAddress=readAddress("address");

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

        }
    }
}
