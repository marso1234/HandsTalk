package com.handstalk.signdetect.Adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.handstalk.signdetect.model.User;
import com.handstalk.signdetect.R;
import com.handstalk.signdetect.utilities.AndroidUtil;
import com.handstalk.signdetect.utilities.FirebaseUtil;
import com.handstalk.signdetect.activities.ChatDetailActivity;

import java.util.ArrayList;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder>{

    ArrayList<User> list;
    Context context;

    public UserAdapter(ArrayList<User> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //set the view to see the users
        View view = LayoutInflater.from(context).inflate(R.layout.sample_show_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //fetch user data (image & name) into the recycle view
        User user = list.get(position);
        //get authId
        FirebaseUtil.getUserId(user.getUserId()).get().addOnCompleteListener(task -> {
            String receiverAuthId = task.getResult().getString("auth_id");
            Log.d("onBindViewHolder:", receiverAuthId);
            //set user image into the recycle view
            FirebaseUtil.getOtherProfilePicStorageRef(receiverAuthId).getDownloadUrl()
                    .addOnCompleteListener(t -> {
                        if(t.isSuccessful()){
                            Uri uri  = t.getResult();
                            AndroidUtil.setProfilePic(context,uri,holder.image);
                        }
                    });
        });

        holder.userName.setText(user.getUsername());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, ChatDetailActivity.class);
                intent.putExtra("User",user);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        ImageView image;
        TextView userName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            image = itemView.findViewById(R.id.profile_image);
            userName = itemView.findViewById(R.id.userNameList);
        }
    }
}
