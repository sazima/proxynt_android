package com.sazima.proxynt.common;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class StringTranslator {
    private Map<Byte, Byte> translationMap;

    public StringTranslator() {
    }

    public StringTranslator(Byte[] from, Byte[] to) {
        translationMap = new HashMap<>();
        if (from.length != to.length)
            throw new IllegalArgumentException("The from and to strings must be of the same length");

        for (int i = 0; i < from.length; i++)
            translationMap.put(from[i],  to[i]);
    }
    public byte[] translate(byte[] b) {
        ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
        for (int i = 0; i < b.length; i++) {
//            char ch = (char) b[i];
            Byte replacement = translationMap.get(b[i]);
            if (replacement != null)
            bodyStream.write(replacement );
        }
        return bodyStream.toByteArray();
    }

//    public String translate(String str, String deleteChars) {
//        StringBuilder buffer = new StringBuilder(str);
//        char[] deletions = deleteChars.toCharArray();
//        for (char ch : deletions) {
//            int index;
//            if ((index = buffer.indexOf("" + ch)) != -1)
//                buffer.deleteCharAt(index);
//        }
//
//        return translate(buffer.toString());
//    }
}