package com.bmc.tasklist.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bmc.tasklist.databinding.FragmentHomeBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);


        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        String userId;
        if (user != null) {
            userId = user.getUid();
        } else {
            userId = "toto";
        }


        db = FirebaseFirestore.getInstance();

        // Get user's profile
        db.collection("users")
                .document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                Map<String, Object> userData = document.getData();
                                Log.d("Firestore", "Document trouvé : " + userData);

                                if (userData != null) {
                                    textView.setText((String) userData.get("username"));

                                    List<String> badges = (List<String>) userData.get("badge");
                                    int level = ((Long) userData.get("level")).intValue();

                                    Log.d("Firestore", "Badges: " + badges);
                                    Log.d("Firestore", "Level: " + level);
                                }
                            } else {
                                Log.d("Firestore", "Aucun document trouvé pour cet ID");
                            }
                        } else {
                            Log.d("Firestore", "Erreur lors de la récupération du document utilisateur : ", task.getException());
                        }
                    }
                });

        // Get user's tasks
        db.collection("users")
        .document(userId)
        .collection("tasks")
        .get()
        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    LinearLayout taskListLayout = binding.taskListLayout;

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String taskId = document.getId();
                        String title = document.getString("title");
                        String description = document.getString("description");
                        boolean completed = Boolean.TRUE.equals(document.getBoolean("completed"));
                        String tag = document.getString("tag");

                        if(!completed) { // Only print tasks not completed
                            TaskCard taskCard = new TaskCard(getContext());
                            taskCard.setTaskName(title);
                            taskCard.setCheckbox(completed);
                            taskCard.setCategory(tag);

                            taskCard.getCheckbox().setOnCheckedChangeListener((buttonView, isChecked) -> {
                                db.collection("users")
                                        .document(userId)
                                        .collection("tasks")
                                        .document(taskId)
                                        .update("completed", isChecked)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(getContext(), "Tâche mise à jour", Toast.LENGTH_SHORT).show();
                                            taskListLayout.removeView(taskCard);
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(getContext(), "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show();
                                        });
                            });
                            taskListLayout.addView(taskCard);
                        }
                    }
                } else {
                    Log.d("Firestore", "Erreur lors de la récupération des tâches : ", task.getException());
                }
            }
        });


//        binding.logoutButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                FirebaseAuth.getInstance().signOut();
//                Toast.makeText(getActivity(), "Déconnecté", Toast.LENGTH_SHORT).show();
//
//                Intent intent = new Intent(getActivity(), LoginActivity.class);
//                startActivity(intent);
//                getActivity().finish();
//            }
//        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}