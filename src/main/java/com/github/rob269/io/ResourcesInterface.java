package com.github.rob269.io;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ResourcesInterface {
    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getName() + ":" + ResourcesInterface.class.getName());
    private static final String rootFolder = "resources/";

    public static List<String> read(String filePath) {
        List<String> lines = new ArrayList<>();
        File file = new File(rootFolder + filePath);
        if (file.exists()) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    lines.add(line);
                }
                bufferedReader.close();
            } catch (IOException e) {
                LOGGER.warning("Can't read the file (" + filePath + ") " + e);
            }
        }
        else {
            LOGGER.warning("File is not exist (" + filePath + ")");
        }
        return lines;
    }

    public synchronized static void write(String filePath, List<String> lines, boolean append) {
        File file = new File(rootFolder + filePath);
        if (file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                LOGGER.warning("Can't create new file (" + filePath + ") " + e);
            }
        }
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), StandardCharsets.UTF_8));
            for (int i = 0; i < lines.size(); i++) {
                bufferedWriter.write(lines.get(i) + "\n");
            }
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            LOGGER.warning("Can't write to file (" + filePath + ") " + e);
        }
    }

    public synchronized static void write(String filePath, List<String> lines) {
        write(filePath, lines, false);
    }

    public static boolean isExist(String filePath) {
        return new File(rootFolder + filePath).exists();
    }

    public synchronized static void writeJSON(String filePath, JSONObject jsonObject) {
        File file = new File(rootFolder + filePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                LOGGER.warning("Can't create new file (" + rootFolder + filePath + ") " + e);
            }
        }
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
            bufferedWriter.write(String.valueOf(jsonObject));
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            LOGGER.warning("Can't write to file (" + rootFolder + filePath + ") " + e);
        }
    }

    public static JSONObject readJSON(String filePath) {
        File file = new File(rootFolder + filePath);
        if (file.exists()) {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));){
                Object o = new JSONParser().parse(bufferedReader);
                return (JSONObject) o;
            } catch (IOException e) {
                LOGGER.warning("Can't read the file (" + rootFolder + filePath + ") " + e);
            } catch (ParseException e) {
                LOGGER.warning("Can't read JSON object (" + rootFolder + filePath + ") " + e);
            }
        }
        return new JSONObject();
    }
}
