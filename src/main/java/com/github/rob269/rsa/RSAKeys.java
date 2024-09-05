package com.github.rob269.rsa;

import com.github.rob269.Main;
import com.github.rob269.User;
import com.github.rob269.io.DataBaseIO;
import com.github.rob269.io.ResourcesIO;
import com.google.gson.annotations.SerializedName;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class RSAKeys {
    @SerializedName("public_key")
    private final Key publicKey;
    @SerializedName("private_key")
    private final Key privateKey;
    private final User user;

    public RSAKeys(Key publicKey, Key privateKey, User user) {
        this.publicKey = addMeta(publicKey);
        this.privateKey = addMeta(privateKey);
        this.user = user;
    }

    public RSAKeys(BigInteger[][] keys, User user) {
        this.publicKey = addMeta(new Key(keys[0], user));
        this.privateKey = addMeta(new Key(keys[1], user));
        this.user = user;
    }

    public Key getPublicKey() {
        return publicKey;
    }

    public Key getPrivateKey() {
        return privateKey;
    }

    public User getUser() {
        return user;
    }

    private static Key addMeta(Key key) {
        return key.setMeta(new BigInteger[]{new BigInteger(RSA.encode(key.getKey()[0].add(BigInteger.valueOf(key.getUser().hashCode())), Guarantor.getPrivateKey())),
                new BigInteger(RSA.encode(key.getKey()[1].add(BigInteger.valueOf(key.getUser().hashCode())), Guarantor.getPrivateKey()))});
    }

    public static boolean isKey(Key key) {
        BigInteger[] meta = key.getMeta();
        BigInteger[] keyData = key.getKey();
        BigInteger one = (new BigInteger(RSA.decode(meta[0], Guarantor.getPublicKey())).subtract(keyData[0]));
        BigInteger two = (new BigInteger(RSA.decode(meta[1], Guarantor.getPublicKey()))).subtract(keyData[1]);
        return one.compareTo(two) == 0 && one.compareTo(BigInteger.valueOf(key.getUser().hashCode())) == 0;
    }

    public static boolean isKeys(RSAKeys keys) {
        if (isKey(keys.getPublicKey()) && isKey(keys.getPrivateKey())) {
            String a = RSA.encode(1, keys.getPublicKey());
            String b = RSA.decode(a, keys.getPrivateKey());
            return "1".equals(b);
        }
        return false;
    }

    public static boolean isIdentified(Key key) {
        return Main.RSA_KEYS.isExist(1, key.getUser().getId());
    }

    public static boolean registerNewKey(Key key) {
        boolean toReturn = false;
        if (!RSAKeys.isIdentified(key)) {
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
        if (!(obj instanceof RSAKeys keys)) return false;
        return this.publicKey.equals(keys.publicKey) && this.privateKey.equals(keys.privateKey);
    }
}
