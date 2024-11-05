package com.handstalk.signdetect.ui.facetime;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.handstalk.signdetect.Adapter.VideoCallAdapter;
import com.handstalk.signdetect.databinding.FragmentFacetimeBinding;
import com.handstalk.signdetect.firebase.FriendsClient;
import com.handstalk.signdetect.model.User;
import com.handstalk.signdetect.utilities.MyLogging;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.ArrayList;

public class FacetimeFragment extends Fragment implements MyLogging, FriendsClient.OnFriendLoad {

    private FragmentFacetimeBinding binding;
    public static ArrayList<User> friendList = new ArrayList<>();
    private final String TAG = "Face Time";
    private FirebaseFirestore mDatabase;
    private boolean isInitialized = false;
    private int friendList_idx = 0;
    public static VideoCallAdapter adapter;
    private static boolean refresh;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentFacetimeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        friendList_idx = 0; // Keep 0 Every time it is called

        mDatabase = FirebaseFirestore.getInstance();

        adapter = new VideoCallAdapter(friendList, getContext());
        binding.videoCallRecyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        binding.videoCallRecyclerView.setLayoutManager(layoutManager);

        FriendsClient friendsClient = FriendsClient.getInstance();

        if(friendList.isEmpty()||refresh){
            friendsClient.fetchFriendList(this);
            refresh=false;
        }

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void Logging(String debugMsg, String userMsg) {
        Log.d(TAG, debugMsg);

        //Skip User Message If Empty
        if(userMsg.isEmpty()) return;
        Toast.makeText(getContext(), userMsg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFriendLoaded(User user) {
        friendList.add(user);
        adapter.notifyItemInserted(friendList_idx);
        ++friendList_idx;
    }
    public static void setRefresh(){
        refresh=true;
    }
}