package com.bmc.tasklist.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bmc.tasklist.MainActivity;
import com.bmc.tasklist.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button registerButton;
    private FirebaseAuth mAuth;
    private TextView loginTextView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);
        loginTextView = findViewById(R.id.loginTextView);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerUser();
            }
        });

        loginTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            }
        });
    }

    private void registerUser() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Veuillez entrer l'email et le mot de passe", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();

                    if (user != null) {
                        String userId = user.getUid();
                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("username", user.getEmail());
                        userData.put("level", 1);
                        userData.put("exp", 0);

                        // Create user document and badges
                        createUserDocument(userId,  db, userData, task);
                        createUserBadges(userId,  db, userData, task);
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, "Échec de l'inscription", Toast.LENGTH_SHORT).show();
                }
            }

            public List<String> getAllBadges(FirebaseFirestore db){
                List<String> badgeNames = new ArrayList<>();
                db.collection("badges")
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    // Add the document ID (badge name) to the list
                                    badgeNames.add(document.getId());
                                }
                                Log.d("Firestore", "Badge names: " + badgeNames);
                            } else {
                                Log.w("Firestore", "Error getting badges.", task.getException());
                            }
                        });
                return badgeNames;
            }

            public void createUserBadges(String userId, FirebaseFirestore db, Map<String, Object> userData, @NonNull Task<AuthResult> task){
                // Create user badges
                db.collection("users").document(userId)
                        .set(userData)
                        .addOnSuccessListener(aVoid -> {
                            for (String badge : getAllBadges(db)) {
                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                Map<String, Object> defaultBadges = new HashMap<>();
                                defaultBadges.put("progress ", 0);
                                defaultBadges.put("title", badge);
                                // Add default task
                                db.collection("users").document(userId)
                                        .collection("badges")
                                        .document(badge)
                                        .set(defaultBadges)
                                        .addOnSuccessListener(taskRef -> {
                                            Log.d("Firestore", "Default task added with ID: " + badge);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.w("Firestore", "Error adding task", e);
                                        });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(RegisterActivity.this, "Échec de la création du document", Toast.LENGTH_SHORT).show();
                        });
            }

            public void createUserDocument(String userId, FirebaseFirestore db, Map<String, Object> userData, @NonNull Task<AuthResult> task){
                db.collection("users").document(userId)
                        .set(userData)
                        .addOnSuccessListener(aVoid -> {
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            Map<String, Object> defaultTask = new HashMap<>();
                            defaultTask.put("title", "First Task");
                            defaultTask.put("description", "This is your first task.");
                            defaultTask.put("completed", false);
                            defaultTask.put("tag", "homework");
                            defaultTask.put("exp", 100);

                            // Add default task
                            db.collection("users").document(userId)
                                    .collection("tasks")
                                    .add(defaultTask)
                                    .addOnSuccessListener(taskRef -> {
                                        Log.d("Firestore", "Default task added with ID: " + taskRef.getId());
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w("Firestore", "Error adding task", e);
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(RegisterActivity.this, "Échec de la création du document", Toast.LENGTH_SHORT).show();
                        });
            }

        });
    }
}
