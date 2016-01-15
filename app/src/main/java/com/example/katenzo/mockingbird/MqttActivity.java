package com.example.katenzo.mockingbird;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MqttActivity extends AppCompatActivity {
    private static final String MQTT_TAG = "mockmqtt";
    public static final String TOPIC = "pahodemo/test";

    private MqttClient mqttClientSubscribe;

    @Bind(R.id.messageSubscribeText)
    TextView messageSubscribe;

    @Bind(R.id.errorTxt)
    TextView errorText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initSubscribeClient();
    }

    private void initSubscribeClient() {
        try {
            MqttClientPersistence mqttClientPersistence = new MqttDefaultFilePersistence(this.getDir("prod", MODE_PRIVATE).getAbsolutePath());
            mqttClientSubscribe = new MqttClient("tcp://localhost:1883", "pahomqttpublish2", mqttClientPersistence);
            mqttClientSubscribe.connect();

            mqttClientSubscribe.subscribe(TOPIC);
            MqttCallback mqttCallback = new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                    errorText.postDelayed(new Runnable() {
                        public void run() {
                            messageSubscribe.setText("Lost Connection");
                        }
                    }, 100);
                }

                @Override
                public void messageArrived(String s, final MqttMessage mqttMessage) throws Exception {

                    messageSubscribe.post(new Runnable() {
                        public void run() {
                            messageSubscribe.setText(mqttMessage.toString());
                        }
                    });

                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

                }
            };
            mqttClientSubscribe.setCallback(mqttCallback);

        } catch (MqttException e) {
            e.printStackTrace();
            Log.e(MQTT_TAG, "Client " + e.getMessage(), e);
        }
    }


    @Override
    protected void onDestroy() {
        try {
            if (mqttClientSubscribe.isConnected()) {
                mqttClientSubscribe.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }

}
