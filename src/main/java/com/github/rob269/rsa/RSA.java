package com.github.rob269.rsa;

import java.math.BigInteger;
import java.util.Random;
import java.util.logging.Logger;

public class RSA {
    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getName() + ":" + RSA.class.getName());

    public static String encode(BigInteger a, BigInteger[] key) {
        return String.valueOf(a.modPow(key[0], key[1]));
    }

    public static String encode(int a, BigInteger[] key) {
        return String.valueOf(BigInteger.valueOf(a).modPow(key[0], key[1]));
    }

    public static String decode(BigInteger a, BigInteger[] key) {
        return String.valueOf(a.modPow(key[0], key[1]));
    }

    public static String decode(int a, BigInteger[] key) {
        return String.valueOf(BigInteger.valueOf(a).modPow(key[0], key[1]));
    }

    public static BigInteger[][] generateKeys() {
        BigInteger p = BigInteger.probablePrime(1024, new Random());
        BigInteger q = BigInteger.probablePrime(1024, new Random());
        BigInteger N = p.multiply(q);
        BigInteger fi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));
        BigInteger e;
        do {
            e = BigInteger.probablePrime(1024, new Random());
        }while (!e.gcd(fi).equals(BigInteger.ONE) || e.compareTo(fi) > 0);
        BigInteger d = e.modInverse(fi);
        BigInteger[] publicKey = new BigInteger[]{e, N};
        BigInteger[] privateKey = new BigInteger[]{d, N};
        return new BigInteger[][]{publicKey, privateKey};
    }
}
