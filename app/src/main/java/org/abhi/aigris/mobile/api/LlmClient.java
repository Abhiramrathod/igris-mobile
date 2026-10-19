package org.abhi.aigris.mobile.api;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.abhi.aigris.mobile.data.PrefsManager;
import org.abhi.aigris.mobile.model.ChatMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LlmClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int MAX_HISTORY = 30;

    public interface ResponseCallback {
        void onSuccess(String response);

        void onError(String message);
    }

    public interface ModelsCallback {
        void onModels(List<String> models);

        void onError(String message);
    }

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void sendChat(List<ChatMessage> history, PrefsManager prefs, String sessionId,
                         ResponseCallback callback) {
        executor.execute(() -> {
            try {
                String response;
                if (prefs.isBackendMode()) {
                    response = sendViaBackend(history, prefs, sessionId);
                } else {
                    response = sendDirect(history, prefs);
                }
                final String result = response;
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                final String message = e.getMessage() != null ? e.getMessage() : "Unknown error";
                mainHandler.post(() -> callback.onError(message));
            }
        });
    }

    public void fetchModels(PrefsManager prefs, ModelsCallback callback) {
        executor.execute(() -> {
            try {
                List<String> models = prefs.isBackendMode()
                        ? fetchModelsViaBackend(prefs)
                        : fetchModelsDirect(prefs);
                final List<String> result = models;
                mainHandler.post(() -> callback.onModels(result));
            } catch (Exception e) {
                final String message = e.getMessage() != null ? e.getMessage() : "Unknown error";
                mainHandler.post(() -> callback.onError(message));
            }
        });
    }

    private String sendDirect(List<ChatMessage> history, PrefsManager prefs) throws IOException {
        String url = prefs.getEffectiveBaseUrl() + "/chat/completions";

        JsonObject root = new JsonObject();
        root.addProperty("model", prefs.getModel());
        root.addProperty("temperature", prefs.getTemperature());
        root.addProperty("max_tokens", prefs.getMaxTokens());
        root.addProperty("stream", false);

        JsonArray messages = new JsonArray();
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", prefs.getSystemPrompt());
        messages.add(system);

        List<ChatMessage> limited = history.size() > MAX_HISTORY
                ? history.subList(history.size() - MAX_HISTORY, history.size())
                : history;
        for (ChatMessage message : limited) {
            JsonObject item = new JsonObject();
            item.addProperty("role", message.getRole());
            item.addProperty("content", message.getContent());
            messages.add(item);
        }
        root.add("messages", messages);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + prefs.getApiKey())
                .post(RequestBody.create(root.toString(), JSON))
                .build();

        try (Response response = http.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("Error " + response.code() + ": " + extractError(body));
            }
            return parseContent(body);
        }
    }

    private String sendViaBackend(List<ChatMessage> history, PrefsManager prefs, String sessionId)
            throws IOException {
        String url = prefs.getEffectiveBaseUrl() + "/ai/chat";

        String prompt = "";
        for (int i = history.size() - 1; i >= 0; i--) {
            if (ChatMessage.ROLE_USER.equals(history.get(i).getRole())) {
                prompt = history.get(i).getContent();
                break;
            }
        }

        JsonObject root = new JsonObject();
        root.addProperty("userName", "guest");
        root.addProperty("sessionId", sessionId);
        root.addProperty("prompt", prompt);
        root.addProperty("model", prefs.getModel());

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(root.toString(), JSON))
                .build();

        try (Response response = http.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("Error " + response.code() + ": " + extractError(body));
            }
            return body.trim().isEmpty() ? "No response received." : body.trim();
        }
    }

    private List<String> fetchModelsDirect(PrefsManager prefs) throws IOException {
        String url = prefs.getEffectiveBaseUrl() + "/models";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + prefs.getApiKey())
                .get()
                .build();
        return executeModelRequest(request, "data");
    }

    private List<String> fetchModelsViaBackend(PrefsManager prefs) throws IOException {
        String url = prefs.getEffectiveBaseUrl() + "/ai/models";
        Request request = new Request.Builder().url(url).get().build();
        return executeModelRequest(request, "models");
    }

    private List<String> executeModelRequest(Request request, String primaryField)
            throws IOException {
        try (Response response = http.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("Error " + response.code() + ": " + extractError(body));
            }
            return parseModels(body, primaryField);
        }
    }

    private static String parseContent(String body) {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                JsonObject first = choices.get(0).getAsJsonObject();
                if (first.has("message") && first.get("message").isJsonObject()) {
                    JsonElement content = first.getAsJsonObject("message").get("content");
                    if (content != null && content.isJsonPrimitive()) {
                        return content.getAsString();
                    }
                }
                if (first.has("text") && first.get("text").isJsonPrimitive()) {
                    return first.get("text").getAsString();
                }
            }
        } catch (Exception ignored) {
        }
        return "No response received.";
    }

    private static List<String> parseModels(String body, String primaryField) {
        Set<String> models = new LinkedHashSet<>();
        try {
            JsonElement parsed = JsonParser.parseString(body);
            JsonArray array = null;
            if (parsed.isJsonObject()) {
                JsonObject object = parsed.getAsJsonObject();
                if (object.has(primaryField) && object.get(primaryField).isJsonArray()) {
                    array = object.getAsJsonArray(primaryField);
                } else {
                    for (String key : new String[]{"data", "models"}) {
                        if (object.has(key) && object.get(key).isJsonArray()) {
                            array = object.getAsJsonArray(key);
                            break;
                        }
                    }
                }
            } else if (parsed.isJsonArray()) {
                array = parsed.getAsJsonArray();
            }
            if (array != null) {
                for (JsonElement element : array) {
                    if (element.isJsonPrimitive()) {
                        models.add(element.getAsString());
                    } else if (element.isJsonObject()) {
                        JsonObject item = element.getAsJsonObject();
                        if (item.has("id") && item.get("id").isJsonPrimitive()) {
                            models.add(item.get("id").getAsString());
                        } else if (item.has("name") && item.get("name").isJsonPrimitive()) {
                            models.add(item.get("name").getAsString());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return new ArrayList<>(models);
    }

    private static String extractError(String body) {
        if (body == null || body.trim().isEmpty()) {
            return "Request failed";
        }
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            if (root.has("error")) {
                JsonElement error = root.get("error");
                if (error.isJsonObject() && error.getAsJsonObject().has("message")) {
                    return error.getAsJsonObject().get("message").getAsString();
                }
                if (error.isJsonPrimitive()) {
                    return error.getAsString();
                }
            }
            if (root.has("message") && root.get("message").isJsonPrimitive()) {
                return root.get("message").getAsString();
            }
        } catch (Exception ignored) {
        }
        return body.length() > 300 ? body.substring(0, 300) + "…" : body;
    }
}
