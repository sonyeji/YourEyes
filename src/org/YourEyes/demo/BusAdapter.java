package org.YourEyes.demo;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_ADD;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

public class BusAdapter extends BaseAdapter {
    Context mContext = null;
    LayoutInflater mLayoutInfalter = null;
    ArrayList<RealTimeStationInfo.busArr> list;

    private TextView busNo;
    private TextView time;
    private TextView preStation;
    private TextToSpeech tts;


    public BusAdapter(Context context, ArrayList<RealTimeStationInfo.busArr> data) {
        mContext = context;
        list = data;
        mLayoutInfalter = LayoutInflater.from(mContext);

        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
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
                        ListSpeaking(list, tts);
                    }
                }else {

                    // 작업 실패

                    Toast.makeText(mContext, "TTS 작업에 실패하였습니다.", Toast.LENGTH_SHORT).show();

                }
            }
        });

    }

    @Override
    public int getCount() {
        return list.size();
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public RealTimeStationInfo.busArr getItem(int position) {
        return list.get(position);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.bus_item, null);

            busNo = (TextView)convertView.findViewById(R.id.busNo);
            time = (TextView)convertView.findViewById(R.id.time);
            preStation = (TextView)convertView.findViewById(R.id.preStation);
        }



        busNo.setText(list.get(position).getRouteno()+"번 ");
        time.setText(list.get(position).getArrtime()+"분 후 도착예정 ");
        preStation.setText(list.get(position).getPrestationcnt()+"정거장 전");
        return convertView;
    }

    public static void ListSpeaking(ArrayList<RealTimeStationInfo.busArr> list, TextToSpeech tts){
        int size = list.size();
        String speak = "";
        int i = 0;
        speak = "총 " + size + "개의 버스 정보가 있습니다.";
        tts.speak(speak, QUEUE_FLUSH, null);
        while (true) {
            if (tts.isSpeaking() == false) {
                if(i < size){
                    speak = list.get(i).getRouteno() + "번" + list.get(i).getArrtime() + "분 후도착";
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
    }
}
