package org.tensorflow.demo;

import android.Manifest;
import android.app.Activity;
//import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BusActivity extends Activity {
     private final String TAG = "myTag";
    private final String key = "ct2dNoWeWzFyGXmO6DUc6%2F3BLuTs8Q8N5X3TEru2OfaDZlPiPaOo1ziG018HK8EVot6EzrBX%2BfxcfHu0Xflqxw%3D%3D";
    private final String endPoint = "http://openapitraffic.daejeon.go.kr/api/rest/busposinfo\n";

    //xml 변수
    private EditText xmlBusNum;
    private EditText xmlStationArsno;
    private TextView xmlShowInfo;

    // 파싱을 위한 필드 선언
    private URL url;
    private InputStream is;
    private XmlPullParserFactory factory;
    private XmlPullParser xpp;
    private String tag;
    private int eventType;

    // xml의 값 입력 변수
    private String busNum; // 버스 번호
    private String stationArsno = ""; //출발 정류장 arsNo
    private StringBuffer buffer;
    // 데이터 검색
    private String busNumId; // 버스 번호 Id
    private String stationId;// 출발 정류소명 Id
    private String sStationArriveTime; // 버스의 정류장 도착정보

    private String car1;
    private String min1;
    private String station1;
    private String car2;
    private String min2;
    private String station2;
    
    
    private Button detectbus_btn;

    private Button voicesearch_btn;
    private SpeechRecognizer mRecognizer;
    private Intent i;
    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bus_activity);
        detectbus_btn = (Button)findViewById(R.id.detect_bus);
        voicesearch_btn = (Button)findViewById(R.id.voice_search);

        i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getBaseContext().getPackageName());
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
        mRecognizer = SpeechRecognizer.createSpeechRecognizer(getBaseContext());
        mRecognizer.setRecognitionListener(listener);


        voicesearch_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                startActivity(new Intent(getBaseContext(), SttTest.class));
            }
        });

                //상태바 없애기(FullScreen)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.bus_activity);

        //xml 아이디 얻어오기
        getXmlId();
        buffer = new StringBuffer();

    }
        //검색하기 onclick버튼
    public void search(View view) {
        //사용자한테 출발정류장, 도착정류장 알아오기.
        busNum = xmlBusNum.getText().toString();
        stationArsno = xmlStationArsno.getText().toString();
        car1 = min1 = station1 = car2 = min2 = station2 = null;
        buffer = null;
        buffer = new StringBuffer();
        xmlShowInfo.setText("");

        //입력값 검사 함수
        if(exmineData()) {
            // 입력값 검사 함수에서 true를 return할 경우 값이 잘못된 것..
            // 종료..
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                // 검색한 버스 id 얻기
                // 오퍼레이션 2
                getBusId(busNum);

                //검색한 정류장 얻기
                //오퍼레이션 1
                getStationId(stationArsno);

                //버스가 언제오는지 확인
                //오퍼레이션 5
                userWant(busNumId, stationId);

                // UI setText 하는 곳..
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, car1 + " " + min1 + " " + station1);
                        Log.d(TAG, car2 + " " + min2 + " " + station2);
                        if(car1 == null) {
                            buffer.append("도착 정보 없음");
                        } else {
                            buffer.append("첫번째 차량 도착 정보\n");
                            buffer.append("차량 번호 : " + car1 + " \n");
                            buffer.append("남은 시간 : " + min1 + " 분 \n");
                            buffer.append("남은 구간 : " + station1 + "정거장\n");
                        }
                        // 두번째 도착 차량은 null이 아닐 경우에만 출력
                        if(car2 != null) {
                            buffer.append("-------------------------\n");
                            buffer.append("두번째 차량 도착 정보\n");
                            buffer.append("차량 번호 : " + car2 + " \n");
                            buffer.append("남은 시간 : " + min2 + "분 \n");
                            buffer.append("남은 구간 : " + station2 + "정거장 \n");
                        }
                        xmlShowInfo.setText(buffer.toString());
                    }
                });
            }
        }).start();
    }

    //정류소명을 입력하면 정류장 ID를 돌려줌
    /*
     * 오퍼레이션 1
     * 정류소명, 정류소ARS번호를 기준으로 정류소ID, 정류소명, GPS좌표, 정류소구분(일반, 마을)을
     * 조회하는 정류소정보 조회 서비스
     */
    public void getStationId(String station) {
        String stationUrl = endPoint + "getArrInfoByStopID?busRouteId=" + station + "&serviceKey=" + key;
        Log.d(TAG, "정류장명 -> 정류장Id : " + stationUrl);

        try {
            setUrlNParser(stationUrl);

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        tag = xpp.getName();

                        if (tag.equals("itemList")) ; //첫번째 검색 결과
                        else if (tag.equals("BUS_NODE_ID")) {
                            xpp.next();
                            stationId = xpp.getText();
                        } else if (tag.equals("BUS_STOP_ID")) ;
                        else if (tag.equals("GPS_LATI")) ;
                        else if (tag.equals("GPS_LONG")) ;
                        else if (tag.equals("ROUTE_CD")) ;
                        else if (tag.equals("STRE_DT")) ;
                        break;
                    case XmlPullParser.TEXT:
                        break;
                    case XmlPullParser.END_TAG:
                        tag = xpp.getName();
                        if (tag.equals("itemList")); // 첫번째 검색 결과 종료.. 줄바꿈
                        break;
                } //end of switch~case

                eventType = xpp.next();
            } //end of while
        } catch (Exception e) {
            e.printStackTrace();
        }
        //return buffer.toString(); //정류장 이름에 해당하는 id를 넘겨줌
    }

    //버스 번호를 입력하면 버스 ID를 돌려줌
    /*
     * 오퍼레이션 2
     * 노선ID, 노선번호를 기준으로 버스종류, 회사이름, 출/도착지, 첫/막차시간, 배차간격을 조회하는 노선정보 조회 서비스
     */
    public void getBusId(String busNum) {
        String busNumUrl = endPoint + "/busInfo?lineno=" + busNum + "&serviceKey=" + key;
        Log.d(TAG,"버스번호 -> 버스id : " + busNumUrl);

        try {
            setUrlNParser(busNumUrl);

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        tag = xpp.getName();

                        if (tag.equals("itemList")) ; //첫번째 검색 결과
                        else if (tag.equals("lineId")) {
                            xpp.next();
                            busNumId = xpp.getText();
                        }
                        else if (tag.equals("buslinenum")) ;
                        else if (tag.equals("bustype")) ;
                        else if (tag.equals("companyid")) ;
                        else if (tag.equals("endpoint")) ;
                        else if (tag.equals("stoptype")) ;
                        else if (tag.equals("firsttime")) ;
                        else if (tag.equals("endtime")) ;
                        else if (tag.equals("headway")) ;
                        else if (tag.equals("headwayNorm")) ;
                        else if (tag.equals("headwayPeak")) ;
                        else if (tag.equals("headwayHoli")) ;
                        break;
                    case XmlPullParser.TEXT:
                        break;
                    case XmlPullParser.END_TAG:
                        tag = xpp.getName();
                        if (tag.equals("itemList")); // 첫번째 검색 결과 종료.. 줄바꿈
                        break;
                } //end of switch~case
                eventType = xpp.next();
            } //end of while
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * 오퍼레이션 5
     * 정류소 ID, 노선 ID를 기준으로 실시간 도착정보인 차량번호, 남은 도착시간, 남은 정류장 수
     * 저상버스유무를 인접버스 두 대에 대해 조회하는 노선 정류소 도착정보 조회 서비스
     */
    public void userWant(String busNumId, String stationId) {
        String dataUrl = endPoint + "/busStopArr?bstopid=" + stationId + "&lineid=" + busNumId + "&serviceKey=" + key;
        Log.d(TAG, dataUrl);

        try {
            setUrlNParser(dataUrl);

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        tag = xpp.getName();

                        if (tag.equals("itemList")) ; //첫번째 검색 결과
                        else if (tag.equals("carNo1")) {
                            xpp.next();
                            car1 = xpp.getText();
                        } else if (tag.equals("min1")) {
                            xpp.next();
                            min1 = xpp.getText();
                        } else if (tag.equals("station1")) {
                            xpp.next();
                            station1 = xpp.getText();
                        } else if (tag.equals("carNo2")) {
                            xpp.next();
                            car2 = xpp.getText();
                        } else if (tag.equals("min2")) {
                            xpp.next();
                            min2 = xpp.getText();
                        } else if (tag.equals("station2")) {
                            xpp.next();
                            station2 = xpp.getText();
                        }else if (tag.equals("bstopId")) ;
                        else if (tag.equals("nodeNm")) ;
                        else if (tag.equals("companyid")) ;
                        else if (tag.equals("gpsX")) ;
                        else if (tag.equals("gpsY")) ;
                        else if (tag.equals("bustype")) ;
                        else if (tag.equals("lineid")) ;
                        else if (tag.equals("bstopidx")) ;
                        break;
                    case XmlPullParser.TEXT:
                        break;
                    case XmlPullParser.END_TAG:
                        tag = xpp.getName();
                        if (tag.equals("itemList")); // 첫번째 검색 결과 종료.. 줄바꿈
                        break;
                } //end of switch~case
                eventType = xpp.next();
            } //end of while
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 사용자가 입력한 값을 검사하는 함수
    public boolean exmineData() {

        // 사용자가 하나 이상의 값을 입력하지 않은 경우
        if (busNum.equals("") || stationArsno.equals("")) {
            Toast.makeText(this, "값을 입력해주세요!", Toast.LENGTH_SHORT).show();
            return true;
        }

        //String[] arr = new String[] {busNum, stationArsno};
        String regExp = "([0-9])"; // 입력값은 반드시 숫자여야하므로 정규 표현식으로 설정
        Pattern pattern_symbol = Pattern.compile(regExp);

        //버스 번호 유효성 검사
        Matcher matcher_busNum = pattern_symbol.matcher(busNum); // 입력값이 유효하다면 true return
        if(matcher_busNum.find() == false) {
            Toast.makeText(this, "버스 번호를 다시 입력해주세요!", Toast.LENGTH_SHORT).show();
            return true;
        }
        //정류장 번호 유효성 검사
        Matcher matcher_stationArsno = pattern_symbol.matcher(stationArsno); // 입력값이 유효하다면 true return
        if(matcher_stationArsno.find() == false) {
            Toast.makeText(this, "정류장 번호를 다시 입력해주세요!", Toast.LENGTH_SHORT).show();
            return true;
        }

        return false; //모든 값이 정상
    }

    // Url, XmlPullParser 객체 생성 및 초기화
    public void setUrlNParser(String quary) {
        try {
            url = new URL(quary); //문자열로 된 요청 url을 URL객체로 생성
            is = url.openStream();

            factory = XmlPullParserFactory.newInstance();
            xpp = factory.newPullParser();
            xpp.setInput(new InputStreamReader(is, "UTF-8")); //inputStream으로부터 xml입력받기

            xpp.next();
            eventType = xpp.getEventType();
        } catch (Exception e) {

        }

    }

    // UI ID 얻는 함수
    public void getXmlId() {
        xmlBusNum = (EditText) findViewById(R.id.busNum);
        xmlStationArsno = (EditText) findViewById(R.id.stationArsno);
        xmlShowInfo = (TextView) findViewById(R.id.showInfo);
    }

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
            String key= "";
            key = SpeechRecognizer.RESULTS_RECOGNITION;
            ArrayList<String> mResult = results.getStringArrayList(key);
            String[] rs = new String[mResult.size()];
            mResult.toArray(rs);
            Toast.makeText(getBaseContext(), rs[0], Toast.LENGTH_SHORT).show();
            //  mRecognizer.startListening(i); //음성인식이 계속 되는 구문이니 필요에 맞게 쓰시길 바람
        }
    };
    
    
}
