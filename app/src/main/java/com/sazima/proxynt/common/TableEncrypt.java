package com.sazima.proxynt.common;

import android.os.Build;
import android.security.identity.IdentityCredentialStore;

import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;

import javax.xml.bind.DatatypeConverter;

public class TableEncrypt {
    private static final BigInteger TWO_COMPL_REF = BigInteger.ONE.shiftLeft(64);
    private static StringTranslator encryptTranslator ;
    private static StringTranslator decryptTranslator ;
//    private String key = "hello world";

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void initTable(String key) {
//        if (encryptTranslator != null) {
//            return;
//        }
        String table = "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9fa0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff";
        byte[] tableHex = DatatypeConverter.parseHexBinary(table);
        byte[] md5 = getMd5(key);
        byte[] b1 = new byte[8];
        byte[] b2 = new byte[8];
        System.arraycopy(md5, 0, b1, 0, 8);
        System.arraycopy(md5, 8, b2, 0, 8);
        long long1 = TypeConversion.bytesToInt64(b1, 0);
        long long2 = TypeConversion.bytesToInt64(b2, 0);
        BigInteger bigInteger1 = parseBigIntegerPositive(String.valueOf(long1));
        BigInteger bigInteger2 = parseBigIntegerPositive(String.valueOf(long2));
        Byte[] tableHex1 = toObjects(tableHex);
        for (int i = 1; i < 1024; i++) {
            int finalI = i;
            Arrays.sort(tableHex1, new Comparator<Byte>() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public int compare(Byte o1, Byte o2) {
                        BigInteger d1 = bigInteger1.mod(BigInteger.valueOf(Byte.toUnsignedInt(o1) + finalI));
                        BigInteger d2 = bigInteger1.mod(BigInteger.valueOf(Byte.toUnsignedInt(o2) + finalI));
                        return d1.compareTo(d2);
//                    catch (Exception e) {
//                        e.printStackTrace();
//                        return 1;
                    }
            });
        }
//        encryptTranslator = StringTranslator()
        byte[] originTableHex = DatatypeConverter.parseHexBinary(table);
        encryptTranslator = new StringTranslator(toObjects(originTableHex), tableHex1);
        decryptTranslator = new StringTranslator(tableHex1, toObjects(originTableHex));
    }
    public byte[] encrypt(byte[] b) {
        return encryptTranslator.translate(b);
    }
    public byte[] decrypt(byte[] b) {
        return decryptTranslator.translate(b);
    }

    /**
     * long 转成unsignLong (bigInt)
     *
     * @param num
     * @return
     */
    private static BigInteger parseBigIntegerPositive(String num) {
        BigInteger b = new BigInteger(num);
        if (b.compareTo(BigInteger.ZERO) < 0)
            b = b.add(TWO_COMPL_REF);
        return b;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private Byte[] toObjects(byte[] bytesPrim) {
        Byte[] bytes = new Byte[bytesPrim.length];
        Arrays.setAll(bytes, n -> bytesPrim[n]);
        return bytes;
    }

    private byte[] getMd5(String yourString) {
        byte[] bytesOfMessage = yourString.getBytes();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return md.digest(bytesOfMessage);
    }
}
