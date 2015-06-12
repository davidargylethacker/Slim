package com.metova.slim.sample;

import com.metova.slim.Slim;
import com.metova.slim.annotation.Callback;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class DemoFragment extends Fragment {

    @Callback
    DemoCallback mCallback;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_demo, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Slim.bind(this);
        Button callbackButton = (Button) view.findViewById(R.id.callback_Button);
        callbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.receiveCallback();
            }
        });
    }

    interface DemoCallback {

        void receiveCallback();
    }
}
