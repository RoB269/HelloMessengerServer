package com.github.rob269.rsa;

import com.github.rob269.Main;
import com.github.rob269.User;
import com.google.gson.annotations.SerializedName;

import java.math.BigInteger;

public class RSAKeysPair {
    @SerializedName("public_key")
    private final UserKey publicKey;
    @SerializedName("private_key")
    private final Key privateKey;
    private final User user;

    public RSAKeysPair(UserKey publicKey, Key privateKey, User user) {
        this.publicKey = addMeta(publicKey);
        this.privateKey = privateKey;
        this.user = user;
    }

    public RSAKeysPair(BigInteger[][] keys, User user) {
        this.publicKey = addMeta(new UserKey(keys[0], user));
        this.privateKey = new Key(keys[1]);
        this.user = user;
    }

    public UserKey getPublicKey() {
        return publicKey;
    }

    public Key getPrivateKey() {
        return privateKey;
    }

    public User getUser() {
        return user;
    }

    private static UserKey addMeta(UserKey key) {
        return key.setMeta(new BigInteger[]{RSA.encode(key.getKey()[0].add(BigInteger.valueOf(key.getUser().hashCode())), Guarantor.getPrivateKey()),
                RSA.encode(key.getKey()[1].add(BigInteger.valueOf(key.getUser().hashCode())), Guarantor.getPrivateKey())});
    }

    public static boolean isKey(UserKey key) {
        BigInteger[] meta = key.getMeta();
        BigInteger[] keyData = key.getKey();
        BigInteger one = (RSA.decode(meta[0], Guarantor.getPublicKey())).subtract(keyData[0]);
        BigInteger two = (RSA.decode(meta[1], Guarantor.getPublicKey())).subtract(keyData[1]);
        return one.compareTo(two) == 0 && one.compareTo(BigInteger.valueOf(key.getUser().hashCode())) == 0;
    }

    public static boolean isKeys(RSAKeysPair keys) {
        if (isKey(keys.getPublicKey())) {
            String a = RSA.encode(1, keys.getPublicKey());
            String b = RSA.decode(a, keys.getPrivateKey());
            return "1".equals(b);
        }
        return false;
    }

    public static boolean isIdentified(UserKey key) {
        return Main.RSA_KEYS.isExist(4, key.getUser().getId());
    }

    public static boolean registerNewKey(UserKey key, boolean safeReg) {
        boolean toReturn = false;
        if (!safeReg || !RSAKeysPair.isIdentified(key)) {
            key = addMeta(key);
            Main.RSA_KEYS.write(key.toString().split("\n"));
            toReturn = true;
        }
        return toReturn;
    }

    @Override
    public String toString() {
        return "public_key:\n" +
                publicKey.toString() +
                "private_key:\n" +
                privateKey.toString() +
                user.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RSAKeysPair keys)) return false;
        return this.publicKey.equals(keys.publicKey) && this.privateKey.equals(keys.privateKey);
    }
}
