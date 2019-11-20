package org.YourEyes.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

public class informationActivity extends Activity{
    ImageView img;
    private SpeechRecognizer mRecognizer;
    private Intent i;
    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;
    private TextToSpeech tts;
    private Context context;
    public String search_name = "";
    public int listen_state = 0;
    public String input_voice = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.information);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.information_titlebar);
        img = (ImageView) findViewById(R.id.appinfo);
        context = getBaseContext();

        i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getBaseContext().getPackageName());
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
        mRecognizer = SpeechRecognizer.createSpeechRecognizer(getBaseContext());
        mRecognizer.setRecognitionListener(listener);

        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != ERROR) {
                    int language = tts.setLanguage(Locale.KOREAN);
                    if (language == TextToSpeech.LANG_MISSING_DATA

                            || language == TextToSpeech.LANG_NOT_SUPPORTED) {

                        // 언어 데이터가 없거나, 지원하지 않는경우

                        Toast.makeText(context, "지원하지 않는 언어입니다.", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        tts.speak("설명을 원하시는 기능이 있다면 화면을 터치한 후 물체인식, 글자인식, 버스안내 중 원하는 것을 말하시기 바랍니다.", TextToSpeech.QUEUE_FLUSH, null);
                    }
                }else {

                    // 작업 실패

                    Toast.makeText(context, "TTS 작업에 실패하였습니다.", Toast.LENGTH_SHORT).show();

                }
            }
        });

        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("-------------------------------------- 음성인식 시작!");
                if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(informationActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_RECORD_AUDIO);
                    //권한을 허용하지 않는 경우
                } else {
                    //권한을 허용한 경우
                    try {
                        if(listen_state == 0) {
                            mRecognizer.startListening(i);
                        }
                        if(listen_state == 1){
                            if(search_name.equals("물체인식")){
                                String speak = "물체인식을 하시려면 카메라 권한을 허용 후 전방을 카메라로 비추세요. 전방에 보이는 것을 안내해드립니다.";
                                tts.speak(speak, QUEUE_FLUSH, null);
                            }
                            else if(search_name.equals("글자인식")){
                                String speak = "글자인식을 하시려면 카메라 권한을 허용 후 인식을 원하는 부분을 카메라로 촬영해주세요. 인식 후 음성으로 안내해드립니다.";
                                tts.speak(speak, QUEUE_FLUSH, null);
                            }
                            else if(search_name.equals("버스안내")){
                                String speak = "버스 안내는 버스노선 상세정보 조회와 버스 정류장 검색, 반경내 정류장 검색 기능이 있습니다. 각 메뉴를 선택 후 직접 검색 혹은 음성 검색을 할 수 있습니다.";
                                tts.speak(speak, QUEUE_FLUSH, null);
                            }
                            listen_state = 0;
                        }
                    } catch(SecurityException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

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
            input_voice = rs[0];
            String[] tmp = input_voice.split(" ");
            search_name = "";
            for(int i = 0; i < tmp.length; i++){
                search_name += tmp[i];
            }
            Toast.makeText(getBaseContext(), search_name, Toast.LENGTH_SHORT).show();
            listen_state = 1;
            tts.speak("다시 한번 화면을 터치해주세요.", QUEUE_FLUSH, null);
        }
    };
}
