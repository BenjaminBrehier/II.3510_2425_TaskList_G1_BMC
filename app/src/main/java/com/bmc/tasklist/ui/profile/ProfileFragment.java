package com.bmc.tasklist.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bmc.tasklist.R;
import com.bmc.tasklist.databinding.FragmentDashboardBinding;
import com.bmc.tasklist.databinding.FragmentProfileBinding;
import com.bmc.tasklist.ui.dashboard.DashboardViewModel;
import com.bmc.tasklist.ui.home.TaskCard;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;

import java.util.Locale;

public class ProfileFragment extends Fragment {
    private FirebaseFirestore db;
    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        String userId;
        if (user != null) {
            userId = user.getUid();
        } else {
            userId = "toto";
        }

        ProfileViewModel profileViewModel =
                new ViewModelProvider(this).get(ProfileViewModel.class);

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        TopProfile topProfile = binding.topProfile;
        topProfile = new TopProfile(getContext());

        db.collection("users")
            .document(userId)
            .collection("badges")
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String badgeId = document.getId();
                        Long progress = document.getLong("progress");

                        if (progress != null && progress > 0) {
                            String title = document.getString("title");
                            Long level = document.getLong("level");

                            // Vérifier si "title" et "level" ne sont pas nuls avant de les utiliser
                            if (title != null && level != null) {
                                BadgeCard badgeCard = new BadgeCard(getContext());
                                badgeCard.setBadgeName(title);
                                badgeCard.setProgressText("LEVEL " + level);
                                String desc = badgeId.equals("all") ? "" : badgeId.toUpperCase() + " ";
                                badgeCard.setBadgeDesc("Finish " + desc + "tasks." + "("+progress+" for now)");
                                binding.taskListLayout.addView(badgeCard);
                            }
                        }
                    }
                }
            });


        return view;
    }
}
