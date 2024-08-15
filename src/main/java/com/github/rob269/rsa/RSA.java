package com.github.rob269.rsa;

import java.math.BigInteger;
import java.util.Random;
import java.util.logging.Logger;

public class RSA {
    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getName() + ":" + RSA.class.getName());

    private static final int DEFAULT_KEY_SIZE = 512;

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
        return String.valueOf(new BigInteger(a).modPow(key.getKey()[0], key.getKey()[1]));
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

    public static BigInteger[][] generateKeys(){
        return generateKeys(DEFAULT_KEY_SIZE);
    }
}
