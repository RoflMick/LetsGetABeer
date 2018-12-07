package com.example.mikulash.firebasechatapp.Adapter;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.mikulash.firebasechatapp.MessageActivity;
import com.example.mikulash.firebasechatapp.Model.Chat;
import com.example.mikulash.firebasechatapp.Model.User;
import com.example.mikulash.firebasechatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    public static final int MSG_LEFT = 0;
    public static final int MSG_RIGHT = 1;
    private Context mContext;
    private List<Chat> mChat;
    private String imageURL;

    private SimpleDateFormat simpleDateFormat;

    FirebaseUser firebaseUser;

    public MessageAdapter(Context mContext, List<Chat> mChat, String imageURL) {
        this.mContext = mContext;
        this.mChat = mChat;
        this.imageURL = imageURL;
    }

    @NonNull
    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_RIGHT){
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_right, parent, false);
            return new MessageAdapter.ViewHolder(view);
        } else{
            View view = LayoutInflater.from(mContext).inflate(R.layout.chat_item_left, parent, false);
            return new MessageAdapter.ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder holder, int position) {

        Chat chat = mChat.get(position);

        holder.show_message.setText(chat.getMessage());

        simpleDateFormat = new SimpleDateFormat("HH:mm   dd.MM. yyyy");
        holder.show_time.setText(simpleDateFormat.format(chat.getDateTime()));

        if (imageURL.equals("default")){
            holder.imageProfile.setImageResource(R.mipmap.ic_launcher);
        } else{
            Glide.with(mContext).load(imageURL).into(holder.imageProfile);
        }
    }

    @Override
    public int getItemCount() {
        return mChat.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        public TextView show_message;
        public TextView show_time;
        public ImageView imageProfile;

        public ViewHolder(View itemView) {
            super(itemView);

            show_message = itemView.findViewById(R.id.show_message);
            show_time = itemView.findViewById(R.id.show_time);
            imageProfile = itemView.findViewById(R.id.imageProfile);
        }
    }

    @Override
    public int getItemViewType(int position) {
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (mChat.get(position).getFrom().equals(firebaseUser.getUid())){
            return MSG_RIGHT;
        } else{
            return MSG_LEFT;
        }
    }
}