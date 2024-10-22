package com.bmc.tasklist.ui.profile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.bmc.tasklist.R;
import com.bmc.tasklist.ui.LoginActivity;
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
    private Button logoutButton;

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

    // To update User profile
    public void refreshData(String username , Long exp, Long level){
        usernameTextView.setText(username);
        levelTextView.setText("Level: " + (level != null ? level : 1));
        if (exp != null) {
            progressBar.setProgress((int) (exp % 1000) / 10); // Exp modulo 1000
        } else {
            progressBar.setProgress(0);
        }
    }

    @SuppressLint("SetTextI18n")
    private void init() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        LayoutInflater.from(getContext()).inflate(R.layout.top_profile, this, true);

        usernameTextView = findViewById(R.id.textView);
        levelTextView = findViewById(R.id.textView2);
        progressBar = findViewById(R.id.progressBar);
        logoutButton = findViewById(R.id.logoutButton);

        logoutButton.setOnClickListener(v -> {
            mAuth.signOut(); // Sign out the user
            Log.d("Auth", "User logged out");
            Toast.makeText(getContext(), "Déconnecté", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getContext(), LoginActivity.class); // Replace with your actual login activity class
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
        });


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

                                    refreshData(username ,exp,level);
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
