package com.jarvis.bridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 48, 48, 48);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("JARVIS Bridge");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);

        TextView info = new TextView(this);
        info.setText(
                "This companion lets JARVIS press WhatsApp's " +
                "Send button after an explicit voice command.\n\n" +
                "Enable the accessibility service below."
        );
        info.setTextSize(16);
        info.setPadding(0, 32, 0, 32);

        Button settings = new Button(this);
        settings.setText("Open Accessibility Settings");

        settings.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Settings.ACTION_ACCESSIBILITY_SETTINGS
            );
            startActivity(intent);
        });

        layout.addView(title);
        layout.addView(info);
        layout.addView(settings);

        setContentView(layout);
    }
}