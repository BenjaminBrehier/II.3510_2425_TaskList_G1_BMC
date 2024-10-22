package com.bmc.tasklist.ui.profile;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.bmc.tasklist.R;

public class BadgeCard extends CardView {
    private TextView badgeName;
    private TextView progressText;
    private TextView badgeDesc;
    private ProgressBar progressBar;

    public BadgeCard(Context context) {
        super(context);
        init(context);
    }

    public BadgeCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.card_badge, this, true);

        badgeName = findViewById(R.id.badge_name);
        progressText = findViewById(R.id.progress_text);
        badgeDesc = findViewById(R.id.badge_desc);
    }

    public void setBadgeName(String name) {
        badgeName.setText(name);
    }

    public void setBadgeDesc(String desc){
        badgeDesc.setText(desc);
    }
    public void setProgressText(String text){
        progressText.setText(text);
    }

    public void setProgressBar(Long progress, Long next){
        progressBar.setProgress((int) (progress/next));
    }
}
