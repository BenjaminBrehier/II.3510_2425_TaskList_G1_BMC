package com.bmc.tasklist.ui.profile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.bmc.tasklist.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class TopProfile extends CardView {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextView usernameTextView;
    private TextView levelTextView;
    private ProgressBar progressBar;

    public TopProfile(@NonNull Context context) {
        super(context);
        init();
    }

    public TopProfile(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TopProfile(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @SuppressLint("SetTextI18n")
    private void init() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        LayoutInflater.from(getContext()).inflate(R.layout.top_profile, this, true);

        usernameTextView = findViewById(R.id.textView);
        levelTextView = findViewById(R.id.textView2);
        progressBar = findViewById(R.id.progressBar);

        FirebaseUser user = mAuth.getCurrentUser();
        String userId;
        if (user != null) {
            userId = user.getUid();
            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                Map<String, Object> userData = document.getData();
                                if (userData != null) {
                                    String username = (String) userData.get("username");
                                    Long level = (Long) userData.get("level");
                                    Long exp = (Long) userData.get("exp");

                                    usernameTextView.setText(username);
                                    levelTextView.setText("Level: " + (level != null ? level : 1));
                                    if (exp != null) {
                                        progressBar.setProgress((int) (exp % 1000) / 10); // Exp modulo 1000
                                    } else {
                                        progressBar.setProgress(0);
                                    }
                                }
                            } else {
                                Log.d("Firestore", "Aucun document trouvé pour cet utilisateur");
                            }
                        } else {
                            Log.d("Firestore", "Erreur lors de la récupération du profil utilisateur", task.getException());
                        }
                    });
        } else {
            Log.d("Auth", "Utilisateur non connecté");
        }
    }
}
