package com.sazima.proxynt.common;

import android.os.Build;

import androidx.annotation.RequiresApi;

public class EncryptUtils {
    private static TableEncrypt tableEncrypt;

//    private TableEncrypt tableEncrypt;
    @RequiresApi(api = Build.VERSION_CODES.N)
    public byte[] encrypt(byte[] b) {
        if (tableEncrypt == null) {
            tableEncrypt = new TableEncrypt();
//            tableEncrypt.initTable();
        }
        return tableEncrypt.encrypt(b);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public byte[] decrypt(byte[] b) {
        if (tableEncrypt == null) {
            tableEncrypt = new TableEncrypt();
        }
        return tableEncrypt.decrypt(b);
    }
}
