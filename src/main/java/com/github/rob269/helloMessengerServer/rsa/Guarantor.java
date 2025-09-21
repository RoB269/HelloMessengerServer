package com.github.rob269.helloMessengerServer.rsa;

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

    static Key getPrivateKey() {
        return privateKey;
    }
}
