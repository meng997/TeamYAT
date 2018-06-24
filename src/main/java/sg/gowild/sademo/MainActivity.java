package sg.gowild.sademo;
import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.services.language.v1.CloudNaturalLanguage;
import com.google.api.services.language.v1.CloudNaturalLanguageRequestInitializer;
import com.google.api.services.language.v1.model.AnnotateTextRequest;
import com.google.api.services.language.v1.model.AnnotateTextResponse;
import com.google.api.services.language.v1.model.Features;
import com.google.api.services.language.v1.model.Sentiment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import ai.api.AIConfiguration;
import ai.api.AIDataService;
import ai.api.AIServiceException;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Fulfillment;
import ai.api.model.Result;
import ai.api.util.IOUtils;
import ai.kitt.snowboy.SnowboyDetect;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.net.*;
import java.util.Map;
import java.util.Scanner;

import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;





public class MainActivity extends AppCompatActivity {
    //Natural Language

    //TAG
    private static final String TAG = "Main Activity";
    // View Variables
    private Button button;
    private TextView textView;

    // ASR Variables
    private SpeechRecognizer speechRecognizer;


    // TTS Variables
    private TextToSpeech textToSpeech;

    // NLU Variables
    private AIDataService aiDataService;

    // Hotword Variables
    private boolean shouldDetect;
    private boolean shouldEnd=false;
    private SnowboyDetect snowboyDetect;
    //Search Variables
    private boolean shouldsearch = false;
    private String finalvariable;

    File model1;
    private boolean recording = false;
    private boolean listening = false;
    Thread hotwordthread2;
    Thread hotwordthread;
    private boolean shouldcontinue= true;
    private boolean stop = false;
    private int x =-1;
    private FirebaseAuth mAuth;
    private String firebasedata;
   private String emotionState= "";
   private String emotionIntensity="";
    Map<String, Object> input = new HashMap<>();

    static {
        System.loadLibrary("snowboy-detect-android");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: Setup Components
    setupViews();
    setupXiaoBaiButton();
    setupAsr();
    setupTts();


    setupNlu();
    setupHotword();


        // TODO: Start Hotword
        startHotword();

// ...
         mAuth = FirebaseAuth.getInstance();

    }
    @Override
    public void onStart() {
        //login to firebase
        super.onStart();
        mAuth.signInWithEmailAndPassword("173236d@mymail.nyp.edu.sg", "Spdsdgo7");
    }

    private void setupViews() {
        // TODO: Setup Views
        textView= findViewById(R.id.textview);
        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                button.setText("Press me to stop");

                if (shouldsearch == true) {
                    shouldsearch = false;
                    startAsr();
                }
                else if(recording== true && listening== false){
                    button.setText("Press me to start");
                    stopRecognition();
                    setupHotword();
                    startHotword();
                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    db.collection("SpeechToText").add(input);
                    x = -1;


                }
                else{
                    shouldDetect = false;
                    shouldEnd= false;

                }

            }

                                  }
        );
    }

    private void setupXiaoBaiButton() {
        String BUTTON_ACTION = "com.gowild.action.clickDown_action";

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BUTTON_ACTION);

        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO: Add action to do after button press is detected

            }
        };
        registerReceiver(broadcastReceiver, intentFilter);
    }

    private void setupAsr() {
        // TODO: Setup ASR
        speechRecognizer= speechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            String container ="";

                try {
                    container = new String(buffer,"UTF-16");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (container.contains("end")|| container.contains("stop")){
            stop = true;
            stopRecognition();
                }
            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int error) {
                Log.e( "asr", "Error: "+ Integer.toString(error));
        //startHotword();
            }

            @Override
            public void onResults(Bundle results) {

                List<String> texts = results.getStringArrayList(speechRecognizer.RESULTS_RECOGNITION);
                if (texts==null || texts.isEmpty()){
                    textView.setText("Please try again");

                }
                else if (shouldsearch ){




                    String text =texts.get(0).toString().toLowerCase();

                    String question ="";
                    question = text.replace(" ","_");

                    new GetHTTP().execute(question);




                }

                else{
                    String neededwords = "";
                    String lastwords = "";
                    String text = texts.get(0).toString().toLowerCase();
                    if(text.contains(" ")) {
                        neededwords = text.substring(0, text.lastIndexOf(" "));
                        lastwords = text.substring(text.lastIndexOf(" ") + 1);

                    if(stop== true) {

                        textView.setText(neededwords);
                        startNlu(neededwords);
                    }else{
                        textView.setText(text);
                        startNlu(text);
                    }
                    }
                    else{
                        textView.setText(text);
                        startNlu(text);
                    }






                }

            }

            @Override
            public void onPartialResults(Bundle partialResults) {



            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });
    }

    private void startAsr() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                // TODO: Set Language
                final Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en");
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000);

                //
                // hotword detection in case it is still running
                shouldDetect = false;

                // TODO: Start ASR
               hotwordthread.interrupt();
                setupEndHotword();

                recording = true;
                speechRecognizer.startListening(recognizerIntent);
                button.setText("Press me to stop");

            }
        };
        Threadings.runInMainThread(this, runnable);
    }




    private void setupTts() {
        // TODO: Setup TTS
        textToSpeech = new TextToSpeech(this,null);
    }

    public void startTts(String text) {
        // TODO: Start TTS

        textToSpeech.speak(text,TextToSpeech.QUEUE_FLUSH, null);
        // TODO: Wait for end and start hotword
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                while (textToSpeech.isSpeaking()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Log.e("tts", e.getMessage(), e);
                    }
                }



                if (shouldsearch || shouldcontinue){


                    startAsr();
                }


                  else  if(shouldcontinue==false) {

                        button.setText("Press me to start");
                        setupHotword();
                        startHotword();


                }
                DisplayData();

            }
        });

    }
