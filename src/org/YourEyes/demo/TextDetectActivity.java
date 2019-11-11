package org.YourEyes.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.YourEyes.demo.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_ADD;

public class TextDetectActivity extends Activity implements View.OnClickListener{
    private boolean CameraOnOffFlag = true;
    private Context mContext;
    private Button m_btnOCR; // 인식하기 위해 사진찍는 버튼
    private TextView m_ocrTextView; // 결과 변환 텍스트
    private ImageView m_ivImage; // 찍은 사진
    private Bitmap image; //사용되는 이미지
    private TextView m_tvTime; // 처리시간 표시 텍스트

    private TessBaseAPI m_Tess; //Tess API reference
    private ProgressCircleDialog m_objProgressCircle = null; // 원형 프로그레스바
    private MessageHandler m_messageHandler;

    private long m_start; // 처리시간 시작지점
    private long m_end; //처리시간 끝지점
    private String mDataPath = ""; //언어데이터가 있는 경로
    private String mCurrentPhotoPath; // 사진 경로
    private final String[] mLanguageList = {"eng","kor"}; // 언어
    private boolean ProgressFlag = false;
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.textdetect_activity);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.textdetect_titlebar);

        mContext = this;

        m_ivImage = (ImageView) findViewById(R.id.iv_image);
        m_ocrTextView = (TextView) findViewById(R.id.tv_view);
        m_tvTime = (TextView) findViewById(R.id.tv_time);
        m_btnOCR = (Button) findViewById(R.id.btn_OCR);

        m_btnOCR.setOnClickListener(this);

        m_objProgressCircle = new ProgressCircleDialog(this);
        m_messageHandler = new MessageHandler();


        if(CameraOnOffFlag)
        {
            PermissionCheck();
            Tesseract();
        }
        else
        {
            //이미지 디코딩을 위한 초기화
            /*image = BitmapFactory.decodeResource(getResources(), R.drawable.sampledata); //샘플이미지파일
            Test();*/
        }

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
                        String speak = "글자를 인식하려면 화면 하단에 있는 사진 찍기 버튼을 누른 후 글자를 촬영해주시기 바랍니다.";
                        tts.speak(speak, TextToSpeech.QUEUE_FLUSH, null);
                    }
                }else {

                    // 작업 실패

                    Toast.makeText(mContext, "TTS 작업에 실패하였습니다.", Toast.LENGTH_SHORT).show();

                }
            }
        });
    }

    public void PermissionCheck() {
        /**
         * 6.0 마시멜로우 이상일 경우에는 권한 체크후 권한을 요청한다.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED &&
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                // 권한 없음
                ActivityCompat.requestPermissions(TextDetectActivity.this,
                        new String[]{Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        ConstantDefine.PERMISSION_CODE);
            } else {
                // 권한 있음
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // 카메라를 찍기위해 카메라 앱을 연다
            case R.id.btn_OCR:
                if(CameraOnOffFlag)
                {
                    dispatchTakePictureIntent();
                }
                else
                {
                    m_start = System.currentTimeMillis();
                    processImage(v);
                }
                m_tvTime.setText("처리시간");
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ConstantDefine.PERMISSION_CODE:
                Toast.makeText(this, "권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
                break;
            case ConstantDefine.ACT_TAKE_PIC:
                //카메라로 찍은 사진을 받는다.
                if ((resultCode == RESULT_OK) ) {

                    try {
                        m_start = System.currentTimeMillis();
                        File file = new File(mCurrentPhotoPath);
                        Bitmap rotatedBitmap = null;
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),
                                FileProvider.getUriForFile(TextDetectActivity.this,
                                        getApplicationContext().getPackageName() + ".fileprovider", file));

                        // 회전된 사진을 원래대로 돌려 표시한다.
                        if (bitmap != null) {
                            ExifInterface ei = new ExifInterface(mCurrentPhotoPath);
                            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                                    ExifInterface.ORIENTATION_UNDEFINED);
                            switch (orientation) {

                                case ExifInterface.ORIENTATION_ROTATE_90:
                                    rotatedBitmap = rotateImage(bitmap, 90);
                                    break;

                                case ExifInterface.ORIENTATION_ROTATE_180:
                                    rotatedBitmap = rotateImage(bitmap, 180);
                                    break;

                                case ExifInterface.ORIENTATION_ROTATE_270:
                                    rotatedBitmap = rotateImage(bitmap, 270);
                                    break;

                                case ExifInterface.ORIENTATION_NORMAL:
                                default:
                                    rotatedBitmap = bitmap;
                            }
                            OCRThread ocrThread = new OCRThread(rotatedBitmap);
                            ocrThread.setDaemon(true);
                            ocrThread.start();
                            m_ivImage.setImageBitmap(rotatedBitmap);// 카메라로 찍은 사진을 뷰에 표시한다.
                            m_ocrTextView.setText("로딩중"); //인식된텍스트 표시
                        }
                    } catch (Exception e) {
                    }
                }
                break;
        }
    }
    // 이미지를 원본과 같게 회전시킨다.
    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Log.d("authority", this.getApplicationContext().getPackageName());
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // 사진파일을 생성한다.
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // 사진파일이 정상적으로 생성되었을때
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        this.getApplicationContext().getPackageName()+".fileprovider",
                        photoFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, ConstantDefine.ACT_TAKE_PIC);
            }
        }
    }
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public void Tesseract() {
        //언어파일 경로
        mDataPath = getFilesDir() + "/tesseract/";

        //트레이닝데이터가 카피되어 있는지 체크
        String lang = "";
        for (String Language : mLanguageList) {
            checkFile(new File(mDataPath + "tessdata/"), Language);
            lang += Language + "+";
        }
        m_Tess = new TessBaseAPI();
        m_Tess.init(mDataPath, lang);
    }
    private void checkFile(File dir, String Language) {
        //디렉토리가 없으면 디렉토리를 만들고 그후에 파일을 카피
        if (!dir.exists() && dir.mkdirs()) {
            copyFiles(Language);
        }
        //디렉토리가 있지만 파일이 없으면 파일카피 진행
        if (dir.exists()) {
            String datafilepath = mDataPath + "tessdata/" + Language + ".traineddata";
            File datafile = new File(datafilepath);
            if (!datafile.exists()) {
                copyFiles(Language);
            }
        }
    }
    private void copyFiles(String Language) {
        try {
            String filepath = mDataPath + "/tessdata/" + Language + ".traineddata";
            AssetManager assetManager = getAssets();
            InputStream instream = assetManager.open("tessdata/"+Language+".traineddata");
            OutputStream outstream = new FileOutputStream(filepath);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public class MessageHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what)
            {
                case ConstantDefine.RESULT_OCR:
                    TextView OCRTextView = (TextView)findViewById(R.id.tv_view);
                    OCRTextView.setText(String.valueOf(msg.obj)); //텍스트 변경

                    String message = String.valueOf(msg.obj);
                    tts.speak(message, TextToSpeech.QUEUE_FLUSH, null);

                    // 원형 프로그레스바 종료
                    if(m_objProgressCircle.isShowing() && m_objProgressCircle !=null)
                        m_objProgressCircle.dismiss();
                    ProgressFlag = false;
                    m_end = System.currentTimeMillis();
                    long time = (m_end - m_start)/1000;
                    m_tvTime.setText("처리시간 : "+time+"초");
                    Toast.makeText(mContext,"종료",Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    public class OCRThread extends Thread
    {
        private Bitmap rotatedImage;
        OCRThread(Bitmap rotatedImage)
        {
            this.rotatedImage = rotatedImage;
            if(!ProgressFlag)
                m_objProgressCircle = ProgressCircleDialog.show(mContext, "", "", true);
            ProgressFlag = true;
        }

        @Override
        public void run() {
            super.run();
            // 사진의 글자를 인식해서 옮긴다
            String speak = "글자를 인식하는 중 입니다. 잠시만 기다려주세요.";
            tts.speak(speak, TextToSpeech.QUEUE_FLUSH, null);
            String OCRresult = null;
            m_Tess.setImage(rotatedImage);
            OCRresult = m_Tess.getUTF8Text();

            Message message = Message.obtain();
            message.what = ConstantDefine.RESULT_OCR;
            message.obj = OCRresult;
            m_messageHandler.sendMessage(message);

        }
    }
    //Process an Image
    public void processImage(View view) {
        OCRThread ocrThread = new OCRThread(image);
        ocrThread.setDaemon(true);
        ocrThread.start();
    }

}
