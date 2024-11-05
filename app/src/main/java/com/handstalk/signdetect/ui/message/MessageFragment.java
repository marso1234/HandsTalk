package com.handstalk.signdetect.ui.message;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.handstalk.signdetect.R;
import com.handstalk.signdetect.Adapter.UserAdapter;
import com.handstalk.signdetect.firebase.FriendsClient;
import com.handstalk.signdetect.model.User;
import com.handstalk.signdetect.databinding.FragmentMessageBinding;
import com.handstalk.signdetect.utilities.Constants;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class MessageFragment extends Fragment implements FriendsClient.OnFriendLoad {

    private FragmentMessageBinding binding;
    private SearchView searchView;
    private boolean isInitialized = false;
    public static ArrayList<User> friendList = new ArrayList<>();
    public static ArrayList<DocumentReference> msgRefList = new ArrayList<>();
    public static UserAdapter adapter;
    private int friendList_idx = 0;
    FirebaseFirestore db;
    private static boolean refresh=true;
    public MessageFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        binding = FragmentMessageBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        friendList_idx = 0;

        adapter = new UserAdapter(friendList, getContext());
        binding.messageRecyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        binding.messageRecyclerView.setLayoutManager(layoutManager);

        FriendsClient friendsClient = FriendsClient.getInstance();

        if(friendList.isEmpty()||refresh){
            friendsClient.fetchFriendList(this);
            refresh=false;
        }


        return binding.getRoot();
    }

    @Override
    public void onFriendLoaded(User user) {
        friendList.add(user);
        DocumentReference msgRef = db.collection(Constants.KEY_MSG_ID).document();
        adapter.notifyItemInserted(friendList_idx);
        ++friendList_idx;
    }

    public static void setRefresh(){
        refresh=true;
    }
}