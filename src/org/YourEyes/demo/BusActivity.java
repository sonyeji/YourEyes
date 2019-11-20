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
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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


public class BusActivity extends Activity {
    private Spinner sp_api;
    private Button bt_api_call;
    private EditText inputBus;
    private EditText inputStation;
    private TextView laneInfo;
    private ListView stationList;
    private LinearLayout inputBus_layout;
    private LinearLayout inputStation_layout;
    private Button inputBus_ok;
    private Button inputStation_ok;

    private LinearLayout laneInfo_layout;
    private Button direction1_btn;
    private Button direction2_btn;
    private LinearLayout direction_btn_layout;


    private String BUS_ID = "";
    private int BUS_DIRECTION = 0;

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
    private SpeechRecognizer mRecognizer;
    private Intent i;
    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;
    public String input_voice = "";
    public String search_name = "";
    public int listen_state = 0;
    public int result_state = 0;

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
        laneInfo = (TextView)findViewById(R.id.laneInfo);
        stationList = (ListView)findViewById(R.id.stationList);
        inputBus = (EditText)findViewById(R.id.inputBus);
        inputStation = (EditText)findViewById(R.id.inputStation);

        inputBus_layout = (LinearLayout)findViewById(R.id.inputBus_layout);
        inputStation_layout = (LinearLayout)findViewById(R.id.inputStation_layout);

        inputBus_ok = (Button)findViewById(R.id.inputBus_ok);
        inputStation_ok = (Button)findViewById(R.id.inputStation_ok);

        laneInfo_layout = (LinearLayout)findViewById(R.id.laneInfo_layout);
        direction_btn_layout = (LinearLayout)findViewById(R.id.direction_btn_layout);
        direction1_btn = (Button)findViewById(R.id.direction1_btn);
        direction2_btn = (Button)findViewById(R.id.direction2_btn);

        sp_api.setSelection(0);

        odsayService = ODsayService.init(BusActivity.this, getString(R.string.odsay_key));
        odsayService.setReadTimeout(5000);
        odsayService.setConnectionTimeout(5000);

        bt_api_call.setOnClickListener(onClickListener);
        sp_api.setOnItemSelectedListener(onItemSelectedListener);
        voice_btn = findViewById(R.id.voice_btn);

