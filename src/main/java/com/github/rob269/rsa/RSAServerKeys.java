package com.github.rob269.rsa;

import com.github.rob269.User;
import com.github.rob269.io.ResourcesInterface;

import java.math.BigInteger;
import java.util.logging.Logger;

public class RSAServerKeys {
    private static RSAKeys serverKeys;
    private static final User SERVER = new User("#SERVER#");
    private static final Logger LOGGER = Logger.getLogger(RSAServerKeys.class.getName());

    public static void initKeys() {
        if (ResourcesInterface.isExist("RSA/serverKeys.json")) {
            try {
                RSAKeys serverKeys = ResourcesInterface.readJSON("RSA/serverKeys.json", RSAKeys.class);
                if (serverKeys == null || serverKeys.getUser() == null) {
                    throw new NullPointerException();
                }
                RSAServerKeys.serverKeys = serverKeys;
                LOGGER.fine("The keys have been read");
            } catch (NullPointerException e) {
                LOGGER.warning("Keys not found");
                writeNewKeys();
            }
        }
        else {
            writeNewKeys();
        }
        LOGGER.fine("The keys have been initialized");
    }

    private static void writeNewKeys() {
        BigInteger[][] keys = RSA.generateKeys();
        RSAServerKeys.serverKeys = new RSAKeys(keys, SERVER);
        ResourcesInterface.writeJSON("RSA/serverKeys.json", serverKeys);
        LOGGER.fine("The keys were generated and written down");
    }

    public static Key getPublicKey() {
        return serverKeys.getPublicKey();
    }

    public static Key getPrivateKey() {
        return serverKeys.getPrivateKey();
    }
}
