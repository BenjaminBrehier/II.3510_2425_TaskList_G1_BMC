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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

                        // Update User Exp
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

                                updateUserExp(userId,taskExp);
                                updateBadges(db, userId, tag);
                                updateUserExp(userId, taskExp);
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

    private void updateUserExp(String userId, Long taskExp){
        // Update User level
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(userDocument -> {
                    if (userDocument.exists()) {
                        Long currentExp = userDocument.getLong("exp");
                        Long currentLevel = userDocument.getLong("level");
                        FirebaseUser user = mAuth.getCurrentUser();
                        String username = user.getEmail();

                        if (currentExp == null) currentExp = 0L; // If exp is null
                        if (currentLevel == null) currentLevel = 1L; // if level is null

                        long newExp;
                        if(taskExp != null){
                            newExp = currentExp + taskExp;
                        } else {
                            newExp = currentExp;
                        }
                        Long newLevel;
                        if (newExp >= currentLevel * 1000) {
                            newLevel = (newExp / 1000) + 1;
                        } else {
                            newLevel = currentLevel;
                        }

                        db.collection("users")
                                .document(userId)
                                .update("exp", newExp, "level", newLevel)
                                .addOnSuccessListener(Void -> {
                                    Toast.makeText(getContext(), "Expérience et niveau mis à jour", Toast.LENGTH_SHORT).show();
                                    binding.topProfile.refreshData(username, newExp, newLevel);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Erreur lors de la mise à jour de l'EXP", Toast.LENGTH_SHORT).show();
                                });
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

    public void updateBadges(FirebaseFirestore db, String userId, String tag){
        int[] updatedProgress = new int[1];
        System.out.println("Test");
        db.collection("users")
            .document(userId)
            .collection("badges")
            .document("all")
            .update("progress", FieldValue.increment(1))
            .addOnSuccessListener(aVoid -> {
                db.collection("users")
                    .document(userId)
                    .collection("badges")
                    .document("all")
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Long progress = document.getLong("progress");
                                if (progress != null) {
                                    updatedProgress[0] = progress.intValue();
                                }

                                db.collection("badges")
                                    .document("all")
                                        .get().addOnCompleteListener(task2 -> {
                                            if (task2.isSuccessful()) {
                                                DocumentSnapshot document2 = task2.getResult();
                                                if (document2.exists()) {
                                                    List<Object> lvl0 = (List<Object>) document2.get("lvl0");
                                                    String title = "";
                                                    if (lvl0 != null && lvl0.size() > 1 && lvl0.get(1) instanceof Long) {
                                                        if (updatedProgress[0] < (Long) lvl0.get(1)) {
                                                            title = (String) lvl0.get(0);
                                                        } else {
                                                            List<Object> lvl1 = (List<Object>) document2.get("lvl1");
                                                            if (lvl1 != null && lvl1.size() > 1) {
                                                                if (updatedProgress[0] < (Long) lvl1.get(1)) {
                                                                    title = (String) lvl1.get(0);
                                                                } else {
                                                                    List<Object> lvl2 = (List<Object>) document2.get("lvl2");
                                                                    if (lvl2 != null && lvl2.size() > 1) {
                                                                        if (updatedProgress[0] < (Long) lvl2.get(1)) {
                                                                            title = (String) lvl2.get(0);
                                                                        } else {
                                                                            title = (String) ((List<?>) document2.get("lvl3")).get(0);
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    db.collection("users")
                                                            .document(userId)
                                                            .collection("badges")
                                                            .document("all")
                                                            .update("title", title);
                                                }
                                            }
                                        });
                            }
                        }
                    });
                });


        int[] updatedProgressTag = new int[1];
        try {
            db.collection("users")
                    .document(userId)
                    .collection("badges")
                    .document(tag).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                db.collection("users")
                                        .document(userId)
                                        .collection("badges")
                                        .document(tag).update("progress", FieldValue.increment(1))
                                        .addOnSuccessListener(aVoid -> {
                                            db.collection("users")
                                                    .document(userId)
                                                    .collection("badges")
                                                    .document(tag)
                                                    .get().addOnCompleteListener(task2 -> {
                                                        if (task2.isSuccessful()) {
                                                            DocumentSnapshot document2 = task2.getResult();
                                                            if (document2.exists()) {
                                                                Long progress = document2.getLong("progress");
                                                                if (progress != null) {
                                                                    updatedProgressTag[0] = progress.intValue();
                                                                }

                                                                db.collection("badges")
                                                                        .document(tag)
                                                                        .get().addOnCompleteListener(task3 -> {
                                                                            if (task3.isSuccessful()) {
                                                                                DocumentSnapshot document3 = task3.getResult();
                                                                                if (document3.exists()) {
                                                                                    List<Object> lvl0 = (List<Object>) document3.get("lvl0");
                                                                                    String title = "";
                                                                                    if (lvl0 != null && lvl0.size() > 1 && lvl0.get(1) instanceof Long) {
                                                                                        if (updatedProgressTag[0] < (Long) lvl0.get(1)) {
                                                                                            title = (String) lvl0.get(0);
                                                                                        } else {
                                                                                            List<Object> lvl1 = (List<Object>) document3.get("lvl1");
                                                                                            if (lvl1 != null && lvl1.size() > 1) {
                                                                                                if (updatedProgressTag[0] < (Long) lvl1.get(1)) {
                                                                                                    title = (String) lvl1.get(0);
                                                                                                } else {
                                                                                                    List<Object> lvl2 = (List<Object>) document3.get("lvl2");
                                                                                                    if (lvl2 != null && lvl2.size() > 1) {
                                                                                                        if (updatedProgressTag[0] < (Long) lvl2.get(1)) {
                                                                                                            title = (String) lvl2.get(0);
                                                                                                        } else {
                                                                                                            title = (String) ((List<?>) document3.get("lvl3")).get(0);
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                    db.collection("users")
                                                                                            .document(userId)
                                                                                            .collection("badges")
                                                                                            .document(tag)
                                                                                            .update("title", title);
                                                                                }
                                                                            }
                                                                        });
                                                            }
                                                        }
                                                    });
                                        });
                                ;
                                ;
                            }
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}