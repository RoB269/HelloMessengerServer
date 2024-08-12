package com.github.rob269.rsa;

import com.github.rob269.io.ResourcesInterface;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.util.logging.Logger;

public class RSAServerKeys {
    private static final Key publicKey = new Key();
    private static final Key privateKey = new Key();

    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getName() + ":" + RSAServerKeys.class.getName());

    public static void initKeys() {
        if (ResourcesInterface.isExist("RSA/serverKeys.json")) {
            try {
                JSONObject serverKeys = ResourcesInterface.readJSON("RSA/serverKeys.json");
                publicKey.setKey(new BigInteger[]{new BigInteger((String) ((JSONArray) serverKeys.get("publicKey")).get(0)),
                        new BigInteger((String) ((JSONArray) serverKeys.get("publicKey")).get(1))});
                privateKey.setKey(new BigInteger[]{new BigInteger((String) ((JSONArray) serverKeys.get("privateKey")).get(0)),
                        new BigInteger((String) ((JSONArray) serverKeys.get("privateKey")).get(1))});
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
        publicKey.setKey(keys[0]);
        privateKey.setKey(keys[1]);
        JSONObject serverKeysJSON = new JSONObject();
        JSONArray publicKeyJSONArray = new JSONArray();
        JSONArray privateKeyJSONArray = new JSONArray();
        publicKeyJSONArray.add(String.valueOf(publicKey.getKey()[0]));
        publicKeyJSONArray.add(String.valueOf(publicKey.getKey()[1]));
        privateKeyJSONArray.add(String.valueOf(privateKey.getKey()[0]));
        privateKeyJSONArray.add(String.valueOf(privateKey.getKey()[1]));
        serverKeysJSON.put("publicKey", publicKeyJSONArray);
        serverKeysJSON.put("privateKey", privateKeyJSONArray);
        ResourcesInterface.writeJSON("RSA/serverKeys.json", serverKeysJSON);
        LOGGER.fine("The keys were generated and written down");
    }

    public static Key getPublicKey() {
        return publicKey;
    }

    public static Key getPrivateKey() {
        return privateKey;
    }
}
