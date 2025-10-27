package com.senasoft.comunidataapi.chat.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import org.springframework.web.multipart.MultipartFile;

public class ComuniDataUtil {
    public static String convertFilesToString(String[] files) throws IOException {
        StringBuilder content = new StringBuilder();

        for (String file : files) {
            content.append(convertFileToString(file));
        }

        return content.toString();
    }

    public static String convertFileToString(String fileUrl) throws IOException {
        StringBuilder content = new StringBuilder();

        // Create a URL object
        URL url = new URL(fileUrl);

        // Open a connection and create a BufferedReader
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        return content.toString();
    }

    public static String convertFileToString(MultipartFile file) throws IOException {
        StringBuilder content = new StringBuilder();

        // Open a connection and create a BufferedReader
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        return content.toString();
    }
}
