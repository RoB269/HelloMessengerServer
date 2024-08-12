package com.github.rob269.rsa;

import java.math.BigInteger;

public class Key {
    private BigInteger[] key = new BigInteger[2];
    private BigInteger[] meta = new BigInteger[2];

    public Key(BigInteger[] key) {
        this.key = key;
        meta[0] = new BigInteger(RSA.encode(key[0], Guarantor.getPrivateKey()));
        meta[1] = new BigInteger(RSA.encode(key[1], Guarantor.getPrivateKey()));
    }

    public Key() {}

    public void setKey(BigInteger[] key) {
        this.key = key;
        meta[0] = new BigInteger(RSA.encode(key[0], Guarantor.getPrivateKey()));
        meta[1] = new BigInteger(RSA.encode(key[1], Guarantor.getPrivateKey()));
    }

    public BigInteger[] getKey() {
        return key;
    }

    public BigInteger[] getMeta() {
        return meta;
    }
}
