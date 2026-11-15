package org.xiaofang_dev.xfxzapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class OtherActivity extends AppCompatActivity {
    private boolean isEn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isEn = LangManager.isEn(this);
        setContentView(R.layout.activity_other);

        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView btnLang = findViewById(R.id.btnOtherLanguage);
        TextView btnInterval = findViewById(R.id.btnOtherInterval);

        btnLang.setText(isEn ? "Language" : "语言");
        btnInterval.setText(isEn ? "Data Refresh Interval" : "数据刷新间隔时间");

        btnBack.setOnClickListener(v -> finish());
        btnLang.setOnClickListener(v -> startActivity(new Intent(this, LanguageActivity.class)));
        btnInterval.setOnClickListener(v -> startActivity(new Intent(this, IntervalActivity.class)));
    }
}
