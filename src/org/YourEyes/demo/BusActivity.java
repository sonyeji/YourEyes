package org.YourEyes.demo;

import android.Manifest;
import android.app.Activity;
//import android.support.v7.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.IdRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.odsay.odsayandroidsdk.API;
import com.odsay.odsayandroidsdk.ODsayData;
import com.odsay.odsayandroidsdk.ODsayService;
import com.odsay.odsayandroidsdk.OnResultCallbackListener;

import org.YourEyes.demo.R;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BusActivity extends Activity {
    private Spinner sp_api;
   // private RadioGroup rg_object_type;
   // private RadioButton rb_json, rb_map;
    private Button bt_api_call;
    private TextView tv_data;

    private Context context;
    private String spinnerSelectedName;


    private ODsayService odsayService;
    private JSONObject jsonObject;
    private Map mapObject;

    private double longitude;
    private double latitude;

    private Location location;
    private LocationManager lm;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.bus_activity);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.bus_titlebar);

        init();
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Init_GPS();
    }

    private void init() {
        context = this;
        sp_api = (Spinner) findViewById(R.id.sp_api);
        bt_api_call = (Button) findViewById(R.id.bt_api_call);
        tv_data = (TextView) findViewById(R.id.tv_data);
        sp_api.setSelection(0);

        odsayService = ODsayService.init(BusActivity.this, getString(R.string.odsay_key));
        odsayService.setReadTimeout(5000);
        odsayService.setConnectionTimeout(5000);

        bt_api_call.setOnClickListener(onClickListener);
        sp_api.setOnItemSelectedListener(onItemSelectedListener);

    }

    private AdapterView.OnItemSelectedListener onItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view,
                                   int position, long id) {
            spinnerSelectedName = (String) parent.getItemAtPosition(position);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    private OnResultCallbackListener onResultCallbackListener = new OnResultCallbackListener() {
        @Override
        public void onSuccess(ODsayData oDsayData, API api) {
            jsonObject = oDsayData.getJson();
            mapObject = oDsayData.getMap();
            tv_data.setText(jsonObject.toString());

        }

        @Override
        public void onError(int i, String errorMessage, API api) {
            tv_data.setText("API : " + api.name() + "\n" + errorMessage);
        }
    };

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String s_lon = Double.toString(longitude);
            String s_lat = Double.toString(latitude);
            String my_loc = s_lon + ":" + s_lat;
            switch (spinnerSelectedName) {
                case "버스 노선 조회":
                    odsayService.requestSearchBusLane("150", "1000", "no", "10", "1", onResultCallbackListener);
                    break;
                case "버스노선 상세정보 조회":
                    odsayService.requestBusLaneDetail("12018", onResultCallbackListener);
                    break;
                case "버스정류장 세부정보 조회":
                    odsayService.requestBusStationInfo("107475", onResultCallbackListener);
                    break;
                case "노선 그래픽 데이터 검색":
                    odsayService.requestLoadLane("0:0@12018:1:-1:-1", onResultCallbackListener);
                    break;
                case "대중교통 정류장 검색":
                    odsayService.requestSearchStation("11", "1000", "1", "10", "1", "127.34408682849399:36.36633737269802", onResultCallbackListener);
                    break;
                case "반경내 대중교통 POI 검색":
                    odsayService.requestPointSearch(s_lon, s_lat, "250", "1", onResultCallbackListener);
                    break;
                case "지도 위 대중교통 POI 검색":
                    odsayService.requestBoundarySearch("127.045478316811:37.68882830829:127.055063420699:37.6370465749586", "127.045478316811:37.68882830829:127.055063420699:37.6370465749586", "1:2", onResultCallbackListener);
                    break;
                case "대중교통 길찾기":
                    odsayService.requestSearchPubTransPath("126.926493082645", "37.6134436427887", "127.126936754911", "37.5004198786564", "0", "0", "0", onResultCallbackListener);
                    break;
                case "도시코드 조회":
                    odsayService.requestSearchCID("서울", onResultCallbackListener);
                    break;
            }
        }
    };

    //처음 gps
    public void Init_GPS() {
        if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(BusActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);

        } else {
            //권한을 허용한 경우
            try {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,100,1, mLocationListener);
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,100,1, mLocationListener);
            } catch(SecurityException e) {
                e.printStackTrace();
            }
        }

    }
    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            Log.d("Location", longitude+", "+latitude);
            Toast.makeText(getApplicationContext(), longitude+" "+latitude, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

}
