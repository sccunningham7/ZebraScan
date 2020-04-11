package com.wb.zebrascan;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.Buffer;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        final Toolbar toolbar = findViewById(R.id.aboutToolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
        // Set app version text
        String versionTextString = "ZebraScan Version: " + BuildConfig.VERSION_NAME;
        final TextView versionText = findViewById(R.id.versionText);
        versionText.setText(versionTextString);

        try {
            Process process = Runtime.getRuntime().exec("logcat ZebraScan:I *:E -v brief -d");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            StringBuilder log = new StringBuilder();
            String line = "";
            while (( line = bufferedReader.readLine()) != null) {
                log.append(line).append("\r\n");
            }
            TextView logView = findViewById(R.id.logText);
            logView.setText(log.toString());
        } catch (IOException ex) {
            TextView logView = findViewById(R.id.logText);
            logView.setText(getResources().getText(R.string.log_error));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
