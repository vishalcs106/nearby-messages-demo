package com.nearby.myapplication;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;

import java.nio.charset.Charset;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private GoogleApiClient mGoogleApiClient;

    private static final String TAG = MainActivity.class.getSimpleName();
    private Dialog pickMeUpDialog;
    private ProgressDialog pickupDialog;
    private EditText cabNumberEditText;
    private TextView cabNameTextView;
    private ProgressBar progressBar;
    private ACTIONS action;

    private enum ACTIONS {
        PICKUP,
        GETTING_PICKED,
        IDLE
    }

    private MessageListener pickupListener, pickmeUpListener;

    private static final int TTL_IN_SECONDS = 3 * 60;

    private static final Strategy PUB_SUB_STRATEGY = new Strategy.Builder()
            .setTtlSeconds(TTL_IN_SECONDS).build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initDialog();
        buildGoogleApiClient();
        initListener();
    }

    private void initView() {
        cabNumberEditText = findViewById(R.id.cab_no_edit_text);
    }

    private void initDialog() {
        pickMeUpDialog = new Dialog(this);
        pickMeUpDialog.setContentView(R.layout.pickme_dialog_layout);
        cabNameTextView = pickMeUpDialog.findViewById(R.id.cab_no_textview);
        progressBar = pickMeUpDialog.findViewById(R.id.progressBar);
        cabNameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                publish(new Message(cabNameTextView.getText().toString().getBytes(Charset.forName("UTF-8"))));
                pickMeUpDialog.dismiss();
            }
        });
        pickMeUpDialog.setCancelable(false);
    }

    private void buildGoogleApiClient() {
        if (mGoogleApiClient != null) {
            return;
        }
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .build();
    }

    public void pickUpUser(View view) {
        pickupDialog = new ProgressDialog(this);
        pickupDialog.setCancelable(false);
        pickupDialog.setMessage("Picking Customer");
        pickupDialog.show();
        action = ACTIONS.PICKUP;
        publish(new Message(cabNumberEditText.getText().toString().getBytes(Charset.forName("UTF-8"))));
        subscribe(pickupListener);
    }

    public void pickMeUp(View view) {
        pickMeUpDialog.show();
        action = ACTIONS.GETTING_PICKED;
        subscribe(pickmeUpListener);
    }

    private void publish(Message message) {
        Log.i(TAG, "Publishing");
        PublishOptions options = new PublishOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(new PublishCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        Log.i(TAG, "No longer publishing");

                    }
                }).build();

        Nearby.Messages.publish(mGoogleApiClient, message, options)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Published successfully.");
                        } else {
                            Toast.makeText(MainActivity.this, "Could not publish "+status, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void subscribe(MessageListener messageListener) {
        Log.i(TAG, "Subscribing");
        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(PUB_SUB_STRATEGY)
                .setCallback(new SubscribeCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        Log.i(TAG, "No longer subscribing");

                    }
                }).build();

        Nearby.Messages.subscribe(mGoogleApiClient, messageListener, options)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Subscribed successfully.");
                        } else {
                            Toast.makeText(MainActivity.this, "Could not receive any message "+status, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void initListener(){
        pickmeUpListener = new MessageListener() {
            @Override
            public void onFound(Message message) {
                Log.d(TAG, "Found message: " + new String(message.getContent()));
                if(action == ACTIONS.GETTING_PICKED){
                    cabNameTextView.setText(new String(message.getContent()));
                    cabNameTextView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                }

            }

            @Override
            public void onLost(Message message) {
                Log.d(TAG, "Lost sight of message: " + new String(message.getContent()));
            }
        };
        pickupListener = new MessageListener() {
            @Override
            public void onFound(Message message) {
                Log.d(TAG, "Found message: " + new String(message.getContent()));
                if(action == ACTIONS.PICKUP && new String(message.getContent()).contains(cabNameTextView.getText().toString())){
                    pickupDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Picked Up Customer", Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void onLost(Message message) {
                Log.d(TAG, "Lost sight of message: " + new String(message.getContent()));
            }
        };
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
