package com.github.rob269.rsa;

import com.github.rob269.User;

import java.math.BigInteger;

public class UserKey extends Key{
    private final BigInteger[] meta = new BigInteger[2];
    private User user;

    public UserKey(BigInteger[] key, User user) {
        super(key);
        this.user = user;
    }

    public UserKey(BigInteger[] key, BigInteger[] meta, User user) {
        super(key);
        this.meta[0] = meta[0];
        this.meta[1] = meta[1];
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public BigInteger[] getMeta() {
        return meta;
    }

    public UserKey setMeta(BigInteger[] meta) {
        this.meta[0] = meta[0];
        this.meta[1] = meta[1];
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UserKey val)) return false;
        return this.getKey()[0].compareTo(val.getKey()[0]) == 0 && this.getKey()[1].compareTo(val.getKey()[1]) == 0
                && this.meta[0].compareTo(val.getMeta()[0]) == 0 && this.meta[1].compareTo(val.getMeta()[1]) == 0
                && this.user.equals(val.user);
    }

    @Override
    public String toString() {
        return getKey()[0] + "\n" + getKey()[1] + "\n" + meta[0] + "\n" + meta[1] + "\n" + user.toString() + "\n";
    }
}
