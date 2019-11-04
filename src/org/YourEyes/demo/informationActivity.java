package org.YourEyes.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

public class informationActivity extends Activity{
    ImageView img;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.information);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.information_titlebar);
        img = (ImageView) findViewById(R.id.appinfo);
    }
}
