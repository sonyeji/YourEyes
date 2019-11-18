package org.YourEyes.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.odsay.odsayandroidsdk.API;
import com.odsay.odsayandroidsdk.ODsayData;
import com.odsay.odsayandroidsdk.ODsayService;
import com.odsay.odsayandroidsdk.OnResultCallbackListener;

import org.YourEyes.demo.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_ADD;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static java.lang.Thread.sleep;


public class BusActivity extends Activity {
    private Spinner sp_api;
    private Button bt_api_call;
    private EditText inputBus;
    private EditText inputStation;
    private ListView stationList;
    private LinearLayout inputBus_layout;
    private LinearLayout inputStation_layout;
    private Button inputBus_ok;
    private Button inputStation_ok;

    private Context context;
    private String spinnerSelectedName;

    private ODsayService odsayService;
    private JSONObject jsonObject;

    private double longitude;
    private double latitude;

    private StatAdapter adapter;
    private LocationManager lm;
    private int RESAULT_CALL_BACK_STATE = 0;

    private String stationName;
    private String cityCode = "25";
    private ArrayList<Station> stations;
    private TextToSpeech tts;
    private ImageButton voice_btn;

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
        stationList = (ListView)findViewById(R.id.stationList);
        inputBus = (EditText)findViewById(R.id.inputBus);
        inputStation = (EditText)findViewById(R.id.inputStation);

        inputBus_layout = (LinearLayout)findViewById(R.id.inputBus_layout);
        inputStation_layout = (LinearLayout)findViewById(R.id.inputStation_layout);

        inputBus_ok = (Button)findViewById(R.id.inputBus_ok);
        inputStation_ok = (Button)findViewById(R.id.inputStation_ok);

        sp_api.setSelection(0);

        odsayService = ODsayService.init(BusActivity.this, getString(R.string.odsay_key));
        odsayService.setReadTimeout(5000);
        odsayService.setConnectionTimeout(5000);

        bt_api_call.setOnClickListener(onClickListener);
        sp_api.setOnItemSelectedListener(onItemSelectedListener);
        voice_btn = findViewById(R.id.voice_btn);

