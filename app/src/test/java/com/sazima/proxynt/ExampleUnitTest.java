package com.sazima.proxynt;

import org.junit.Test;

import static org.junit.Assert.*;

import com.android.tools.r8.com.google.common.primitives.Longs;
import com.sazima.proxynt.common.EncryptUtils;
import com.sazima.proxynt.common.StringTranslator;
import com.sazima.proxynt.common.TypeConversion;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;

import javax.xml.bind.DatatypeConverter;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
        byte[] helloworlds = new EncryptUtils().encrypt("helloworld".getBytes(StandardCharsets.UTF_8));

        byte[] res = new EncryptUtils().decrypt(helloworlds);
//        String table = "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9fa0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff";
//        byte[] tableHex = DatatypeConverter.parseHexBinary(table);
//        byte[] md5 = getMd5("hello world");
//        byte[] b1 = new byte[8];
//        byte[] b2 = new byte[8];
//        System.arraycopy(md5, 0, b1, 0, 8);
//        System.arraycopy(md5, 8, b2, 0, 8);
//        long long1 = TypeConversion.bytesToInt64(b1, 0);
//        long long2 = TypeConversion.bytesToInt64(b2, 0);
//        BigInteger bigInteger1 = parseBigIntegerPositive(String.valueOf(long1));
//        BigInteger bigInteger2 = parseBigIntegerPositive(String.valueOf(long2));
////        byte tableHex1 = tableHex[1];
////        Arrays.sort(tableHex, (Byte a, Byte b) -> 0);
//        Byte[] tableHex1 = toObjects(tableHex);
//        for (int i = 1; i < 1024; i ++ ) {
//            int finalI = i;
//            Arrays.sort(tableHex1, new Comparator<Byte>() {
//                @Override
//                public int compare(Byte o1, Byte o2) {
//                    try {
//                        BigInteger d1 = bigInteger1.mod(BigInteger.valueOf(Byte.toUnsignedInt(o1) + finalI));
//                        BigInteger d2 = bigInteger1.mod(BigInteger.valueOf(Byte.toUnsignedInt(o2) + finalI));
//                        return d1.compareTo(d2);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        return 1;
//                    }
//                }
//            });
//        }

//        String lo = new StringTranslator("!0", "lo").translate("He!!0 W0r!d");
//        System.out.println("---------------" + lo);
    }
    private byte[] getMd5(String yourString)  {
        byte[] bytesOfMessage = yourString.getBytes();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] theMD5digest = md.digest(bytesOfMessage);
        byte[] newB = new byte[16];
        return theMD5digest;
//        ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        for (int i= 0 ; i<16; i++){
//            int i1 = theMD5digest[i] & 0xFF;
////            stream.write(i1);
//            newB[i] = (byte) (i1);
//        }
//        return newB;
    }
    private byte[] reverse(byte[] b) {
        byte[] newBytes = new byte[8];
        for (int i = 0; i < b.length; i++) {
            newBytes[ 7 -i] = b[i];

        }
        return newBytes;

    }
    private static final BigInteger TWO_COMPL_REF = BigInteger.ONE.shiftLeft(64);

    public static BigInteger parseBigIntegerPositive(String num) {
        BigInteger b = new BigInteger(num);
        if (b.compareTo(BigInteger.ZERO) < 0)
            b = b.add(TWO_COMPL_REF);
        return b;
    }
    Byte[] toObjects(byte[] bytesPrim) {
        Byte[] bytes = new Byte[bytesPrim.length];
        Arrays.setAll(bytes, n -> bytesPrim[n]);
        return bytes;
    }
//    public static long toUnsignedIn64(byte[] bytes)
//    {
//        long value = 0;
//        byte[] newBytes = new byte[8];
//        for (int i = 0; i < bytes.length; i++)
//        {
//            value = (value << 8) + (bytes[i] & 0xff);
//            newBytes[ 7 -i] = bytes[i];
//
//        }
//        long l = new BigInteger(bytes).longValue();
//        Longs.fromByteArray(newBytes);
//        return value;
//    }

}