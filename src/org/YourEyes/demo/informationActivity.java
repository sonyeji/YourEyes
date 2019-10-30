package org.YourEyes.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

public class informationActivity extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.information);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.information_titlebar);
    }
}
