package com.github.rob269.rsa;

import com.github.rob269.User;
import com.github.rob269.io.ResourcesIO;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

public class Guarantor {
    private static Key privateKey = null;

    private static Key publicKey = null;

    public static void init(Key publicKey, Key privateKey) {
        if (Guarantor.publicKey == null || Guarantor.privateKey == null){
            Guarantor.publicKey = publicKey;
            Guarantor.privateKey = privateKey;
        }
    }

    public static Key getPublicKey() {
        return publicKey;
    }

    protected static Key getPrivateKey() {
        return privateKey;
    }
}
