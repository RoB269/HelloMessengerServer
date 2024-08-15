package com.github.rob269.rsa;

import com.github.rob269.User;

import java.math.BigInteger;

public class Key {
    private BigInteger[] key = new BigInteger[2];
    private final BigInteger[] meta = new BigInteger[2];
    private User user;

    public Key(BigInteger[] key, User user) {
        this.key = key;
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public Key() {}

    public void setKey(BigInteger[] key) {
        this.key = key;
    }

    public BigInteger[] getKey() {
        return key;
    }

    public BigInteger[] getMeta() {
        return meta;
    }

    protected void setMeta(BigInteger[] meta) {
        this.meta[0] = meta[0];
        this.meta[1] = meta[1];
    }

    @Override
    public String toString() {
        return key[0] + "\n" + key[1] + "\n" + meta[0] + "\n" + meta[1] + "\n";
    }
}
