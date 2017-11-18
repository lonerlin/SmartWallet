package nhlcgz.com.smartwallet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button bSetting;
    String deviceAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bSetting=(Button) findViewById(R.id.bSetting);

        deviceAddress=readAddress("address");

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
}