        tts = new TextToSpeech(getBaseContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != ERROR) {
                    int language = tts.setLanguage(Locale.KOREAN);
                    if (language == TextToSpeech.LANG_MISSING_DATA

                            || language == TextToSpeech.LANG_NOT_SUPPORTED) {

                        // 언어 데이터가 없거나, 지원하지 않는경우

                        Toast.makeText(getBaseContext(), "지원하지 않는 언어입니다.", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        String speak = "버스 안내 메뉴 입니다.";
                        tts.speak(speak, TextToSpeech.QUEUE_FLUSH, null);
                    }
                }else {

                    // 작업 실패

                    Toast.makeText(getBaseContext(), "TTS 작업에 실패하였습니다.", Toast.LENGTH_SHORT).show();

                }
            }
        });

        //정류장 클릭 -> 실시간 도착정보 액티비티 시작
        stationList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String stationID =  stations.get(position).getLocal_id();
                String stationName = stations.get(position).getName();
                Intent intent = new Intent(getApplicationContext(), RealTimeStationInfo.class);

                intent.putExtra("name", stationName);
                intent.putExtra("id", stationID);
                intent.putExtra("cityCode", cityCode);
                startActivity(intent);
            }
        });

        inputBus_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        inputStation_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stationName = inputStation.getText().toString();

                ReceiveStationTask receiveStationTaskTask = new ReceiveStationTask();
                receiveStationTaskTask.execute("");
            }
        });
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
            //tv_data.setText(jsonObject.toString());
            switch(RESAULT_CALL_BACK_STATE) {
                case 1:     //Bus
                    break;
                case 2:     //Station

                    break;
                case 3:     //Location

                    break;
            }

        }

        @Override
        public void onError(int i, String errorMessage, API api) {
            //tv_data.setText("API : " + api.name() + "\n" + errorMessage);
        }
    };

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String s_lon = Double.toString(longitude);
            String s_lat = Double.toString(latitude);
            String my_loc = s_lon + ":" + s_lat;
            switch (spinnerSelectedName) {
                case "버스노선 상세정보 조회":
                    inputStation_layout.setVisibility(View.GONE);stationList.setVisibility(View.GONE);
                    inputBus_layout.setVisibility(View.VISIBLE);
                    voice_btn.setVisibility(View.GONE);

                    RESAULT_CALL_BACK_STATE = 1;
                    odsayService.requestBusLaneDetail("12018", onResultCallbackListener);
                    break;
                case "버스정류장 세부정보 조회":
                    inputBus_layout.setVisibility(View.GONE);stationList.setVisibility(View.GONE);
                    inputStation_layout.setVisibility(View.VISIBLE);
                    voice_btn.setVisibility(View.VISIBLE);

                    RESAULT_CALL_BACK_STATE = 2;

                    break;

                case "반경내 정류장 검색":
                    //반경 내 정류장 나타내는 View만 활성화
                    inputBus_layout.setVisibility(View.GONE);inputStation_layout.setVisibility(View.GONE);
                    voice_btn.setVisibility(View.VISIBLE);
                    RESAULT_CALL_BACK_STATE = 3;

                    //정류장 검색 Task
                    ReceiveStationTask receiveStationTaskTask = new ReceiveStationTask();
                    receiveStationTaskTask.execute("");

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

    //버스 정류장 검색
    private class ReceiveStationTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        @Override
        protected String doInBackground(String... datas) {
            String readed = "";
            try {
                Log.d("startapi", "gg");
                String SERVICE_KEY = "zJAqMbeplL5DHNnwY00zhzBEqz4NOelbiI5Oir5QmkLI%2BMNfEcQmSPyRtDZVzDjIRHeKSSG%2B%2BscjNosmgeHlEQ%3D%3D";
                String URL = "";
                StringBuilder urlBuilder = new StringBuilder(URL);

                //버스 정류장 검색
                if(RESAULT_CALL_BACK_STATE == 2) {
                    URL = "http://openapi.tago.go.kr/openapi/service/BusSttnInfoInqireService/getSttnNoList?ServiceKey="+SERVICE_KEY;
                    urlBuilder = new StringBuilder(URL);
                    urlBuilder.append("&cityCode="+cityCode);
                    String encode_name = URLEncoder.encode(stationName, "utf-8");
                    urlBuilder.append("&nodeNm="+encode_name);

                    Log.d("URL", urlBuilder.toString());
                }

                //반경 내 정류장 검색
                else if(RESAULT_CALL_BACK_STATE == 3) {
                    URL = "http://openapi.tago.go.kr/openapi/service/BusSttnInfoInqireService/getCrdntPrxmtSttnList?ServiceKey="+SERVICE_KEY;
                    urlBuilder = new StringBuilder(URL);
                    String s_lon = Double.toString(longitude);
                    String s_lat = Double.toString(latitude);

                    urlBuilder.append("&gpsLati="+s_lat);
                    urlBuilder.append("&gpsLong="+s_lon);

                    Log.d("URL2", urlBuilder.toString());
                }

                java.net.URL url = new URL(urlBuilder.toString());
                InputStream is= url.openStream(); //url위치로 입력스트림 연결
                InputStreamReader reader = new InputStreamReader(is);
                BufferedReader in = new BufferedReader(reader);

                //결과 값 저장
                while((readed = in.readLine()) != null)
                    return readed;

            } catch (Exception e) {

            }
            return "";
        }
        @Override
        protected void onPostExecute(String result) {
            Log.d("postexe1", result);
            try {
                //파싱
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new InputSource(new StringReader(result)));

                NodeList nodeList = document.getElementsByTagName("item");
                stations = new ArrayList<>();

                if(RESAULT_CALL_BACK_STATE == 2) {
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        Node node = nodeList.item(i);

                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            Element element = (Element) node;
                            String y = getTagValue("gpslati", element);
                            String x = getTagValue("gpslong", element);
                            String local_id = getTagValue("nodeid", element);
                            String name = getTagValue("nodenm", element);
                            String id = getTagValue("nodeno", element);

                            stations.add(new Station(name, id, local_id, x, y));
                        }
                    }

                    //버스 정류장 ListView 어댑터
                    adapter = new StatAdapter(getApplicationContext(), stations);
                    stationList.setAdapter(adapter);
                }

                else if(RESAULT_CALL_BACK_STATE == 3) {
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        Node node = nodeList.item(i);

                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            Element element = (Element) node;
                            String code = getTagValue("citycode", element);

                            if(code.equals(cityCode)) {
                                String y = getTagValue("gpslati", element);
                                String x = getTagValue("gpslong", element);
                                String local_id = getTagValue("nodeid", element);
                                String name = getTagValue("nodenm", element);
                                String id = getTagValue("nodeno", element);

                                stations.add(new Station(name, id, local_id, x, y));
                            }
                        }
                    }
                    adapter = new StatAdapter(getApplicationContext(), stations);
                    stationList.setAdapter(adapter);
                }

                stationList.setVisibility(View.VISIBLE);
                Thread speech = new Thread() {
                    public void run() {
                        try {
                            ListSpeaking(stations, tts);
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                speech.start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private static String getTagValue(String tag, Element eElement) {
        NodeList nlList = eElement.getElementsByTagName(tag).item(0).getChildNodes();
        Node nValue = (Node) nlList.item(0);
        if(nValue == null)
            return null;
        return nValue.getNodeValue();
    }
    class Station {
        String name;
        String id;
        String local_id;
        String x;
        String y;
        public Station(String name, String id, String local_id, String x, String y) {
            this.name = name;
            this.id = id;
            this.local_id = local_id;
            this.x = x;
            this.y = y;
        }

        public String getName() {
            return this.name;
        }
        public String getId() {
            return this.id;
        }
        public String getLocal_id() {
            return this.local_id;
        }
    }

    private static void ListSpeaking(ArrayList<Station> stations, TextToSpeech tts) throws InterruptedException {
        int size = stations.size();
        String speak = "";
        int i = 0;
        while (true) {
            if(i > size)    break;
            if (tts.isSpeaking() == false) {
                speak = stations.get(i).name + " " + stations.get(i).id;
                tts.speak(speak, QUEUE_FLUSH, null);
                tts.playSilence(1000, QUEUE_ADD, null);
                i++;
            }else{
                continue;
            }
        }
    }

}
