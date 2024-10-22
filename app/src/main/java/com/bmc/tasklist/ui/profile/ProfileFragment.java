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

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        ProfileViewModel profileViewModel =
                new ViewModelProvider(this).get(ProfileViewModel.class);

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        TopProfile topProfile = binding.topProfile;
        topProfile = new TopProfile(getContext());

        return view;
    }
}
