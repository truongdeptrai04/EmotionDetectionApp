package com.example.smartclassemotion.utils;

public interface OnImageUploadedCallback {
    void onImageUploaded(String imageUrl);
    void onError(Exception e);
}
