package com.example.kdash.theremin;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener
{

    public TextView texto;
    public ProgressBar barra;
    private SensorManager manejadorSensor;
    public static DecimalFormat DECIMAL_FORMATTER;

    private final int duration = 1; // seconds
    private final int sampleRate = 8000;
    private final int numSamples = duration * sampleRate;
    private final double sample[] = new double[numSamples];
    private final double baseAccelerationZ = 1.00;
    double accelerationZ;
    private final double baseAccelerationY = 0.00;
    double accelerationY;
    public double freqOfTone;
    public Thread thread;
    private final byte generatedSnd[] = new byte[2 * numSamples];
    boolean isPlaying = false;

    Handler handler = new Handler();
    AudioTrack audioTrack;
    boolean once = true;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        barra=(ProgressBar)findViewById(R.id.progressBar);
        texto=(TextView)findViewById(R.id.textView);

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        DECIMAL_FORMATTER = new DecimalFormat("#.000", symbols);
        manejadorSensor=(SensorManager)getSystemService(SENSOR_SERVICE);
        freqOfTone=440.00;
    }

    @Override
    protected void onStart() {
        super.onStart();

        manejadorSensor.registerListener(this,
                manejadorSensor.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);

        final Boolean running = true;
        final Thread thread = new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            public void run() {
                while (running)
                {

                    //genTone(523.25);
                    if(once) {
                        genTone(freqOfTone);
                    }
                    playSound();
                    handler.post(new Runnable() {
                        public void run() {
                            //playSound();
                        }
                    });
                }
            }
        });
        thread.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            // get values for each axes X,Y,Z
            float magX = event.values[0];
            float magY = event.values[1];
            float magZ = event.values[2];

            freqOfTone  = Math.sqrt((magX * magX) + (magY * magY) + (magZ * magZ));


            //audioTrack.setPlaybackRate((int)(8000*(( 1.1111 *(freqOfTone)) )+ 0.8889));

            barra.setProgress((int) freqOfTone);
            texto.setText(DECIMAL_FORMATTER.format(freqOfTone) + " \u00B5Tesla");


        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i)
    {

    }
    void genTone(double freq){
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/freq));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    void playSound()
    {
        int x;
        audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(generatedSnd.length)
                .build();
       /* audioTrack.setTransferMode(AudioTrack.MODE_STATIC);
        audioTrack.setBufferSizeInBytes(generatedSnd.length);
        audioTrack.build();*/
       audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.play();
        do{                                                     // Wait until sound is complete
            x = audioTrack.getPlaybackHeadPosition();
        }while (x < generatedSnd.length / 2);


        /*(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                AudioTrack.MODE_STATIC);*/
        //write(generatedSnd, 0, generatedSnd.length);


    }
}
