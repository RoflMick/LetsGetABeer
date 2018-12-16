package com.example.mikulash.firebasechatapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.mikulash.firebasechatapp.Adapter.MessageAdapter;
import com.example.mikulash.firebasechatapp.Model.Chat;
import com.example.mikulash.firebasechatapp.Model.User;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageActivity extends AppCompatActivity {

    //Layout
    RecyclerView recyclerView;
    CircleImageView imageProfile;
    TextView username;
    ImageButton butSend;
    EditText editMessage;
    ImageButton butAddPhoto;

    //Firebase
    FirebaseUser firebaseUser;
    DatabaseReference reference;

    MessageAdapter messageAdapter;
    List<Chat> mchat;

    Intent intent;

    String userId;

    StorageReference imageStorageReference;
    private StorageTask uploadStorageTask;
    private static final int CHAT_CAMERA_REQUEST = 3;
    private Uri imageUri;

    //AlertDialog
    AlertDialog.Builder builder;
    AlertDialog alertDialog;

    //Audio
    private MediaPlayer mediaPlayer;
    private Uri userAudioUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MessageActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        //Layout init
        imageProfile = findViewById(R.id.imageProfile);
        username = findViewById(R.id.username);
        editMessage = findViewById(R.id.editMessage);
        butSend = findViewById(R.id.butSend);
        butAddPhoto = findViewById(R.id.butAddPhoto);

        //AlertDialog
        builder = new AlertDialog.Builder(getApplicationContext());
        builder.setView(R.layout.progress_bar);
        builder.setCancelable(false);
        alertDialog = builder.create();

        intent = getIntent();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        userId = intent.getStringExtra("userId");

        imageStorageReference = FirebaseStorage.getInstance().getReference("imageMessages");

//        butAddPhoto.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                openCamera();
//            }
//        });

        imageProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playAudio();
            }
        });

        butSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strMessage = editMessage.getText().toString();

                if (!strMessage.equals("")){
                    sendMessage(firebaseUser.getUid(), userId, strMessage, new Date());
                } else{
                    Toast.makeText(MessageActivity.this, "Empty message cannot be sent.", Toast.LENGTH_SHORT).show();
                }
                editMessage.setText("");
            }
        });

        reference = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                username.setText(user.getUsername());

                //Audio
                if (!user.getAudioURL().equals("default")) {
                    mediaPlayer = new MediaPlayer();
                    userAudioUri = Uri.parse(user.getAudioURL());
                }

                //Image
                if (user.getImageURL().equals("default")){
                    imageProfile.setImageResource(R.drawable.default_profile);
                } else {
                    Glide.with(getApplicationContext()).load(user.getImageURL()).into(imageProfile);
                }

                readMessage(firebaseUser.getUid(), userId, user.getImageURL());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    //Audio start playing
    private void playAudio() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.setDataSource(getApplicationContext(), userAudioUri);
                mediaPlayer.prepare();
                mediaPlayer.start();
                Toast.makeText(getApplicationContext(), "Audio is being played.", Toast.LENGTH_SHORT).show();

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Toast.makeText(getApplicationContext(), "Audio has ended.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(getApplicationContext(), "No recorded audio available.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    //    private void uploadChatImage(Intent data) {
//        alertDialog.show();
//
//        //Get the image taken from camera
//        Bundle extras = data.getExtras();
//        final Bitmap bitmap = (Bitmap) data.getExtras().get("data");
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
//        byte[] dataBAOS = baos.toByteArray();
//
//        final StorageReference bitmapStorageReference = imageStorageReference.child(System.currentTimeMillis() + "_cameraBitmap");
//
//        //Put image to Firebase
//        uploadStorageTask = bitmapStorageReference.putBytes(dataBAOS);
//        uploadStorageTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
//            @Override
//            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
//                if (!task.isSuccessful()) {
//                    throw task.getException();
//                }
//                return bitmapStorageReference.getDownloadUrl();
//            }
//        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
//            @Override
//            public void onComplete(@NonNull Task<Uri> task) {
//                if (task.isSuccessful()) {
//                    Uri downloadUri = task.getResult();
//                    String strUri = downloadUri.toString();
//
//                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
//
//                    HashMap<String, Object> hashMap = new HashMap<>();
//                    hashMap.put("from", firebaseUser.getUid());
//                    hashMap.put("to", userId);
//                    hashMap.put("message", strUri);
//                    hashMap.put("dateTime", new Date());
//                    hashMap.put("type", "image");
//
//                    reference.child("Chats").push().setValue(hashMap);
//
//                    reference.updateChildren(hashMap);
//
//                    alertDialog.dismiss();
//                }
//            }
//        });
//    }
//
//    private void openCamera() {
//        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        startActivityForResult(intent, CHAT_CAMERA_REQUEST);
//    }

    public void sendMessage(String from, String to, String message, Date dateTime){

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("from", from);
        hashMap.put("to", to);
        hashMap.put("message", message);
        hashMap.put("dateTime", dateTime);
        hashMap.put("type", "text");

        reference.child("Chats").push().setValue(hashMap);
    }

    private void readMessage(final String myId, final String userId, final String imageURL){
        mchat = new ArrayList<>();

        reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mchat.clear();
                for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat.getTo().equals(myId) && chat.getFrom().equals(userId) || chat.getTo().equals(userId) && chat.getFrom().equals(myId)){
                        mchat.add(chat);
                    }

                    messageAdapter = new MessageAdapter(MessageActivity.this, mchat, imageURL);
                    recyclerView.setAdapter(messageAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        Toast.makeText(this, requestCode + " " + resultCode + " " + data, Toast.LENGTH_SHORT).show();
//        //Chat Photo Upload
//        if (requestCode == CHAT_CAMERA_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
//            imageUri = data.getData();
//            Toast.makeText(this, "request if", Toast.LENGTH_SHORT).show();
//            if (uploadStorageTask != null && uploadStorageTask.isInProgress()) {
//                Toast.makeText(getApplicationContext(), "Chat Photo Uploading is in progress.", Toast.LENGTH_SHORT).show();
//            } else {
//                Toast.makeText(this, "else", Toast.LENGTH_SHORT).show();
//                uploadChatImage(data);
//            }
//        }
//    }
}