;
    private void setupNlu() {
        // TODO: Change Client Access Token
        String clientAccessToken = "fa7b7de2923647c3bd1a3a645865a74b";
        AIConfiguration aiConfiguration = new AIConfiguration(clientAccessToken,
                AIConfiguration.SupportedLanguages.English);
        aiDataService = new AIDataService(aiConfiguration);
    }

    private void startNlu(final String text) {
        // TODO: Start NLU

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                FirebaseFirestore db = FirebaseFirestore.getInstance();

                x=x+1;
                AIRequest aiRequest = new AIRequest();
                aiRequest.setQuery(text);
                try {
                    AIResponse aiResponse = aiDataService.request(aiRequest);
                    Result result = aiResponse.getResult();
                    Fulfillment fulfillment = result.getFulfillment();
                    String speech = fulfillment.getSpeech();
                    String responseText = "";
                    if(speech.equalsIgnoreCase("weather_function")){
                        responseText = getWeather();
                        startTts(responseText);
                        shouldsearch=false;
                    }
                    else if(speech.equalsIgnoreCase("search_function")){
                        shouldsearch= true;
                        shouldDetect= false;


                            responseText = "Please tell me what you would like to search.";
                            startTts(responseText);



                    }
                    else  {

                        responseText = speech;
                        if(responseText=="end_function"){
                            shouldcontinue=false;
                        }
                        if (!text.equals("hello")){
                            if (x == 1) {

                                 input.put("Name", text);




                            } if (x == 2) {

                                input.put("Feeling", text);
                                NaturalLanguage(text);
                                input.put("FeelingEmotionState", emotionState);
                                input.put("FeelingEmotionIntensity", emotionIntensity);

                            }  if (x == 3) {

                                input.put("Topic", text);
                                NaturalLanguage(text);
                                input.put("TopicEmotionState", emotionState);
                                input.put("TopicEmotionIntensity", emotionIntensity);

                            }  if (x == 4) {

                                input.put("Root Cause", text);
                                NaturalLanguage(text);
                                input.put("RootEmotionState", emotionState);
                                input.put("RootEmotionIntensity", emotionState);


                            }  if (x == 5) {

                                input.put("Told anyone(No)", text);



                            }  if (x == 6) {

                                input.put("Tell Counsellor(No/Yes)", text);


                            }  if (x == 7) {

                                input.put("Wishes", text);


                                    //placeholder
                                shouldcontinue=false;
                                x = -1;

                            }else {
                                shouldcontinue=true;
                            }

                        }

                            //INSERT CLOUD API

                        if(shouldcontinue==false){


                            db.collection("SpeechToText").add(input);

                            x = -1;


                        }


                        startTts(responseText);
                        shouldsearch=false;


                    }



                } catch (AIServiceException e) {
                    e.printStackTrace();
                }
            }
        };
        Threadings.runInBackgroundThread(runnable);
    }

    private void setupHotword() {
        shouldDetect = false;
        SnowboyUtils.copyAssets(this);

        // TODO: Setup Model File
        File snowboyDirectory = SnowboyUtils.getSnowboyDirectory();
       File  model = new File(snowboyDirectory, "Xiaobai.pmdl");

        File common = new File(snowboyDirectory, "common.res");

        // TODO: Set Sensitivity
        snowboyDetect = new SnowboyDetect(common.getAbsolutePath(), model.getAbsolutePath());
        snowboyDetect.setSensitivity("0.60");
        snowboyDetect.applyFrontend(true);
    }


    private void setupEndHotword() {
        shouldEnd = false;
        SnowboyUtils.copyAssets(this);

        // TODO: Setup Model File
        File snowboyDirectory = SnowboyUtils.getSnowboyDirectory();
        model1 = new File(snowboyDirectory, "stop.pmdl");
        File common = new File(snowboyDirectory, "common.res");

        // TODO: Set Sensitivity
        snowboyDetect = new SnowboyDetect(common.getAbsolutePath(), model1.getAbsolutePath());
        snowboyDetect.setSensitivity("0.70");
        snowboyDetect.applyFrontend(true);
    }
