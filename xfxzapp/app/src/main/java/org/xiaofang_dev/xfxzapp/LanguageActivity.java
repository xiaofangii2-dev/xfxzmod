package org.xiaofang_dev.xfxzapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LanguageActivity extends AppCompatActivity {
    private TextView tvCurrentLang, btnSelectLang;
    private boolean isEn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isEn = LangManager.isEn(this);
        setContentView(R.layout.activity_language);
        
        tvCurrentLang = findViewById(R.id.tvCurrentLang);
        btnSelectLang = findViewById(R.id.btnSelectLang);
        ImageButton btnBack = findViewById(R.id.btnBack);

        refresh();

        btnBack.setOnClickListener(v -> finish());

        btnSelectLang.setOnClickListener(v -> {
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_language, null);
            TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
            TextView btnZh = dialogView.findViewById(R.id.btnLangZh);
            TextView btnEn = dialogView.findViewById(R.id.btnLangEn);

            tvTitle.setText(isEn ? "Select Language" : "选择语言");

            AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar)
                    .setView(dialogView)
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                // 控制原生弹窗的边距，让它看起来像原生一样，两边有点空隙但不过分宽
                dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.75), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            btnZh.setOnClickListener(v2 -> {
                LangManager.setLang(this, false);
                dialog.dismiss();
                recreate();
            });

            btnEn.setOnClickListener(v2 -> {
                LangManager.setLang(this, true);
                dialog.dismiss();
                recreate();
            });

            dialog.show();
        });
    }

    private void refresh() {
        isEn = LangManager.isEn(this);
        tvCurrentLang.setText(isEn ? "Current Language: English" : "当前语言: 中文");
        btnSelectLang.setText(isEn ? "Change Language" : "切换语言");
    }
}
