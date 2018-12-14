package com.example.mikulash.firebasechatapp.Fragments;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.gms.tasks.OnSuccessListener;
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
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends Fragment {

    //TODO
    Button butOpenCamera;

    //Layout
    CircleImageView imageProfile;
    TextView username;
    ProgressBar progressBar;
    Button butDefaultImage;
    Button butRecordAudio;
    Button butPlayAudio;
    Button butStopAudio;
    TextView debugtext;

    //Audio
    private MediaRecorder mediaRecorder;
    private String audioFileName = null;
    StorageReference audioStorageReference;
    private Uri audioUri;
    private MediaPlayer mediaPlayer;

    //Firebase
    DatabaseReference reference;
    FirebaseUser firebaseUser;

    //Storage
    StorageReference storageReference;
    private static final int PROFILE_IMAGE_REQUEST = 1;
    private static final int PROFILE_CAMERA_REQUEST = 2;
    private Uri imageUri;
    private StorageTask uploadStorageTask;

    //AlertDialog
    AlertDialog.Builder builder;
    AlertDialog alertDialog;

    //Accelerometer
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private SensorEventListener accelerometerSensorEventListener;
    private float mAccel; //acceleration apart from gravity
    private float mAccelCurrent; //current acceleration including gravity
    private float mAccelLast; //last acceleration including gravity

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        //XML init
        imageProfile = view.findViewById(R.id.imageProfile);
        username = view.findViewById(R.id.username);
        progressBar = view.findViewById(R.id.progressBar);
        butDefaultImage = view.findViewById(R.id.butDefaultImage);
        butOpenCamera = view.findViewById(R.id.butOpenCamera);
        butRecordAudio = view.findViewById(R.id.butRecordAudio);
        butPlayAudio = view.findViewById(R.id.butPlayAudio);
        butStopAudio = view.findViewById(R.id.butStopAudio);
        butStopAudio.setEnabled(false);
        debugtext = view.findViewById(R.id.debugtext);

        //Audio
        audioFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        audioFileName += "/recorded_audio.3gp";
        audioStorageReference = FirebaseStorage.getInstance().getReference();

        //Firebase init
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());
        storageReference = FirebaseStorage.getInstance().getReference("imageProfiles");

        //AlertDialog
        builder = new AlertDialog.Builder(getContext());
        builder.setView(R.layout.progress_bar);
        builder.setCancelable(false);
        alertDialog = builder.create();

        //Accelerometer
        sensorManager = (SensorManager) getContext().getSystemService(getContext().SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
        if (accelerometerSensor == null) {
            Toast.makeText(getContext(), "Device does not support Accelerometer features.", Toast.LENGTH_SHORT).show();
        }

        //TODO
        butOpenCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });

        //Accelerometer on shake
        accelerometerSensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                mAccelLast = mAccelCurrent;
                mAccelCurrent = (float) Math.sqrt((double) (x*x + y*y + z*z));
                float delta = mAccelCurrent - mAccelLast;
                mAccel = mAccel * 0.9f + delta; // perform low-cut filter

                if (mAccel > 15) {
                    openCamera();
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        butPlayAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                butStopAudio.setEnabled(true);
                playAudio();
            }
        });

        butStopAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                butStopAudio.setEnabled(false);
                stopAudio();
            }
        });

        butRecordAudio.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startRecording();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    stopRecording();
                }
                return false;
            }
        });

        //On Default Image Button Clicked
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
                    imageProfile.setImageResource(R.drawable.default_profile);
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

        //On Image Clicked
        imageProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageSelection();
            }
        });

        return view;
    }

    //Audio stop playing
    private void stopAudio() {
        mediaPlayer.stop();
        Toast.makeText(getContext(), "Audio stopped.", Toast.LENGTH_SHORT).show();
    }

    //Audio start playing
    private void playAudio() {
        mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(getContext(), audioUri);
            mediaPlayer.prepare();
            mediaPlayer.start();

            Toast.makeText(getContext(), "Audio is being played.", Toast.LENGTH_SHORT).show();

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mediaPlayer.release();
                    mediaPlayer = null;

                    Toast.makeText(getContext(), "Audio has ended.", Toast.LENGTH_SHORT).show();
                    butStopAudio.setEnabled(false);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Audio start recording
    private void startRecording() {
        Toast.makeText(getContext(), "Audio recording has started...", Toast.LENGTH_SHORT).show();

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(audioFileName);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            Toast.makeText(getContext(), "prepare() failed" + e, Toast.LENGTH_SHORT).show();
        }

        mediaRecorder.start();
    }

    //Audio stop recording
    private void stopRecording() {
        Toast.makeText(getContext(), "Audio recording has stopped.", Toast.LENGTH_SHORT).show();

        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;

        uploadAudio();
    }

    //Audio upload to Firebase
    private void uploadAudio() {
        alertDialog.show();

        StorageReference fileStorageReference = audioStorageReference.child("Audio").child("audio_" + System.currentTimeMillis() + ".3gp");
        audioUri = Uri.fromFile(new File(audioFileName));

        fileStorageReference.putFile(audioUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                alertDialog.dismiss();
                Toast.makeText(getContext(), "Audio recording successfully uploaded.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, PROFILE_CAMERA_REQUEST);
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(accelerometerSensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(accelerometerSensorEventListener);
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
            alertDialog.show();

            final StorageReference fileStorageReference = storageReference.child(System.currentTimeMillis() + "." + getFileExtension(imageUri));

            //Put image to Firebase
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

    private void uploadCameraImage(Intent data) {
        alertDialog.show();

        //Get the image taken from camera
        Bundle extras = data.getExtras();
        final Bitmap bitmap = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] dataBAOS = baos.toByteArray();

        final StorageReference bitmapStorageReference = storageReference.child(System.currentTimeMillis() + "_cameraBitmap");

        //Put image to Firebase
        uploadStorageTask = bitmapStorageReference.putBytes(dataBAOS);
        uploadStorageTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return bitmapStorageReference.getDownloadUrl();
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
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Image Upload
        if (requestCode == PROFILE_IMAGE_REQUEST && resultCode == RESULT_OK && data!= null && data.getData() != null) {
            imageUri = data.getData();
            if (uploadStorageTask != null && uploadStorageTask.isInProgress()) {
                Toast.makeText(getContext(), "Image Uploading is in progress.", Toast.LENGTH_SHORT).show();
            } else {
                uploadImage();
            }
        }

        //Camera Image Upload
        if (requestCode == PROFILE_CAMERA_REQUEST && resultCode == RESULT_OK) {
            imageUri = data.getData();
            if (uploadStorageTask != null && uploadStorageTask.isInProgress()) {
                Toast.makeText(getContext(), "Image Uploading is in progress.", Toast.LENGTH_SHORT).show();
            } else {
                uploadCameraImage(data);
            }
//            fileStorageReference.putFile(photoURI).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//                @Override
//                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                    alertDialog.dismiss();
//                    Toast.makeText(getContext(), "succesdasfsdf", Toast.LENGTH_SHORT).show();
//                }
//            });

            /*
            if (uploadStorageTask != null && uploadStorageTask.isInProgress()) {
                Toast.makeText(getContext(), "Photo Uploading is in progress.", Toast.LENGTH_SHORT).show();
            } else {
                uploadImage();
            }*/
        }
    }
}
