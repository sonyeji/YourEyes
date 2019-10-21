package org.YourEyes.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
//import android.view.WindowManager;
//import android.widget.Button;
import android.widget.ImageButton;

import org.YourEyes.demo.R;

public class MenuActivity extends Activity {
    private ImageButton detect_btn;
    private ImageButton text_btn;
    private ImageButton bus_btn;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_activity);

        detect_btn = (ImageButton)findViewById(R.id.detect_btn);
        text_btn = (ImageButton)findViewById(R.id.text_btn);
        bus_btn = (ImageButton)findViewById(R.id.bus_btn);

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
               // startActivity(new Intent(getBaseContext(), SttTest.class));
            }
        });
    }
}
