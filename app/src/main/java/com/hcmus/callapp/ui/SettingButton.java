package com.hcmus.callapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

import com.hcmus.callapp.R;

public class SettingButton {
    private Activity activity;
    private int id;
    private Button button;

    public SettingButton(Activity activity, int id)
    {
        this.activity = activity;
        this.id = id;
        this.button = (Button)activity.findViewById(id);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSetting();
            }
        });
    }

    private void openSetting() {
        Intent intent = new Intent(activity, SettingsActivity.class);
        activity.startActivity(intent);
    }
}
