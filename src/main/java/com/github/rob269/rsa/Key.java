package com.github.rob269.rsa;

import com.github.rob269.Main;
import com.github.rob269.User;
import com.github.rob269.io.ResourcesIO;

import java.math.BigInteger;
import java.util.List;

public class Key {
    private final BigInteger[] key = new BigInteger[2];
    private final BigInteger[] meta = new BigInteger[2];
    private User user;

    public Key(BigInteger[] key, User user) {
        this.key[0] = key[0];
        this.key[1] = key[1];
        this.user = user;
    }

    public Key(BigInteger[] key, BigInteger[] meta, User user) {
        this.key[0] = key[0];
        this.key[1] = key[1];
        this.meta[0] = meta[0];
        this.meta[1] = meta[1];
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
        List<String> entry = Main.RSA_KEYS.readLine(5, user.getId());
        Key val = new Key(new BigInteger[]{new BigInteger(entry.get(1)), new BigInteger(entry.get(2))},
                new BigInteger[]{new BigInteger(entry.get(3)), new BigInteger(entry.get(4))}, new User(entry.get(5)));
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
