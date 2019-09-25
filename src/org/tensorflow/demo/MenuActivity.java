package org.tensorflow.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

public class MenuActivity extends Activity {
    private Button detect_btn;
    private Button text_btn;
    private Button bus_btn;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_activity);

        detect_btn = (Button)findViewById(R.id.detect_btn);
        text_btn = (Button)findViewById(R.id.text_btn);
        bus_btn = (Button)findViewById(R.id.bus_btn);

        detect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               startActivity(new Intent(getBaseContext(), DetectorActivity.class));
            }
        });
        text_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(), TextDetectActivity.class));
            }
        });
        bus_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getBaseContext(), BusActivity.class));
            }
        });
    }
}
