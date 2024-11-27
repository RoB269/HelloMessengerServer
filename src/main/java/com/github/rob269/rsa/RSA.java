package com.github.rob269.rsa;

import com.github.rob269.Main;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class RSA {
    private static final int DEFAULT_KEY_SIZE = 512;
    private static final int MAX_PACKAGE_SIZE = 64;

    public static String encode(BigInteger a, Key key) {
        return String.valueOf(a.modPow(key.getKey()[0], key.getKey()[1]));
    }

    public static String encode(int a, Key key) {
        return String.valueOf(BigInteger.valueOf(a).modPow(key.getKey()[0], key.getKey()[1]));
    }

    public static String decode(BigInteger a, Key key) {
        return String.valueOf(a.modPow(key.getKey()[0], key.getKey()[1]));
    }

    public static String decode(int a, Key key) {
        return String.valueOf(BigInteger.valueOf(a).modPow(key.getKey()[0], key.getKey()[1]));
    }

    public static String decode(String a, Key key) {
        if (a.isEmpty()) return "";
        return String.valueOf(new BigInteger(a).modPow(key.getKey()[0], key.getKey()[1]));
    }

    static final BigInteger n256 = new BigInteger("256");
    public synchronized static String encodeString(String string, Key key) {
        byte[] byteString = string.getBytes();
        BigInteger[] integerString = new BigInteger[(int) Math.ceil(byteString.length/(double) MAX_PACKAGE_SIZE)];
        Arrays.fill(integerString, BigInteger.ZERO);
        int j;
        for (int i = 0; i < byteString.length; i++) {
            j = i/MAX_PACKAGE_SIZE;
            BigInteger byteVar = BigInteger.valueOf(byteString[i]);
            if (byteVar.compareTo(BigInteger.ZERO) < 0) byteVar = byteVar.add(new BigInteger("256"));
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

    public synchronized static String decodeString(String string, Key key) {
        String[] strings = string.split("/");
        List<Byte> byteString = new ArrayList<>();
        for (String s : strings) {
            if (!s.isEmpty()) {
                BigInteger integer = new BigInteger(decode(s, key));
                while (integer.compareTo(BigInteger.ZERO) != 0) {
                    BigInteger num = integer.mod(n256);
                    if (num.compareTo(new BigInteger("127"))>0) num = num.subtract(new BigInteger("256"));
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

    public synchronized static BigInteger[][] generateKeys(int bitSize) {
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

    public static BigInteger[][] generateKeys(){
        return generateKeys(DEFAULT_KEY_SIZE);
    }
}