        i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getBaseContext().getPackageName());
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
        mRecognizer = SpeechRecognizer.createSpeechRecognizer(getBaseContext());
        mRecognizer.setRecognitionListener(listener);

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
                    }
                }else {

                    // 작업 실패

                    Toast.makeText(getBaseContext(), "TTS 작업에 실패하였습니다.", Toast.LENGTH_SHORT).show();

                }
            }
        });

        //음성 검색 코드
        voice_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("-------------------------------------- 음성인식 시작!");
                if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(BusActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_RECORD_AUDIO);
                    //권한을 허용하지 않는 경우
                } else {
                    //권한을 허용한 경우
                    try {

                        if(listen_state == 0) {
                            mRecognizer.startListening(i);
                        }
                        if(listen_state == 1){
                            if(RESAULT_CALL_BACK_STATE == 2){//버스정류장 세부정보 조회에서 음성검색
                                String speak = search_name + " 글자가 포함된 정류장을 검색합니다.";
                                tts.speak(speak, QUEUE_FLUSH, null);
                                stationName = search_name;

                                ReceiveStationTask receiveStationTaskTask = new ReceiveStationTask();
                                receiveStationTaskTask.execute("");
                            }
                            else if(RESAULT_CALL_BACK_STATE == 3){//반경 내 검색 메뉴에서 음성검색
                                String speak = search_name + " 정류장에 대한 상세 정보입니다.";
                                tts.speak(speak, QUEUE_FLUSH, null);
                                searchInList(stations, search_name, context, cityCode);
                            }
                            listen_state = 0;
                        }
                    } catch(SecurityException e) {
                        e.printStackTrace();
                    }
                }

            }
        });

        //정류장 클릭 -> 실시간 도착정보 액티비티 시작
        stationList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String stationID =  stations.get(position).getLocal_id();
                String stationName = stations.get(position).getName();
                String speak = stationName + " " + stations.get(position).getId() +"정류장의 상세 정보입니다.";
                tts.speak(speak, QUEUE_FLUSH, null);
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
                //station List 초기화
                stations = new ArrayList<>();
                adapter = new StatAdapter(getApplicationContext(), stations);
                stationList.setAdapter(adapter);

                String bus = inputBus.getText().toString();
                RESAULT_CALL_BACK_STATE = 1;
                odsayService.requestSearchBusLane(bus, "3000", "no", "1", null, onResultCallbackListener);
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

        //방향 선택 후 상세 정보 호출
        direction1_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BUS_DIRECTION = 1;
                RESAULT_CALL_BACK_STATE = 4;
                odsayService.requestBusLaneDetail(BUS_ID, onResultCallbackListener);

            }
        });
        direction2_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BUS_DIRECTION = 2;
                RESAULT_CALL_BACK_STATE = 4;
                odsayService.requestBusLaneDetail(BUS_ID, onResultCallbackListener);
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
                case 1:     //버스 노선 조회
                    Log.d("laneinfo", jsonObject.toString());
                    String busID = "";
                    try {
                        JSONArray jsonArray = jsonObject.getJSONObject("result").getJSONArray("lane");
                        JSONObject jsonObject = jsonArray.getJSONObject(0);

                        String busNo = jsonObject.getString("busNo");
                        String StartPoint = jsonObject.getString("busStartPoint");
                        String EndPoint = jsonObject.getString("busEndPoint");
                        String FirstTime = jsonObject.getString("busFirstTime");
                        String LastTime = jsonObject.getString("busLastTime");
                        String busInterval = jsonObject.getString("busInterval");
                        String busInterval_Sat = jsonObject.getString("bus_Interval_Sat");
                        String busInterval_Sun = jsonObject.getString("bus_Interval_Sun");

                        busID = jsonObject.getString("busID");
                        Log.d("test", busNo);

                        BUS_ID = busID;
                        laneInfo.setText("버스 번호 : "+busNo+"\n"+"기점지 : "+StartPoint+"\n"+"종점지 : "+EndPoint);

                        laneInfo_layout.setVisibility(View.VISIBLE);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    break;
                case 2:     //Station

                    break;
                case 3:     //Location
                    break;

                case 4: //노선 전체 정류장 1 방향, 2 방향 있음
                    try {
                        Log.d("case4", jsonObject.toString());
                        JSONArray jsonArray = jsonObject.getJSONObject("result").getJSONArray("station");
                        stations = new ArrayList<>();
                        for(int i = 0; i<jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String stationDirection = jsonObject.getString("stationDirection");
                            if(BUS_DIRECTION == Integer.parseInt(stationDirection)) {
                                String stationName = jsonObject.getString("stationName");
                                String stationID = jsonObject.getString("stationID");
                                String localStationID = jsonObject.getString("localStationID");
                                String x = jsonObject.getString("x");
                                String y = jsonObject.getString("y");

                                stations.add(new Station(stationName, stationID, localStationID, x, y));
                            }

                            adapter = new StatAdapter(getApplicationContext(), stations);
                            stationList.setAdapter(adapter);
                            stationList.setVisibility(View.VISIBLE);
                        }
                        String start = stations.get(0).name;
                        String end = stations.get(stations.size()-1).name;
                        String speak = start + "에서 " + end + "로 가는 방향입니다.";
                        tts.speak(speak, QUEUE_FLUSH, null);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
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

            //station List 초기화 ( 반응이 느려서 자꾸 전에 리스트 값이 약간 보임 )
            stations = new ArrayList<>();
            adapter = new StatAdapter(getApplicationContext(), stations);
            stationList.setAdapter(adapter);

            switch (spinnerSelectedName) {
                case "버스노선 상세정보 조회":
                    inputStation_layout.setVisibility(View.GONE);stationList.setVisibility(View.GONE);
                    inputBus_layout.setVisibility(View.VISIBLE);
                    voice_btn.setVisibility(View.GONE);

                    RESAULT_CALL_BACK_STATE = 1;

                    break;
                case "버스정류장 세부정보 조회":
                    inputBus_layout.setVisibility(View.GONE);stationList.setVisibility(View.GONE);
                    inputStation_layout.setVisibility(View.VISIBLE); laneInfo_layout.setVisibility(View.GONE);
                    voice_btn.setVisibility(View.VISIBLE);

                    RESAULT_CALL_BACK_STATE = 2;

                    break;

                case "반경내 정류장 검색":
                    //반경 내 정류장 나타내는 View만 활성화
                    inputBus_layout.setVisibility(View.GONE);inputStation_layout.setVisibility(View.GONE);
                    voice_btn.setVisibility(View.VISIBLE);laneInfo_layout.setVisibility(View.GONE);
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
        protected void onPostExecute(final String result) {
            Log.d("postexe1", result);
            try {
                //파싱
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new InputSource(new StringReader(result)));

                NodeList nodeList = document.getElementsByTagName("item");

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
                    result_state = 2;
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
                    result_state = 3;
                }

                stationList.setVisibility(View.VISIBLE);
                Thread speech = new Thread() {
                    public void run() {
                        try {
                            ListSpeaking(stations, tts, result_state);
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

    private static void ListSpeaking(ArrayList<Station> stations, TextToSpeech tts, int state){
        int size = stations.size();
        String speak = "";
        int i = 0;
        speak = "총 " + size + "개의 정류장이 있습니다.";
        tts.speak(speak, QUEUE_FLUSH, null);
        while (true) {
            if (tts.isSpeaking() == false) {
                if(i < size){
                    speak = (i+1) + "번 " + stations.get(i).name + " " + stations.get(i).id;
                    tts.speak(speak, QUEUE_FLUSH, null);
                    tts.playSilence(1000, QUEUE_ADD, null);
                    i++;
                }else{
                    if(state == 2){
                        speak = "정류장별 상세 정보 검색은 리스트에서 정류장을 선택하세요.";
                        tts.speak(speak, QUEUE_FLUSH, null);
                    }else if(state == 3){
                        speak = "정류장별 상세 정보 검색은 리스트에서 정류장을 선택하거나 하단의 음성 검색 버튼을 이용하세요.";
                        tts.speak(speak, QUEUE_FLUSH, null);
                    }
                    break;
                }
            }else{
                continue;
            }
        }
    }

    //음성인식 listener
    private RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            System.out.println("onReadyForSpeech.........................");
        }
        @Override
        public void onBeginningOfSpeech() {
            Toast.makeText(getBaseContext(), "음성을 입력받고 있습니다!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            System.out.println("onRmsChanged.........................");
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            System.out.println("onBufferReceived.........................");
        }

        @Override
        public void onEndOfSpeech() {
            System.out.println("onEndOfSpeech.........................");
        }

        @Override
        public void onError(int error) {
            Toast.makeText(getBaseContext(), "천천히 다시 말해주세요.", Toast.LENGTH_SHORT).show();
            tts.speak("천천히 다시 말해주세요.", QUEUE_FLUSH, null);
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            System.out.println("onPartialResults.........................");
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            System.out.println("onEvent.........................");
        }

        @Override
        public void onResults(Bundle results) {
            if(listen_state == 1){
                input_voice = "";
                search_name = "";
                listen_state = 0;
            }
            else{
                String key= "";
                key = SpeechRecognizer.RESULTS_RECOGNITION;
                ArrayList<String> mResult = results.getStringArrayList(key);
                String[] rs = new String[mResult.size()];
                mResult.toArray(rs);
                //Toast.makeText(getBaseContext(), rs[0], Toast.LENGTH_SHORT).show();
                //음성인식 결과 (rs[0]) 를 전역변수 input_voice에 저장
                input_voice = rs[0];
                String[] tmp = input_voice.split(" ");
                search_name = "";
                for(int i = 0; i < tmp.length; i++){
                    search_name += tmp[i];
                }
                Toast.makeText(getBaseContext(), search_name, Toast.LENGTH_SHORT).show();
                listen_state = 1;
                tts.speak("다시 한번 음성 검색 메뉴를 눌러주세요.", QUEUE_FLUSH, null);
                //  mRecognizer.startListening(i); //음성인식이 계속 되는 구문이니 필요에 맞게 쓰시길 바람
            }
        }
    };

    // 반경 내 정류장 검색 메뉴에서 음성 검색 버튼을 눌렀을 때
    // 정류장 리스트에서 일치하는 정류장 이름을 찾으면 해당 결과의 정보를 나타냄
    private void searchInList(ArrayList<Station> stations, String search_name, Context context, String cityCode){
        int index = -1;
        for(int i = 0; i < stations.size(); i++){
            if(search_name.equals(stations.get(i).name)){
                index = i;
                break;
            }
        }
        if(index != -1){
            String stationID = stations.get(index).getLocal_id();
            String stationName = stations.get(index).getName();

            Intent intent = new Intent(context, RealTimeStationInfo.class);

            intent.putExtra("name", stationName);
            intent.putExtra("id", stationID);
            intent.putExtra("cityCode", cityCode);
            startActivity(intent);
        }
    }

    @Override
    public void onBackPressed() {
        if (tts.isSpeaking() == true){
            tts.shutdown();
        }
        super.onBackPressed();
    }
}
