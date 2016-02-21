package com.example.youxian.pathview_divide_sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

import com.eftimoff.androipathview.PathView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        Button forwardButton = (Button) findViewById(R.id.button_forward);
        Button backwardButton = (Button) findViewById(R.id.button_backward);
        PathView pathView = (PathView) findViewById(R.id.pathview);
    }
}
