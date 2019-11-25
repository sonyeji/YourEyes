package org.YourEyes.demo;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.odsay.odsayandroidsdk.API;
import com.odsay.odsayandroidsdk.ODsayData;
import com.odsay.odsayandroidsdk.ODsayService;
import com.odsay.odsayandroidsdk.OnResultCallbackListener;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_ADD;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

public class RealTimeStationInfo extends Activity {
    private ArrayList<busArr> busArrArrayList;
    private String stationId;
    private ODsayService odsayService;
    private JSONObject jsonObject;
    private String cityCode;
    private String localStationID;
    private BusAdapter adapter;

    private ListView buslist;

    private TextToSpeech tts;
    public Context mContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getBaseContext();

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.realtimestationinfo);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.bus_titlebar);

      //  stationId = getIntent().getExtras().getString("id");
        localStationID = getIntent().getExtras().getString("id");
        cityCode = getIntent().getExtras().getString("cityCode");

        buslist = (ListView)findViewById(R.id.buslist);
        odsayService = ODsayService.init(RealTimeStationInfo.this, getString(R.string.odsay_key));
        odsayService.setReadTimeout(5000);
        odsayService.setConnectionTimeout(5000);

        busArrArrayList = new ArrayList<>();

       // getStationId(stationId);
        ReceiveBusTask receiveBusTask = new ReceiveBusTask();
        receiveBusTask.execute("");


        tts = new TextToSpeech(mContext, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != ERROR) {
                    int language = tts.setLanguage(Locale.KOREAN);
                    if (language == TextToSpeech.LANG_MISSING_DATA

                            || language == TextToSpeech.LANG_NOT_SUPPORTED) {

                        // 언어 데이터가 없거나, 지원하지 않는경우

                        Toast.makeText(mContext, "지원하지 않는 언어입니다.", Toast.LENGTH_SHORT).show();
                    }
                    else {

                    }
                }else {

                    // 작업 실패

                    Toast.makeText(mContext, "TTS 작업에 실패하였습니다.", Toast.LENGTH_SHORT).show();

                }
            }
        });

    }

    // API 용 버스 ID 구하기
    private void getStationId(String id) {
        odsayService.requestBusStationInfo(stationId, onResultCallbackListener);
    }
    private OnResultCallbackListener onResultCallbackListener = new OnResultCallbackListener() {
        @Override
        public void onSuccess(ODsayData oDsayData, API api) {
            jsonObject = oDsayData.getJson();
            try {
                localStationID = jsonObject.getJSONObject("result").getString("localStationID");
                Log.d("id", localStationID);
                ReceiveBusTask receiveBusTask = new ReceiveBusTask();
                receiveBusTask.execute("");
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }

        @Override
        public void onError(int i, String errorMessage, API api) {

        }
    };

    //실시간 버스 도착 정보
    private class ReceiveBusTask extends AsyncTask<String, Void, String> {
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
                String URL = "http://openapi.tago.go.kr/openapi/service/ArvlInfoInqireService/getSttnAcctoArvlPrearngeInfoList?ServiceKey="+SERVICE_KEY;
                StringBuilder urlBuilder = new StringBuilder(URL);
                urlBuilder.append("&cityCode="+cityCode);
                urlBuilder.append("&nodeId="+localStationID);
                Log.d("id", localStationID);
                java.net.URL url = new URL(urlBuilder.toString());
                InputStream is= url.openStream(); //url위치로 입력스트림 연결
                InputStreamReader reader = new InputStreamReader(is);
                BufferedReader in = new BufferedReader(reader);

                while((readed = in.readLine()) != null)
                    return readed;

            } catch (Exception e) {

            }
            return "";
        }

        //파싱
        @Override
        protected void onPostExecute(String result) {
            Log.d("postexe1", result);
            try {

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new InputSource(new StringReader(result)));

                NodeList nodeList = document.getElementsByTagName("item");
                for(int i = 0; i<nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);

                    if(node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element)node;
                        int prevstationcnt = Integer.parseInt(getTagValue("arrprevstationcnt", element));
                        int arrtime = Integer.parseInt(getTagValue("arrtime", element));
                        String routeid = getTagValue("routeid", element);
                        String routeno = getTagValue("routeno", element);
                        String routetp = getTagValue("routetp", element);
                        String vehicletp = getTagValue("vehicletp", element);

                        busArrArrayList.add(new busArr(arrtime, prevstationcnt, routeid, routeno, routetp, vehicletp));
                        Log.d("businfo", busArrArrayList.get(i).toString());
                    }
                }
                adapter = new BusAdapter(getApplicationContext(), busArrArrayList);
                buslist.setAdapter(adapter);

                Thread speech = new Thread() {
                    public void run() {
                        try {
                            ListSpeaking(busArrArrayList, tts);
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                speech.start();
                //textView.setText(buffer.toString());
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

    //버스 도착 정보
    class busArr {
        int arrtime;                    //남은 시간
        int prestationcnt;             //전 정류장 개수
        String routeid;                 //버스 id
        String routeno;                 //버스 번호
        String routetp;                 //급행 or 일반
        String vehicletp;               //일반 or 저상

        public busArr(int arrtime, int prestationcnt, String routeid, String routeno, String routetp, String vehicletp) {
            this.arrtime = arrtime/60;
            this.prestationcnt = prestationcnt;
            this.routeid = routeid;
            this.routetp = routetp;
            this.routeno = routeno;
            this.vehicletp = vehicletp;
        }
        public int getArrtime() {
            return this.arrtime;
        }
        public int getPrestationcnt() {
            return this.prestationcnt;
        }
        public String getRouteid() {
            return this.routeid;
        }
        public String getRoutetp() {
            return this.routetp;
        }
        public String getVehicletp() {
            return this.vehicletp;
        }
        public String getRouteno() {
            return this.routeno;
        }
        @Override
        public String toString() {
            return this.routeno+"번 "+this.arrtime+"분 후 도착"+" "+prestationcnt+"정거장 전";
        }
    }

    public static void ListSpeaking(ArrayList<busArr> list, TextToSpeech tts){
        int size = list.size();
        String speak = "";
        int i = 0;
        speak = "총 " + size + "개의 버스 정보가 있습니다.\n";
        tts.speak(speak, QUEUE_FLUSH, null);

        for(int j = 0; j < size; j++){
            speak += list.get(j).getRouteno() + "번" + list.get(j).getArrtime() + "분 후 도착예정\n";
        }
        tts.speak(speak, QUEUE_FLUSH, null);
        /*
        while (true) {
            if (tts.isSpeaking() == false) {
                if(i < size){
                    speak = list.get(i).getRouteno() + "번" + list.get(i).getArrtime() + "분 후 도착예정";
                    tts.speak(speak, QUEUE_FLUSH, null);
                    tts.playSilence(1000, QUEUE_ADD, null);
                    i++;
                }else{
                    break;
                }
            }else{
                continue;
            }
        }
        */
    }

    @Override
    public void onBackPressed() {
        if (tts.isSpeaking() == true){
            tts.shutdown();
        }
        super.onBackPressed();
    }
}
