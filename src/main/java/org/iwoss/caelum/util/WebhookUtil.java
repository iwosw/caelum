package org.iwoss.caelum.util;

import org.iwoss.caelum.CaelumConfig;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebhookUtil {
    // FIX: Dedicated daemon thread so we don't starve ForkJoinPool.commonPool()
    private static final ExecutorService WEBHOOK_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Caelum-Webhook");
        t.setDaemon(true);
        return t;
    });

    public static void sendUpdate(String message) {
        String urlString = CaelumConfig.SERVER.webhookUrl.get();
        if (urlString == null || urlString.isEmpty()) return;

        WEBHOOK_EXECUTOR.submit(() -> {
            HttpURLConnection con = null;
            try {
                URL url = new URL(urlString);
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);

                // Basic escape to not break JSON
                String safeMsg = message.replace("\"", "\\\"").replace("\n", " ");
                String json = "{\"content\": \"[Caelum Physics] " + safeMsg + "\"}";

                try (OutputStream os = con.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                con.getResponseCode();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (con != null) con.disconnect();
            }
        });
    }
}