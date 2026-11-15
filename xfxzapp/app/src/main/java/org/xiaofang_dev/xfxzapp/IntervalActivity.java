package org.xiaofang_dev.xfxzapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class IntervalActivity extends AppCompatActivity {
    private TextView tvCurrentInterval, btnEditInterval;
    private boolean isEn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isEn = LangManager.isEn(this);
        setContentView(R.layout.activity_interval);
        
        tvCurrentInterval = findViewById(R.id.tvCurrentInterval);
        btnEditInterval = findViewById(R.id.btnEditInterval);
        ImageButton btnBack = findViewById(R.id.btnBack);

        refresh();

        btnBack.setOnClickListener(v -> finish());

        btnEditInterval.setOnClickListener(v -> {
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_input, null);
            TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
            TextView tvHint = dialogView.findViewById(R.id.tvDialogHint);
            EditText etInput = dialogView.findViewById(R.id.etDialogInput);
            Button btnCancel = dialogView.findViewById(R.id.btnDialogCancel);
            Button btnSave = dialogView.findViewById(R.id.btnDialogSave);
            
            tvTitle.setText(isEn ? "Set Refresh Interval" : "设置数据刷新间隔时间");
            tvHint.setText(isEn ? "Unit: Seconds (2 - 10 recommended)" : "单位：秒（建议 2 - 10 秒）");
            etInput.setText(String.valueOf(Prefs.getUpdateInterval(this)));
            btnCancel.setText(isEn ? "Cancel" : "取消");
            btnSave.setText(isEn ? "Save" : "保存");

            AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar)
                    .setView(dialogView)
                    .create();
            
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            btnCancel.setOnClickListener(v2 -> dialog.dismiss());
            btnSave.setOnClickListener(v2 -> {
                try {
                    int val = Integer.parseInt(etInput.getText().toString());
                    if (val < 1) val = 1;
                    Prefs.setUpdateInterval(this, val);
                    refresh();
                } catch (Exception ignored) {}
                dialog.dismiss();
            });

            dialog.show();
        });
    }

    private void refresh() {
        isEn = LangManager.isEn(this);
        tvCurrentInterval.setText(isEn ? "Current Interval: " + Prefs.getUpdateInterval(this) + "s" : "当前刷新间隔: " + Prefs.getUpdateInterval(this) + " 秒");
        btnEditInterval.setText(isEn ? "Change Interval" : "修改时间");
    }
}
