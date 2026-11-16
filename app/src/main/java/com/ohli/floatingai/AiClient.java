package com.ohli.floatingai;

import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Sends prompts through the private gateway. Provider keys never live in the APK. */
final class AiClient {
    private static final int TIMEOUT_MS = 65000;
    private AiClient() { }

    static String ask(String provider, String prompt, File image, List<ChatMessage> history) throws Exception {
        String gateway = BuildConfig.AI_GATEWAY_URL == null ? "" : BuildConfig.AI_GATEWAY_URL.trim();
        if (gateway.isEmpty() || !gateway.startsWith("https://")) {
            throw new IllegalStateException("Gateway AI belum dipasang. APK tidak meminta API key; pemilik aplikasi perlu mengaktifkan gateway aman terlebih dahulu.");
        }
        JSONObject body = new JSONObject();
        body.put("provider", "Gemini".equals(provider) ? "gemini" : "deepseek");
        body.put("prompt", prompt == null ? "" : prompt);
        JSONArray messages = new JSONArray();
        if (history != null) {
            for (ChatMessage item : history) {
                JSONObject message = new JSONObject();
                message.put("role", "user".equals(item.role) ? "user" : "assistant");
                message.put("text", item.text == null ? "" : item.text);
                messages.put(message);
            }
        }
        body.put("history", messages);
        if (image != null && image.isFile()) {
            JSONObject imageObject = new JSONObject();
            imageObject.put("mimeType", "image/jpeg");
            imageObject.put("data", encodeFile(image));
            body.put("image", imageObject);
        }
        JSONObject result = post(gateway.replaceAll("/+$", "") + "/v1/chat", body.toString());
        String answer = result.optString("answer", "").trim();
        if (answer.isEmpty()) throw new IllegalStateException("Gateway menerima permintaan tetapi jawaban AI kosong.");
        return answer;
    }

    private static JSONObject post(String endpoint, String body) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(payload.length);
            try (OutputStream out = connection.getOutputStream()) { out.write(payload); }
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
            String response = readAll(stream);
            JSONObject json;
            try { json = new JSONObject(response); }
            catch (Exception ex) { throw new IllegalStateException("Gateway mengirim respons tidak valid (HTTP " + status + ")."); }
            if (status < 200 || status >= 300) {
                JSONObject error = json.optJSONObject("error");
                String message = error == null ? json.optString("message", "Permintaan AI gagal (HTTP " + status + ").") : error.optString("message", "Permintaan AI gagal (HTTP " + status + ").");
                throw new IllegalStateException(message);
            }
            return json;
        } finally { if (connection != null) connection.disconnect(); }
    }

    private static String encodeFile(File file) throws Exception {
        try (FileInputStream in = new FileInputStream(file); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
        }
    }

    private static String readAll(InputStream input) throws Exception {
        if (input == null) return "{}";
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            return out.toString("UTF-8");
        }
    }
}
