package com.bmc.tasklist.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bmc.tasklist.R;
import com.bmc.tasklist.databinding.FragmentDashboardBinding;
import com.bmc.tasklist.ui.dashboard.DashboardViewModel;
import com.bmc.tasklist.ui.profile.TopProfile;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class CompletedFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private boolean isManaging = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        String userId;
        if (user != null) {
            userId = user.getUid();
        } else {
            userId = "toto";
        }

        binding.manageButton.setOnClickListener(v -> {
            isManaging = !isManaging;
            updateTaskView();
        });

        db = FirebaseFirestore.getInstance();

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

                        if (completed) { // Only print tasks not completed
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

                            // Get the delete icon (initially hidden)
                            ImageView deleteIcon = taskCard.findViewById(R.id.deleteIcon);
                            deleteIcon.setVisibility(isManaging ? View.VISIBLE : View.GONE); // Show if managing

                            // Set up delete
                            deleteIcon.setOnClickListener(v -> {
                                db.collection("users")
                                .document(userId)
                                .collection("tasks")
                                .document(taskId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Tâche supprimée", Toast.LENGTH_SHORT).show();
                                    binding.taskListLayout.removeView(taskCard);
                                })
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Erreur lors de la suppression", Toast.LENGTH_SHORT).show());
                            });
                            taskListLayout.addView(taskCard);
                        }
                    }
                } else {
                    Log.d("Firestore", "Erreur lors de la récupération des tâches : ", task.getException());
                }
            }
        });
        return root;
    }

    private void updateTaskView() {
        // Loop through each child in taskListLayout (starting at 1 to exclude the buttons)
        for (int i = 1; i < binding.taskListLayout.getChildCount(); i++) {
            View taskCard = binding.taskListLayout.getChildAt(i);
            ImageView deleteIcon = taskCard.findViewById(R.id.deleteIcon);

            if (deleteIcon != null) {
                deleteIcon.setVisibility(isManaging ? View.VISIBLE : View.GONE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}