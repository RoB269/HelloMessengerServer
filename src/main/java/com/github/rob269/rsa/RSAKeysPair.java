package com.github.rob269.rsa;

import com.github.rob269.User;
import com.google.gson.annotations.SerializedName;

import java.math.BigInteger;

public class RSAKeysPair {
    @SerializedName("public_key")
    private final UserKey publicKey;
    @SerializedName("private_key")
    private final Key privateKey;
    private final User adminId;

    public RSAKeysPair(BigInteger[][] keys, User adminId) {
        this.publicKey = addMeta(new UserKey(keys[0], adminId));
        this.privateKey = new Key(keys[1]);
        this.adminId = adminId;
    }

    public UserKey getPublicKey() {
        return publicKey;
    }

    public Key getPrivateKey() {
        return privateKey;
    }

    public User getAdminId() {
        return adminId;
    }

    private static UserKey addMeta(UserKey key) {
        return key.setMeta(new BigInteger[]{RSA.encode(key.getKey()[0].add(BigInteger.valueOf(key.getUser().hashCode())), Guarantor.getPrivateKey()),
                RSA.encode(key.getKey()[1].add(BigInteger.valueOf(key.getUser().hashCode())), Guarantor.getPrivateKey())});
    }

    @Override
    public String toString() {
        return "public_key:\n" +
                publicKey.toString() +
                "private_key:\n" +
                privateKey.toString() +
                adminId.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RSAKeysPair keys)) return false;
        return this.publicKey.equals(keys.publicKey) && this.privateKey.equals(keys.privateKey);
    }
}
