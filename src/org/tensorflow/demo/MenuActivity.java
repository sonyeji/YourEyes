package org.tensorflow.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

public class MenuActivity extends Activity {
    Button detect_btn;
    Button text_btn;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        detect_btn = (Button)findViewById(R.id.detect_btn);
        text_btn = (Button)findViewById(R.id.text_btn);

        detect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               startActivity(new Intent(getBaseContext(), DetectorActivity.class));
            }
        });
        text_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

    }
}