private void startHotword() {
        hotwordthread = new Thread() {
            @Override
            public void run() {
                listening= true;
                shouldDetect = true;
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

                int bufferSize = 3200;
                byte[] audioBuffer = new byte[bufferSize];
                AudioRecord audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.DEFAULT,
                        16000,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                );

                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e("hotword", "audio record fail to initialize");
                    return;
                }

                audioRecord.startRecording();
                Log.d("hotword", "start listening to hotword");

                while (shouldDetect) {
                    audioRecord.read(audioBuffer, 0, audioBuffer.length);

                    short[] shortArray = new short[audioBuffer.length / 2];
                    ByteBuffer.wrap(audioBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray);

                    int result = snowboyDetect.runDetection(shortArray, shortArray.length);
                    if (result > 0) {
                        Log.d("hotword", "detected");
                        shouldDetect = false;

                    }
                }

                audioRecord.stop();
                audioRecord.release();
                Log.d("hotword", "stop listening to hotword");

                // TODO: Add action after hotword is detected
                listening=false;
                startAsr();
            }

        };
        Threadings.runInBackgroundThread(hotwordthread);
    }

    private void startEndHotword() {
         hotwordthread2 = new Thread() {
            @Override
            public void run() {

                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

                int bufferSize = 3200;
                byte[] audioBuffer = new byte[bufferSize];
                AudioRecord audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.DEFAULT,
                        16000,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                );

                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e("hotword", "audio record fail to initialize");
                    return;
                }

                audioRecord.startRecording();
                Log.d("hotword", "start listening to hotword");

                shouldEnd= true;
                while (shouldEnd) {
                    audioRecord.read(audioBuffer, 0, audioBuffer.length);

                    short[] shortArray = new short[audioBuffer.length / 2];
                    ByteBuffer.wrap(audioBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray);

                    int result = snowboyDetect.runDetection(shortArray, shortArray.length);
                    if (result > 0) {
                        Log.d("hotword", "detected");
                        shouldEnd = false;
                        stopRecognition();
                    }
                }

                audioRecord.stop();
                audioRecord.release();
                Log.d("hotword", "stop listening to hotword");

                // TODO: Add action after hotword is detected

            }

        };
        Threadings.runInBackgroundThread(hotwordthread2);
    }


    private String getWeather() {
        // TODO: (Optional) Get Weather Data via REST API
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().url("https://api.data.gov.sg/v1/environment/2-hour-weather-forecast")
                .addHeader("accept", "application/json").build();

        try {
            Response response = okHttpClient.newCall(request).execute();
            String responseBody = response.body().string();
            JSONObject jsonObject = new JSONObject(responseBody);
            JSONArray forecasts = jsonObject.getJSONArray("items").getJSONObject(0).getJSONArray("forecasts");
            for (int i =0; i < forecasts.length(); i++){
                JSONObject forecastObject = forecasts.getJSONObject(i);
                String area = forecastObject.getString("area");
                if(area.equalsIgnoreCase("ang mo kio")){
                    String forecast = forecastObject.getString("forecast");
                    return "The weather in Ang Mo Kio is " + forecast ;
                }
                if(area.equalsIgnoreCase("clementi")){
                    String forecast = forecastObject.getString("forecast");
                    return "The weather in Clementi is " + forecast ;

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "No weather info";
    }
    private void GetSearch(String text) throws IOException{
       // String test = ;
        //URLConnection con  = new URL(test).openConnection();
        //InputStream response = con.getInputStream();
        //StringWriter writer = new StringWriter();
        //IOUtils.copy(response,writer);



    }
    private void stopRecognition() {
        Runnable runnable = new Runnable(){
            public void run() {
                speechRecognizer.stopListening();
            }

        };

        Threadings.runInMainThread(this, runnable);

    }

    @SuppressLint("StaticFieldLeak")
    private void NaturalLanguage(String inputspeech){


        final CloudNaturalLanguage naturalLanguageService = new CloudNaturalLanguage.Builder(
                AndroidHttp.newCompatibleTransport(),
                new AndroidJsonFactory(),
                null
        ).setCloudNaturalLanguageRequestInitializer(
                new CloudNaturalLanguageRequestInitializer("AIzaSyAnX2WVjqWyfOYgTPyta4blXjcNmLKJGpk")
        ).build();


        Features features = new Features();
        features.setExtractDocumentSentiment(true);
        com.google.api.services.language.v1.model.Document document = new com.google.api.services.language.v1.model.Document()
                .setContent(inputspeech).setType("PLAIN_TEXT").setLanguage("en-US");

        final AnnotateTextRequest request= new AnnotateTextRequest();
        request.setDocument(document);
        request.setFeatures(features);
        new  AsyncTask<Object, Void, AnnotateTextResponse>() {
            @Override
            protected AnnotateTextResponse doInBackground(Object... params) {
                AnnotateTextResponse response = null;
                try {
                    response = naturalLanguageService.documents().annotateText(request).execute();

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }

            @Override
            protected void onPostExecute(AnnotateTextResponse response) {
                super.onPostExecute(response);
                if (response != null) {
                    Sentiment sentiment = response.getDocumentSentiment();
                    if (sentiment == null) {
                        System.out.println("No sentiment found");
                    } else {
                        if (sentiment.getScore() >= 0) {
                            emotionState = "Positive";
                        }
                        else {
                            emotionState = "Negative";
                        }

                        if (sentiment.getMagnitude() >= 0 && sentiment.getMagnitude() < 1) {
                            emotionIntensity = "Slightly";
                        } else if (sentiment.getMagnitude() >= 1 && sentiment.getMagnitude() < 2) {
                            emotionIntensity = "Mildly";
                        } else if (sentiment.getMagnitude() >= 2 && sentiment.getMagnitude() < 3) {
                            emotionIntensity = "Likely";
                        } else {
                            emotionIntensity = "Very";
                        }

                        System.out.printf("Sentiment magnitude: %.3f\n", sentiment.getMagnitude());
                        System.out.printf("Sentiment score: %.3f\n", sentiment.getScore());
                    }
                }
            }
        }.execute();


    }


    private class GetHTTP extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... question) {
            String answer="";
            String urlquestion = "http://www.answers.com/Q/" + Arrays.toString(question).replaceAll("[\\[\\]]","") ;
            try {
                //StringBuilder result =new StringBuilder();
               // URL url = new URL(urlquestion);
                //HttpURLConnection con = (HttpURLConnection) url.openConnection();
                //con.setRequestMethod("GET");
                //InputStreamReader input = new InputStreamReader(con.getInputStream(),"UTF-8");
                //BufferedReader in = new BufferedReader(input);




                //while (in.readLine()!=null){

                  //  result.append(in.readLine());
                //}

                //in.close();

                //Document doc = Jsoup.parse(result.toString());
                org.jsoup.nodes.Document doc = Jsoup.connect(urlquestion).get();
                Element ans = doc.select("div.answer_text").first();
                //Element last = ans.select("p").first();

                try {
                    answer = ans.text();

                }
                catch (NullPointerException e){
                    answer="Please try again.";

                }


                //con.disconnect();

            }
            catch (IOException e) {

            }

            return finalvariable = answer;

        }

        @Override
        protected void onPostExecute(String temp) {
            textView.setText(finalvariable);

            startTts(finalvariable);
            shouldsearch = false;

        }

    }

    private void DisplayData(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("SpeechToText").document("1").get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        //test get data
                        firebasedata = document.getString("Name");

                        Log.d(TAG, "DocumentSnapshot data: " + firebasedata);
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }


        });

    }
    }
