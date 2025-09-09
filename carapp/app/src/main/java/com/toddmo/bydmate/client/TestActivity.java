package com.toddmo.bydmate.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.toddmo.bydmate.client.utils.KLog;

/**
 * 测试Activity - 用于测试画中画功能
 */
public class TestActivity extends AppCompatActivity {

    private static final String TAG = "TestActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        // 测试按钮
        Button testPipButton = findViewById(R.id.test_pip_button);
        Button testMainButton = findViewById(R.id.test_main_button);
        Button testDebugButton = findViewById(R.id.test_debug_button);

        testPipButton.setOnClickListener(v -> {
            // 测试画中画功能
            KLog.d(TAG + " Testing PIP functionality");
            Toast.makeText(this, "画中画功能测试", Toast.LENGTH_SHORT).show();
        });

        testMainButton.setOnClickListener(v -> {
            // 跳转到主界面
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });

        testDebugButton.setOnClickListener(v -> {
            // 跳转到调试界面
            Intent intent = new Intent(this, DebuggingActivity.class);
            startActivity(intent);
        });
    }
}