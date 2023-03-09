package com.sazima.proxynt.common;

import android.os.Build;

import androidx.annotation.RequiresApi;

public class EncryptUtils {
//    private static TableEncrypt tableEncrypt;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static byte[] encrypt(byte[] b) {
        return TableEncrypt.encrypt(b);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static byte[] decrypt(byte[] b) {
        return TableEncrypt.decrypt(b);
    }
}
