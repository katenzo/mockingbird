package com.example.katenzo.mockingbird;

import android.app.Activity;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import org.eclipse.moquette.server.Server;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import rx.Observable;
import rx.Subscriber;
import rx.observers.TestSubscriber;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

/**
 * Created by garry on 1/14/16.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest

public class PublishMqttScreenTest extends ActivityInstrumentationTestCase2<MqttActivity> {
    private static final String LOG_TAG = "mockTest";
    private static final String STRING_TO_BE_TYPED = "Publish Mqtt";
    private static final String MQTT_SERVER = "tcp://localhost:1883";

    private MqttActivity activity;
    private static Server mqttServer;

    private MqttAsyncClient mqttAsyncClientSignal;

    public PublishMqttScreenTest() {
        super(MqttActivity.class);
    }


    @BeforeClass
    public static void setUpBeforeClass() throws  Exception{
       try {
           mqttServer = new Server();
           Log.i(LOG_TAG, "Start Server");
           mqttServer.startServer();
       } catch (RuntimeException e) {
           Log.e(LOG_TAG, " Start Server mqtt " + e.getMessage(), e);
       }
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        activity = getActivity();

        initSignalClient();

    }

    private void initSignalClient() {
        try {
            mqttAsyncClientSignal = createAsyncClient("clientTest3");

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private MqttAsyncClient createAsyncClient(String clientId) throws MqttException {

            MqttClientPersistence mqttClientPersistence = new MqttDefaultFilePersistence(activity.getDir(clientId, Activity.MODE_PRIVATE).getAbsolutePath());
            MqttAsyncClient client = new MqttAsyncClient(MQTT_SERVER, clientId , mqttClientPersistence);
            return client;
    }

    @Test
    public void testPublishMessageAsyncShouldShowOnView() throws InterruptedException {

        Observable<String> observable =  publishAsyncMessage(STRING_TO_BE_TYPED);

        TestSubscriber testSubscriber = new TestSubscriber();
        observable.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();

        onView(withId(R.id.messageSubscribeText)).check(matches(withText(STRING_TO_BE_TYPED)));

    }


    private Observable<String> publishAsyncMessage(final String message) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(final Subscriber<? super String> subscriber) {
                mqttAsyncClientSignal.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {

                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                        String fromBroker = mqttMessage.toString();
                        if (message.equals(fromBroker)) {
                            subscriber.onNext(message);
                            subscriber.onCompleted();
                        } else {
                            subscriber.onError(
                                    new IllegalArgumentException("Publish: " + message + " , From Broker: " + fromBroker));
                        }
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {

                    }
                });

                final MqttMessage mqttMessage = new MqttMessage();
                mqttMessage.setPayload(message.getBytes());
                try {

                    MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
                    mqttConnectOptions.setServerURIs(new String[] {MQTT_SERVER});

                    mqttAsyncClientSignal.connect(mqttConnectOptions, null, new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            try {
                                mqttAsyncClientSignal.subscribe(MqttActivity.TOPIC, 1);
                                mqttAsyncClientSignal.publish(MqttActivity.TOPIC, mqttMessage);
                            } catch (MqttException e) {
                                Log.e(LOG_TAG, "Error Publish  " + e.getMessage(), e);
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                        }
                    });


                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error Connect To Server " + e.getMessage(), e);
                    e.printStackTrace();
                }
            }
        });

    }

    @After
    @Override
    public void tearDown() throws Exception  {
        if (mqttAsyncClientSignal.isConnected()) {
            Log.i(LOG_TAG, "Disconnect Client");
            mqttAsyncClientSignal.disconnect();
        }
    }

    @AfterClass
    public static void tearDownAfterClass() {
        try {
            if (mqttServer != null) {
                Log.i(LOG_TAG, "Stop Server");
                mqttServer.stopServer();
            }
        } catch (RuntimeException e) {
            Log.e(LOG_TAG, "Tear Down : Stop Server " + e.getMessage(), e);
        }

    }
}
