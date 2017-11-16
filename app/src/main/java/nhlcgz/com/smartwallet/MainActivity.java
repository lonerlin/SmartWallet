package nhlcgz.com.smartwallet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button bSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bSetting=(Button) findViewById(R.id.bSetting);

        bSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(MainActivity.this,DeviceListActivity.class);
                startActivity(intent);
            }
        });
    }

    private void saveMac(String key,String value)
    {
        SharedPreferences.Editor editor=getSharedPreferences("SmartWallet",MODE_WORLD_WRITEABLE).edit();
        editor.putString(key,value);
        editor.commit();
    }
    private String readMac(String key)
    {
        SharedPreferences sharedPreferences=getSharedPreferences("SmartWallet",MODE_WORLD_READABLE);
        return sharedPreferences.getString(key,"");
    }
}
