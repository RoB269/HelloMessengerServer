package com.github.rob269.rsa;

import com.github.rob269.User;
import com.github.rob269.io.ResourcesInterface;

import java.math.BigInteger;

public class Key {
    private final BigInteger[] key = new BigInteger[2];
    private final BigInteger[] meta = new BigInteger[2];
    private User user;

    public Key(BigInteger[] key, User user) {
        this.key[0] = key[0];
        this.key[1] = key[1];
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public Key() {}

    public BigInteger[] getKey() {
        return key;
    }

    public BigInteger[] getMeta() {
        return meta;
    }

    public Key setMeta(BigInteger[] meta) {
        this.meta[0] = meta[0];
        this.meta[1] = meta[1];
        return this;
    }

    public boolean isAuthenticated() {
        Key val = ResourcesInterface.readJSON("RSA/clients/" + this.getUser().getId() + ResourcesInterface.EXTENSION, Key.class);
        return this.equals(val);
    }

    @Override
    public String toString() {
        return key[0] + "\n" + key[1] + "\n" + meta[0] + "\n" + meta[1] + "\n" + user.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Key val)) return false;
        return this.key[0].compareTo(val.getKey()[0]) == 0 && this.key[1].compareTo(val.getKey()[1]) == 0
                && this.meta[0].compareTo(val.getMeta()[0]) == 0 && this.meta[1].compareTo(val.getMeta()[1]) == 0
                && this.user.equals(val.user);
    }
}
