package com.github.rob269.helloMessengerServer;

import com.github.rob269.helloMessengerServer.io.ResourcesIO;
import com.github.rob269.helloMessengerServer.rsa.Guarantor;
import com.github.rob269.helloMessengerServer.rsa.Key;
import com.github.rob269.helloMessengerServer.rsa.RSAServerKeys;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class HMPConfig implements Config{
    private static final Logger LOGGER = Logger.getLogger(HMPConfig.class.getName());
    private static final List<String> configList = List.of(new String[]{"guarantor_private_key", "guarantor_public_key", "port"});

    @Override
    public void parseConfigFiles() {
        if (!ResourcesIO.isExist("config")) {
            ResourcesIO.write("config", new ArrayList<>());
            LOGGER.severe("The configuration file does not exists");
            throw new RuntimeException();
        }
        Map<String, String[]> values = new HashMap<>();
        StringBuilder builder = new StringBuilder();
        for (String line : ResourcesIO.read("config")) builder.append(line);
        String[] lines = builder.toString().replaceAll(" ", "").split(";");
        for (String line : lines) {
            for (String config : configList) {
                if (line.startsWith(config)) {
                    values.put(config, line.split("=")[1].split(","));
                }
            }
        }
        if (values.containsKey("guarantor_public_key") && values.containsKey("guarantor_private_key")) {
            Guarantor.init(new Key(new BigInteger[]{new BigInteger(values.get("guarantor_public_key")[0]), new BigInteger(values.get("guarantor_public_key")[1])}),
                    new Key(new BigInteger[]{new BigInteger(values.get("guarantor_private_key")[0]), new BigInteger(values.get("guarantor_private_key")[1])}));
            if (values.containsKey("port")) {
                Main.setPort(Integer.parseInt(values.get("port")[0]));
            }
        }
        else {
            LOGGER.severe("The configuration file does not contain the necessary data");
            throw new RuntimeException();
        }
        RSAServerKeys.initKeys();
    }
}
