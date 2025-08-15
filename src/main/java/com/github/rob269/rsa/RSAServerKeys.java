package com.github.rob269.rsa;

import com.github.rob269.User;
import com.github.rob269.io.ResourcesIO;

import java.math.BigInteger;
import java.util.logging.Logger;

public class RSAServerKeys {
    private static RSAKeysPair serverKeys;
    private static final User SERVER = new User("#SERVER#");
    private static final Logger LOGGER = Logger.getLogger(RSAServerKeys.class.getName());

    public static void initKeys() {
        if (ResourcesIO.isExist("RSA/serverKeys.json")) {
            try {
                RSAKeysPair serverKeys = ResourcesIO.readJSON("RSA/serverKeys.json", RSAKeysPair.class);
                if (serverKeys == null || serverKeys.getAdminId() == null) {
                    throw new NullPointerException();
                }
                RSAServerKeys.serverKeys = serverKeys;
                LOGGER.fine("The keys have been read");
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
        LOGGER.fine("The keys were generated and written down");
    }

    public static UserKey getPublicKey() {
        return serverKeys.getPublicKey();
    }

    public static Key getPrivateKey() {
        return serverKeys.getPrivateKey();
    }
}
