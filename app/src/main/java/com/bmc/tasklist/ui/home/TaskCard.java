package com.bmc.tasklist.ui.home;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.bmc.tasklist.R;

public class TaskCard extends CardView {

    private TextView taskName;
    private Button categoryButton;
    private CheckBox checkBox;
    private TextView taskDescription;

    public TaskCard(Context context) {
        super(context);
        init(context);
    }

    public TaskCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.card_task, this, true);

        taskName = findViewById(R.id.badge_name);
        taskDescription = findViewById(R.id.task_description);
        categoryButton = findViewById(R.id.btn_task_category);
        checkBox = findViewById(R.id.checkBox);
    }

    public void setTaskName(String name) {
        taskName.setText(name);
    }

    public void setCategory(String category) {
        categoryButton.setText(category);
        //categoryButton.setBackgroundTintList(getResources().getColorStateList(color, null));
    }

    public void setTaskDesc(String desc) {
        taskDescription.setText(desc);
    }


    public void setCheckbox(boolean isChecked) {
        checkBox.setChecked(isChecked);
    }

    public CheckBox getCheckbox() {
        return checkBox;
    }
}

