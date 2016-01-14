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
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.junit.After;
import org.junit.Before;
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
    private static final String STRING_TO_BE_TYPED = "Publish Mqtt";
    private static final String MQTT_SERVER = "tcp://localhost:1883";

    private MqttActivity activity;
    private Server mqttServer;

    private MqttClient mqttClientPublisher;
    private MqttAsyncClient mqttAsyncClient;
    private MqttAsyncClient mqttAsyncClientSignal;
    private boolean done;

    public PublishMqttScreenTest() {
        super(MqttActivity.class);
    }


    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        mqttServer = new Server();
        mqttServer.startServer();

        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        activity = getActivity();

        initPublishClient();
        initAsyncClient();
        initSignalClient();

    }



    private void initPublishClient() {
        try {
            MqttClientPersistence mqttClientPersistence = new MqttDefaultFilePersistence(activity.getDir("test1", Activity.MODE_PRIVATE).getAbsolutePath());
            mqttClientPublisher = new MqttClient(MQTT_SERVER, "clienttest1", mqttClientPersistence);
            mqttClientPublisher.connect();

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void initAsyncClient() {
        try {
            mqttAsyncClient = createAsyncClient("clientTest2");
            mqttAsyncClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Test","  Client Test 2 Error Connect " + e.getMessage(), e );
        }

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
//
//
//    @Test
//    public void testMessageShouldShowOnView() {
//
//        for (int i=1;i<=1;i++) {
//            publishMessage(STRING_TO_BE_TYPED + i);
//            onView(withId(R.id.messageSubscribeText)).check(matches(withText(STRING_TO_BE_TYPED + i)));
//        }
//    }


    @Test
    public void testPublishMessageAsyncShouldShowOnView() throws InterruptedException {

        Observable<String> observable =  publishAsyncMessage(STRING_TO_BE_TYPED);

        TestSubscriber testSubscriber = new TestSubscriber();
        observable.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();

        onView(withId(R.id.messageSubscribeText)).check(matches(withText(STRING_TO_BE_TYPED)));

    }

    private MqttCallback createMqttCallBack() {
        MqttCallback mqttCallback = new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String s, final MqttMessage mqttMessage) throws Exception {
                done = true;
                onView(withId(R.id.messageSubscribeText)).check(matches(withText(STRING_TO_BE_TYPED)));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        };
        return mqttCallback;
    }

    private void publishMessage(String message) {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(message.getBytes());
        try {
            mqttClientPublisher.publish(MqttActivity.TOPIC, mqttMessage);
        } catch (MqttException e) {
            e.printStackTrace();
        }
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
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                        }
                    });


                } catch (Exception e) {
                    Log.e("Test", "Error Connect " + e.getMessage(), e);
                    e.printStackTrace();
                }
            }
        });

    }


    @After
    @Override
    public void tearDown() throws Exception {

        if (mqttClientPublisher.isConnected()) {
            mqttClientPublisher.disconnect();
        }

        if (mqttAsyncClient.isConnected()) {
            mqttAsyncClient.disconnect();
        }

        if (mqttAsyncClientSignal.isConnected()) {
            mqttAsyncClientSignal.disconnect();
        }

        if (mqttServer != null) {
         //   mqttServer.stopServer();
        }
        super.tearDown();
    }
}
