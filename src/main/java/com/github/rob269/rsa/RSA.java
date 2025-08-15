package com.github.rob269.rsa;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class RSA {
    private static final int DEFAULT_KEY_SIZE = 512;
    private static final int MAX_PACKAGE_SIZE = DEFAULT_KEY_SIZE/8;

    public static BigInteger encode(BigInteger a, Key key) {
        return a.modPow(key.getKey()[0], key.getKey()[1]);
    }

    public static String encode(int a, Key key) {
        return String.valueOf(BigInteger.valueOf(a).modPow(key.getKey()[0], key.getKey()[1]));
    }

    public static BigInteger decode(BigInteger a, Key key) {
        return a.modPow(key.getKey()[0], key.getKey()[1]);
    }

    public static String decode(String a, Key key) {
        if (a.isEmpty()) return "";
        return String.valueOf(new BigInteger(a).modPow(key.getKey()[0], key.getKey()[1]));
    }

    static final BigInteger n256 = new BigInteger("256");
    public static String encodeString(String string, Key key) {
        byte[] byteString = string.getBytes();
        BigInteger[] integerString = new BigInteger[(int) Math.ceil(byteString.length/(double) MAX_PACKAGE_SIZE)];
        Arrays.fill(integerString, BigInteger.ZERO);
        int j;
        for (int i = 0; i < byteString.length; i++) {
            j = i/MAX_PACKAGE_SIZE;
            BigInteger byteVar = BigInteger.valueOf(byteString[i]);
            if (byteVar.compareTo(BigInteger.ZERO) < 0) byteVar = byteVar.add(n256);
            integerString[j] = integerString[j].add(byteVar.multiply(n256.pow(i%MAX_PACKAGE_SIZE)));
        }
        StringBuilder builder = new StringBuilder();
        if (integerString.length > 0) {
            builder.append(encode(integerString[0], key));
            for (int i = 1; i < integerString.length; i++) {
                builder.append("/").append(encode(integerString[i], key));
            }
        }
        return builder.toString();
    }

    public static String decodeString(String string, Key key) {
        String[] strings = string.split("/");
        List<Byte> byteString = new ArrayList<>();
        for (String s : strings) {
            if (!s.isEmpty()) {
                BigInteger integer = new BigInteger(decode(s, key));
                while (integer.compareTo(BigInteger.ZERO) != 0) {
                    BigInteger num = integer.mod(n256);
                    if (num.compareTo(new BigInteger("127"))>0) num = num.subtract(n256);
                    byteString.add(num.byteValue());
                    integer = integer.divide(n256);
                }
            }
        }
        byte[] bytes = new byte[byteString.size()];
        for (int i = 0; i < byteString.size(); i++) {
            bytes[i] = byteString.get(i);
        }
        return new String(bytes);
    }

    public static byte[] encodeStringToByte(String string, Key key) {
        return encodeByteArray(string.getBytes(), key);
    }

    public static String decodeByteToString(byte[] bytes, Key key) {
        return new String(decodeByteArray(bytes, key));
    }

    public static byte[] encodeByteArray(byte[] byteString, Key key) {
        BigInteger[] integerString = new BigInteger[(int) Math.ceil((double) byteString.length / MAX_PACKAGE_SIZE)];
        Arrays.fill(integerString, BigInteger.ZERO);
        for (int i = 0, j; i < byteString.length; i++) {
            j = i/MAX_PACKAGE_SIZE;
            BigInteger byteVar = BigInteger.valueOf(byteString[i]);
            if (byteVar.compareTo(BigInteger.ZERO) < 0) byteVar = byteVar.add(n256);
            integerString[j] = integerString[j].add(byteVar.multiply(n256.pow(i%MAX_PACKAGE_SIZE)));
        }
        byte[] encodedByteString = new byte[integerString.length*130];
        for (int i = 0; i < integerString.length; i++) {
            BigInteger integer = RSA.encode(integerString[i], key);
            byte[] bytes = integer.toByteArray();
            System.arraycopy(bytes, 0, encodedByteString, 130 * i, bytes.length);
            encodedByteString[130*i+129] = (byte)(129 - bytes.length);
        }
        return encodedByteString;
    }

    public static byte[] decodeByteArray(byte[] byteString, Key key) {
        byte[] bytePackage;
        byte[] decodedByteString = new byte[byteString.length/130*64];
        int ind = 0;
        for (int i = 0; i < byteString.length/130; i++) {
            bytePackage = new byte[129 - byteString[i*130+129]];
            System.arraycopy(byteString, i*130, bytePackage, 0, bytePackage.length);
            BigInteger integer = new BigInteger(bytePackage);
            integer = decode(integer, key);
            while (integer.compareTo(BigInteger.ZERO) != 0) {
                int mod = integer.mod(n256).intValue();
                decodedByteString[ind] = (byte) mod;
                integer = integer.divide(n256);
                ind++;
            }
        }
        byte[] result = new byte[ind];
        System.arraycopy(decodedByteString, 0, result, 0, ind);
        return result;
    }


    public static BigInteger[][] generateKeys(int bitSize) {
        BigInteger p = BigInteger.probablePrime(bitSize, new Random());
        BigInteger q = BigInteger.probablePrime(bitSize, new Random());
        BigInteger N = p.multiply(q);
        BigInteger fi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));
        BigInteger e;
        do {
            e = BigInteger.probablePrime(bitSize, new Random());
        }while (!e.gcd(fi).equals(BigInteger.ONE) || e.compareTo(fi) > 0);
        BigInteger d = e.modInverse(fi);
        BigInteger[] publicKey = new BigInteger[]{e, N};
        BigInteger[] privateKey = new BigInteger[]{d, N};
        return new BigInteger[][]{publicKey, privateKey};
    }

    public static BigInteger[][] generateKeys() {
        return generateKeys(DEFAULT_KEY_SIZE);
    }
}
