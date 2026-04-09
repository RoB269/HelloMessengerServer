package com.github.rob269.helloMessengerServer.rsa;

import com.github.rob269.helloMessengerServer.User;
import com.github.rob269.helloMessengerServer.io.ResourcesIO;

import java.math.BigInteger;
import java.util.logging.Logger;

public class RSAServerKeys {
    private static final Logger LOGGER = Logger.getLogger(RSAServerKeys.class.getName());
    private static RSAKeysPair serverKeys;
    private static final User SERVER = new User("#SERVER#");

    public static void initKeys() {
        if (ResourcesIO.isExist("RSA/serverKeys.json")) {
            try {
                RSAKeysPair serverKeys = ResourcesIO.readJSON("RSA/serverKeys.json", RSAKeysPair.class);
                if (serverKeys == null || serverKeys.getAdmin() == null) {
                    throw new NullPointerException();
                }
                RSAServerKeys.serverKeys = serverKeys;
            } catch (NullPointerException e) {
                LOGGER.warning("Keys not found");
                generateNewKeys();
            }
        }
        else {
            generateNewKeys();
        }
        LOGGER.fine("The keys have been initialized");
    }

    private static void generateNewKeys() {
        BigInteger[][] keys = RSA.generateKeys();
        RSAServerKeys.serverKeys = new RSAKeysPair(keys, SERVER);
        ResourcesIO.writeJSON("RSA/serverKeys.json", serverKeys);
        LOGGER.fine("The keys were generated and written into the key's file");
    }

    public static UserKey getPublicKey() {
        return serverKeys.getPublicKey();
    }

    public static Key getPrivateKey() {
        return serverKeys.getPrivateKey();
    }
}
