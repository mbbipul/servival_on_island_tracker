package com.example.survival_on_island.ui.Home;


import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.survival_on_island.MainActivity;
import com.example.survival_on_island.Models.Pin;
import com.example.survival_on_island.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.type.DateTime;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static android.content.ContentValues.TAG;
import static com.example.survival_on_island.utils.FirebaseUtils.FIRESTORE_PIN_REFS;
import static com.example.survival_on_island.utils.FirebaseUtils.getCurrentUser;
import static com.example.survival_on_island.utils.FirebaseUtils.isUserLoggedIn;

public class PinCreateActivity extends Activity implements View.OnClickListener {

    static final int REQUEST_IMAGE_CAPTURE = 1;

    private ImageView pinImage;
    private EditText pinTitle;
    private EditText pinDetails;
    private Button cancelPinCreate;
    private Button createPin;
    View parentLayout;

    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_create);

        intent = getIntent();

        pinImage = findViewById(R.id.pin_image);
        pinTitle = findViewById(R.id.pin_title);
        pinDetails = findViewById(R.id.pin_details);
        cancelPinCreate = findViewById(R.id.cancel_action);
        createPin = findViewById(R.id.create_pin);

        parentLayout = findViewById(R.id.content);

        pinImage.setOnClickListener(this);
        cancelPinCreate.setOnClickListener(this);
        createPin.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id){
            case R.id.pin_image:
                dispatchTakePictureIntent();
                break;

            case R.id.cancel_action:
                finish();
                break;

            case R.id.create_pin:
                submitPinToFirestore();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            pinImage.setImageBitmap(imageBitmap);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (ActivityNotFoundException e) {
            // display error state to the user
        }
    }

    private void submitPinToFirestore(){

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        StorageReference imageRef = storageRef.child("servival_ux/"+
                String.valueOf(System.currentTimeMillis())+".png");

        OnFailureListener uploadPinImageFailurListner = new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                showSnackMessage(exception.getMessage());
            }
        };

        OnSuccessListener uploadPinImageSuccesListner = new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                imageRef.getDownloadUrl()
                        .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    // Access a Cloud Firestore instance from your Activity
                                    if( !pinTitle.getText().equals("") && !pinDetails.getText().equals("")
                                            &&  isUserLoggedIn()
                                    ){
                                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                                        Pin pin = new Pin();

                                        pin.setTitle(pinTitle.getText().toString());
                                        pin.setImageUrl( uri.toString());
                                        pin.setDetails(pinDetails.getText().toString());
                                        pin.setLatitude((Double) intent.getExtras().get("latitude"));
                                        pin.setLongitude((Double) intent.getExtras().get("longitude"));

                                        pin.setCreatedBY(getCurrentUser().getUid());
                                        pin.setCreatedAt(String.valueOf(Calendar.getInstance().getTime()));

                                        db.collection(FIRESTORE_PIN_REFS)
                                                .add(pin)
                                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                    @Override
                                                    public void onSuccess(DocumentReference documentReference) {

                                                        showSnackMessage("Successfully Created Pin"+pin.getTitle());
                                                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                                                        finish();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        showSnackMessage("Something wrong to create \""+pin.getTitle()+"\" ! Please Try again");
                                                        Log.w(TAG, "Error adding document", e);
                                                    }
                                                });
                                    }else {
                                        showSnackMessage("Fill all field");
                                    }

                                }}
                            );
            }
        };


        uploadImageToFirebaseStorage(imageRef,uploadPinImageFailurListner,
                uploadPinImageSuccesListner);


    }

    private void uploadImageToFirebaseStorage(StorageReference imageRef,OnFailureListener failureListener
            ,OnSuccessListener onSuccessListener
        ){


        // Get the data from an ImageView as bytes
        pinImage.setDrawingCacheEnabled(true);
        pinImage.buildDrawingCache();
        Bitmap bitmap = ((BitmapDrawable) pinImage.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = imageRef.putBytes(data);
        uploadTask
                .addOnFailureListener(failureListener)
                .addOnSuccessListener(onSuccessListener);
    }

    private void showSnackMessage(String message){
//        Snackbar.make(parentLayout, message, Snackbar.LENGTH_LONG)
//                .setAction("CLOSE", new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//
//                    }
//                })
//                .setActionTextColor(getResources().getColor(android.R.color.holo_red_light ))
//                .show();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

}