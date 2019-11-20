package org.YourEyes.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
//import android.view.WindowManager;
//import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import org.YourEyes.demo.R;

import java.util.Locale;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

public class MenuActivity extends Activity {
    private ImageButton detect_btn;
    private ImageButton text_btn;
    private ImageButton bus_btn;
    private ImageButton information_btn;

    private TextToSpeech tts;
    private Context context;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_activity);

        detect_btn = (ImageButton)findViewById(R.id.detect_btn);
        text_btn = (ImageButton)findViewById(R.id.text_btn);
        bus_btn = (ImageButton)findViewById(R.id.bus_btn);
        information_btn = (ImageButton)findViewById(R.id.information_btn);

        context = getBaseContext();

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
                    }
                }else {

                    // 작업 실패

                    Toast.makeText(context, "TTS 작업에 실패하였습니다.", Toast.LENGTH_SHORT).show();

                }
            }
        });

        detect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tts.speak("물체인식 메뉴입니다.", QUEUE_FLUSH, null);
                startActivity(new Intent(getBaseContext(), DetectorActivity.class));
            }
        });
        text_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tts.speak("글자인식 메뉴입니다.", QUEUE_FLUSH, null);
                startActivity(new Intent(getBaseContext(), TextDetectActivity.class));
            }
        });
        bus_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tts.speak("버스안내 메뉴입니다.", QUEUE_FLUSH, null);
                startActivity(new Intent(getBaseContext(), BusActivity.class));
            }
        });
        information_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tts.speak("어플정보 메뉴입니다.", QUEUE_FLUSH, null);
                startActivity(new Intent(getBaseContext(), informationActivity.class));
            }
        });
    }
}
