package com.metova.slim;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

public abstract class SlimActionBarActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View layout = Slim.createLayout(this, this);
        if (layout != null) {
            setContentView(layout);
        }

        Slim.injectExtras(getIntent().getExtras(), this);
        Slim.injectFragment(this);
    }
}
