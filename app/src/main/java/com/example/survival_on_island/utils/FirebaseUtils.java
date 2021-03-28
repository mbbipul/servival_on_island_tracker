package com.example.survival_on_island.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseUtils {

    public static final String FIRESTORE_PIN_REFS = "pins";
    public static final String FIRESTORE_USERS_REFS = "users";
    public static final String DATABASE_USER_REFS ="ux_users_location";
    public static FirebaseUser getCurrentUser(){
        return FirebaseAuth.getInstance().getCurrentUser();
    }
    public static boolean isUserLoggedIn(){
        if(getCurrentUser() != null)
            return true;
        return false;
    }
}
