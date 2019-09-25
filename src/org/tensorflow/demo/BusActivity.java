package org.tensorflow.demo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;

public class BusActivity extends Activity {
    private Button detectbus_btn;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        detectbus_btn = (Button)findViewById(R.id.detectbus);

    }
}
