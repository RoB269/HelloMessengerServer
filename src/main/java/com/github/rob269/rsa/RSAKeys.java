package com.github.rob269.rsa;

import com.github.rob269.User;
import com.github.rob269.io.ResourcesInterface;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.math.BigInteger;
import java.util.logging.Logger;

public class RSAKeys {
    @SerializedName("public_key")
    private Key publicKey;
    @SerializedName("private_key")
    private Key privateKey;
    private User user;
    
    private static final int DEFAULT_KEY_SIZE = 512;
    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getName() + ":" + RSAKeys.class.getName());

    public RSAKeys(Key publicKey, Key privateKey, User user) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.user = user;
        addMeta();
    }

    public RSAKeys(BigInteger[][] keys, User user) {
        this.publicKey = new Key(keys[0], user);
        this.privateKey = new Key(keys[1], user);
        this.user = user;
        addMeta();
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

    private void addMeta() {
        publicKey.setMeta(new BigInteger[]{new BigInteger(RSA.encode(publicKey.getKey()[0].add(BigInteger.valueOf(user.hashCode())), Guarantor.getPrivateKey())),
                new BigInteger(RSA.encode(publicKey.getKey()[1].add(BigInteger.valueOf(user.hashCode())), Guarantor.getPrivateKey()))});
        privateKey.setMeta(new BigInteger[]{new BigInteger(RSA.encode(privateKey.getKey()[0].add(BigInteger.valueOf(user.hashCode())), Guarantor.getPrivateKey())),
                new BigInteger(RSA.encode(privateKey.getKey()[1].add(BigInteger.valueOf(user.hashCode())), Guarantor.getPrivateKey()))});
    }

    public static boolean isKey(Key key) {
        BigInteger[] meta = key.getMeta();
        BigInteger[] keyData = key.getKey();
        BigInteger one = (new BigInteger(RSA.decode(meta[0], Guarantor.getPublicKey())).subtract(keyData[0]));
        BigInteger two = (new BigInteger(RSA.decode(meta[1], Guarantor.getPublicKey()))).subtract(keyData[1]);
        return one.compareTo(two) == 0 && one.compareTo(BigInteger.valueOf(key.getUser().hashCode())) == 0;
    }

    public static boolean isRegistered(RSAKeys keys) {
        if (isKey(keys.getPublicKey()) && isKey(keys.getPrivateKey())) {
            String a = RSA.encode(1, keys.getPublicKey());
            String b = RSA.decode(a, keys.getPrivateKey());
            if (String.valueOf(1).equals(b)) {
                File keyFile = new File(ResourcesInterface.ROOT_FOLDER + "RSA/clients/" + keys.getUser().getId() + ".json");
                return keyFile.exists();
            }
        }
        return false;
    }

    public static boolean registerNewKeys(RSAKeys keys) {
        boolean toReturn = false;
        if (!isRegistered(keys)) {
            ResourcesInterface.writeJSON("RSA/clients/" + keys.getUser().getId() + ".json", keys);
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
}
