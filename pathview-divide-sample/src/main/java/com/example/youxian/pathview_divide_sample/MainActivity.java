package com.example.youxian.pathview_divide_sample;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
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
        final PathView pathView = (PathView) findViewById(R.id.pathview);
        pathView.setStateDivider(2);
        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pathView.setStateCounter(pathView.getStateCounter() + 1);
                ObjectAnimator animator = ObjectAnimator.ofFloat(pathView, "percentage", 0.0f, 1.0f);
                animator.setStartDelay(100);
                animator.setDuration(1000);
                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        pathView.setForward(true);
                    }
                });
                animator.start();
            }
        });
        backwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(pathView, "percentage", 1.0f, 0.0f);
                animator.setStartDelay(100);
                animator.setDuration(1000);
                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                animator.addListener(new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        pathView.setBackward(true);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        pathView.setStateCounter(pathView.getStateCounter() - 1);
                    }
                });
                animator.start();
            }
        });
    }
}