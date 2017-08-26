package com.company.yyj.soundreco;

import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.naver.speech.clientapi.SpeechRecognitionResult;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class SpeechFragment extends Fragment {
    private static final String TAG = SpeechFragment.class.getSimpleName();
    private static final String CLIENT_ID = "O9QeYaNYfSKP3Xp04WEO";

    TextView cdsText;

    public MediaPlayer mp;

    MainActivity owner;

    // 1. "내 애플리케이션"에서 Client ID를 확인해서 이곳에 적어주세요.
    // 2. build.gradle (Module:app)에서 패키지명을 실제 개발자센터 애플리케이션 설정의 '안드로이드 앱 패키지 이름'으로 바꿔 주세요

    private RecognitionHandler handler;
    private NaverRecognizer naverRecognizer;

    private TextView txtResult;
    public static ImageView btnStart;
    private String mResult;

    private AudioWriterPCM writer;

    // Handle speech recognition Messages.
    private void handleMessage(Message msg) {
        switch (msg.what) {
            case R.id.clientReady:
                // Now an user can speak.
                txtResult.setText("Connected");
                writer = new AudioWriterPCM(
                        Environment.getExternalStorageDirectory().getAbsolutePath() + "/NaverSpeechTest");
                writer.open("Test");
                break;

            case R.id.audioRecording:
                writer.write((short[]) msg.obj);
                break;

            case R.id.partialResult:
                // Extract obj property typed with String.
                mResult = (String) (msg.obj);
                txtResult.setText(mResult);
                break;

            case R.id.finalResult:
                // Extract obj property typed with String array.
                // The first element is recognition result for speech.
                SpeechRecognitionResult speechRecognitionResult = (SpeechRecognitionResult) msg.obj;
                List<String> results = speechRecognitionResult.getResults();
                List<String> uniqueItems = new ArrayList<>(new HashSet<>(results));
                processBySound(uniqueItems);
                Log.e("items", uniqueItems.toString());

                if (uniqueItems.size() != 0) {
                    mp.start();
                }

                break;

            case R.id.recognitionError:
                if (writer != null) {
                    writer.close();
                }

                mResult = "Error code : " + msg.obj.toString();
                txtResult.setText(mResult);
                btnStart.setImageResource(R.drawable.microphone);
                btnStart.setEnabled(true);
                break;

            case R.id.clientInactive:
                if (writer != null) {
                    writer.close();
                }

                btnStart.setImageResource(R.drawable.microphone);
                btnStart.setEnabled(true);
                break;
        }
    }

    private void processBySound(List<String> results) {
        for (String item : results) {
            filterString(item);
        }
    }

    private boolean filterString(String item) {
        boolean filterFlag = false;

        switch (item) {
            case "hi":
            case "하이":
            case "파이":
            case "다이":
            case "아이":
            case "나이":
            case "화이":
                filterFlag = sendSocketMessage("activate");
                break;
            case "by":
            case "bye":
            case "바이":
                filterFlag = sendSocketMessage("deactivate");
                break;
            case "옷장":
                filterFlag = sendSocketMessage(3);
                break;
            case "다음":
                filterFlag = sendSocketMessage(4);
                break;
            case "이전":
                filterFlag = sendSocketMessage(5);
                break;
            case "집중연습":
            case "집중 연습":
                filterFlag = sendSocketMessage("practice-1");
                break;
            case "웃는연습":
            case "웃는 연습":
                filterFlag = sendSocketMessage("practice-2");
                break;
            case "그만":
                filterFlag = sendSocketMessage("close-video");
                break;
            default:
                Log.e("filter string", "인식 오류~");
        }

        return filterFlag;
    }

    private boolean sendSocketMessage(final String s) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(MyConstant.SOCKET_ADDR, MyConstant.SOCKET_PORT);
                    OutputStreamWriter outputStream = new OutputStreamWriter(socket.getOutputStream());
                    Log.e("socket write data", s);
                    outputStream.write(s + "\n");
                    outputStream.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        return true;
    }

    private boolean sendSocketMessage(final int num) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(MyConstant.SOCKET_ADDR, MyConstant.SOCKET_PORT);
                    OutputStreamWriter outputStream = new OutputStreamWriter(socket.getOutputStream());
                    Log.e("socket write data", num + "");
                    outputStream.write(num + "");
                    outputStream.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        return true;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_main, container, false);

        owner = (MainActivity) getActivity();
        txtResult = view.findViewById(R.id.txt_result);
        btnStart = view.findViewById(R.id.btn_start);
        cdsText = view.findViewById(R.id.cds_text);
        mp = MediaPlayer.create(owner, R.raw.effectsound);
        handler = new RecognitionHandler(this);
        naverRecognizer = new NaverRecognizer(owner, handler, CLIENT_ID);

        btnStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!naverRecognizer.getSpeechRecognizer().isRunning()) {
                    // Start button is pushed when SpeechRecognizer's state is inactive.
                    // Run SpeechRecongizer by calling recognize().
                    mResult = "";
                    txtResult.setText("Connecting...");
                    btnStart.setImageResource(R.drawable.microphone_on);
                    naverRecognizer.recognize();
                } else {
                    Log.d(TAG, "stop and wait Final Result");
                    btnStart.setEnabled(false);

                    naverRecognizer.getSpeechRecognizer().stop();
                }
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // NOTE : initialize() must be called on start time.
        naverRecognizer.getSpeechRecognizer().initialize();
    }

    @Override
    public void onResume() {
        super.onResume();
        mResult = "";
        txtResult.setText("");
        btnStart.setImageResource(R.drawable.microphone);
        btnStart.setEnabled(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        // NOTE : release() must be called on stop time.
        naverRecognizer.getSpeechRecognizer().release();
    }

    // Declare handler for handling SpeechRecognizer thread's Messages.
    static class RecognitionHandler extends Handler {
        private final WeakReference<SpeechFragment> mActivity;

        RecognitionHandler(SpeechFragment activity) {
            mActivity = new WeakReference<SpeechFragment>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            SpeechFragment activity = mActivity.get();
            if (activity != null) {
                activity.handleMessage(msg);
            }
        }
    }
}

