package com.example.mikulash.firebasechatapp.Fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.mikulash.firebasechatapp.Model.User;
import com.example.mikulash.firebasechatapp.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
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

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends Fragment {

    //Layout
    CircleImageView imageProfile;
    TextView username;
    ProgressBar progressBar;
    Button butDefaultImage;

    //Firebase
    DatabaseReference reference;
    FirebaseUser firebaseUser;

    //Storage
    StorageReference storageReference;
    private static final int PROFILE_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private StorageTask uploadStorageTask;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        imageProfile = view.findViewById(R.id.imageProfile);
        username = view.findViewById(R.id.username);
        progressBar = view.findViewById(R.id.progressBar);
        butDefaultImage = view.findViewById(R.id.butDefaultImage);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());
        storageReference = FirebaseStorage.getInstance().getReference("imageProfiles");

        //Button Default Image
        butDefaultImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reference.child("imageURL").setValue("default");
                Toast.makeText(getContext(), "Profile picture has been set to default.", Toast.LENGTH_SHORT).show();
            }
        });

        //On Image changed
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                username.setText(user.getUsername());
                if (user.getImageURL().equals("default")) {
                    imageProfile.setImageResource(R.mipmap.ic_launcher);
                } else {
                    if (getActivity() == null) {
                        return;
                    }
                    Glide.with(getContext()).load(user.getImageURL()).into(imageProfile);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        imageProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageSelection();
            }
        });

        return view;
    }

    private void openImageSelection () {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PROFILE_IMAGE_REQUEST);
    }

    private String getFileExtension (Uri imageUri) {
        ContentResolver contentResolver = getContext().getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(imageUri));
    }

    private void uploadImage () {
        if (imageUri != null) {

            //Alert builder
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setView(R.layout.progress_bar);
            builder.setCancelable(false);
            final AlertDialog alertDialog = builder.create();
            alertDialog.show();

            final StorageReference fileStorageReference = storageReference.child(System.currentTimeMillis() + "." + getFileExtension(imageUri));

            uploadStorageTask = fileStorageReference.putFile(imageUri);
            uploadStorageTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return fileStorageReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        String strUri = downloadUri.toString();

                        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("imageURL", strUri);
                        reference.updateChildren(map);

                        alertDialog.dismiss();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    alertDialog.dismiss();
                }
            });
        } else {
            Toast.makeText(getContext(), "Image needs to be selected.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PROFILE_IMAGE_REQUEST && resultCode == RESULT_OK && data!= null && data.getData() != null) {
            imageUri = data.getData();
            if (uploadStorageTask != null && uploadStorageTask.isInProgress()) {
                Toast.makeText(getContext(), "Uploading is in progress.", Toast.LENGTH_SHORT).show();
            } else {
                uploadImage();
            }
        }
    }
}
