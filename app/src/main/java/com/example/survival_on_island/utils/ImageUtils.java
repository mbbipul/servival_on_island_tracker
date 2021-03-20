package com.example.survival_on_island.utils;

import android.graphics.Bitmap;

public class ImageUtils {
    public  static Bitmap ResizedBitmap(Bitmap bitmap,int newWidth,int newHeight){
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

}
