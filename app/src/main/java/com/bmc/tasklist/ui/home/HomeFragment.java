package com.bmc.tasklist.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bmc.tasklist.R;
import com.bmc.tasklist.databinding.FragmentHomeBinding;
import com.bmc.tasklist.ui.profile.TopProfile;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    private final int EXP = 100; // Default task EXP
    private boolean isManaging = false;
    private FragmentHomeBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Set the create button listener
        binding.createButton.setOnClickListener(v -> showCreateTaskDialog());
        binding.manageButton.setOnClickListener(v -> {
            isManaging = !isManaging;
            updateTaskView();
        });

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
        .addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    Map<String, Object> userData = document.getData();
                    Log.d("Firestore", "Document trouvé : " + userData);

                } else {
                    Log.d("Firestore", "Aucun document trouvé pour cet ID");
                }
            } else {
                Log.d("Firestore", "Erreur lors de la récupération du document utilisateur : ", task.getException());
            }
        });

        // Get user's tasks
        loadTasks(userId);
        TopProfile topProfile = binding.topProfile;
        topProfile = new TopProfile(getContext());

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

    private void loadTasks(String userId) {
        db.collection("users")
        .document(userId)
        .collection("tasks")
        .get()
        .addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Remove all views except the buttons (keeping index 0 for buttons)
                for (int i = binding.taskListLayout.getChildCount() - 1; i > 0; i--) {
                    binding.taskListLayout.removeViewAt(i);
                }

                for (QueryDocumentSnapshot document : task.getResult()) {
                    String taskId = document.getId();
                    String title = document.getString("title");
                    String description = document.getString("description");
                    boolean completed = Boolean.TRUE.equals(document.getBoolean("completed"));
                    String tag = document.getString("tag");

                    if (!completed) {
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
                                binding.taskListLayout.removeView(taskCard); // Remove task when completed
                                Long taskExp = document.getLong("exp");

                                // Update User level
                                db.collection("users")
                                .document(userId)
                                .get()
                                .addOnSuccessListener(userDocument -> {
                                    if (userDocument.exists()) {
                                        Long currentExp = userDocument.getLong("exp");
                                        Long currentLevel = userDocument.getLong("level");

                                        if (currentExp == null) currentExp = 0L; // If exp is null
                                        if (currentLevel == null) currentLevel = 1L; // if level is null
                                        long newExp = currentExp;

                                        if(taskExp != null){
                                            newExp = currentExp + taskExp;
                                        }


                                        Long newLevel = currentLevel;
                                        if (newExp >= currentLevel * 1000) {
                                            newLevel = (newExp / 1000) + 1;
                                        }

                                        db.collection("users")
                                        .document(userId)
                                        .update("exp", newExp, "level", newLevel)
                                        .addOnSuccessListener(Void -> {
                                            Toast.makeText(getContext(), "Expérience et niveau mis à jour", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(getContext(), "Erreur lors de la mise à jour de l'EXP", Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                });
                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show());
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
                        binding.taskListLayout.addView(taskCard);
                    }
                }
            } else {
                Log.d("Firestore", "Erreur lors de la récupération des tâches : ", task.getException());
            }
        });
    }

    private void showCreateTaskDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.task_creation_bottom_sheet, null);
        bottomSheetDialog.setContentView(sheetView);

        EditText taskTitleInput = sheetView.findViewById(R.id.taskTitleInput);
        EditText taskDescriptionInput = sheetView.findViewById(R.id.taskDescriptionInput);
        EditText taskTagInput = sheetView.findViewById(R.id.taskTagInput);

        sheetView.findViewById(R.id.createTaskButton).setOnClickListener(v -> {
            String title = taskTitleInput.getText().toString();
            String description = taskDescriptionInput.getText().toString();
            String tag = taskTagInput.getText().toString();

            if (!title.isEmpty() && !description.isEmpty()) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    String userId = user.getUid();

                    Map<String, Object> taskData = new HashMap<>();
                    taskData.put("title", title);
                    taskData.put("description", description);
                    taskData.put("tag", tag);
                    taskData.put("exp", EXP);
                    taskData.put("completed", false);

                    db.collection("users")
                    .document(userId)
                    .collection("tasks")
                    .add(taskData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(getContext(), "Tâche créée avec succès", Toast.LENGTH_SHORT).show();
                        bottomSheetDialog.dismiss();
                        loadTasks(userId);
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Erreur lors de la création de la tâche", Toast.LENGTH_SHORT).show());
                }
            } else {
                Toast.makeText(getContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            }
        });

        bottomSheetDialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}