package com.metova.slim.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class DemoActivity extends AppCompatActivity implements DemoFragment.DemoCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
    }

    @Override
    public void receiveCallback() {
        TextView callbackTextView = (TextView) findViewById(R.id.value_TextView);
        callbackTextView.setText(R.string.yes_callback);
    }
}
