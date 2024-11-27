package com.github.rob269.io;

import com.github.rob269.logging.ConsoleFormatter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ResourcesIO {
    private static final Logger LOGGER = Logger.getLogger(ResourcesIO.class.getName());
    public static final String RESOURCES_FOLDER = "resources/";
    public static final String EXTENSION = ".json";

    public static List<String> read(String filePath) {
        List<String> lines = new ArrayList<>();
        File file = new File(RESOURCES_FOLDER + filePath);
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

    public synchronized static void delete(String filePath) {
        File file = new File(RESOURCES_FOLDER+filePath);
        if (file.exists()){
            file.delete();
        }
    }

    public synchronized static void write(String filePath, List<String> lines, boolean append) {
        File file = new File(RESOURCES_FOLDER + filePath);
        if (file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                LOGGER.warning("Can't create new file (" + filePath + ")\n" + ConsoleFormatter.formatStackTrace(e));
            }
        }
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), StandardCharsets.UTF_8));
            for (String line : lines) {
                bufferedWriter.write(line + "\n");
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
        return new File(RESOURCES_FOLDER + filePath).exists();
    }

    public synchronized static void writeJSON(String filePath, Object object) {
        File file = new File(RESOURCES_FOLDER + filePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                LOGGER.warning("Can't create new file (" + RESOURCES_FOLDER + filePath + ") " + e);
            }
        }
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
            GsonBuilder builder = new GsonBuilder();
            builder.setPrettyPrinting();
            builder.serializeNulls();
            Gson gson = builder.create();
            bufferedWriter.write(gson.toJson(object));
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            LOGGER.warning("Can't write to file (" + RESOURCES_FOLDER + filePath + ") " + e);
        }
    }

    public static <T> T readJSON(String filePath, Class<T> classOfT) {
        File file = new File(RESOURCES_FOLDER + filePath);
        if (file.exists()) {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));){
                Gson gson = new Gson();
                T object = gson.fromJson(bufferedReader, classOfT);
                return object;
            } catch (IOException e) {
                LOGGER.warning("Can't read the file (" + RESOURCES_FOLDER + filePath + ") " + e);
            }
        }
        return null;
    }
}
