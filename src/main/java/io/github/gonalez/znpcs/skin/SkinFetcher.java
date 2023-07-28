package io.github.gonalez.znpcs.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SkinFetcher {
  private static final ExecutorService SKIN_EXECUTOR_SERVICE = Executors.newCachedThreadPool();
  
  private static final JsonParser JSON_PARSER = new JsonParser();
  
  private final SkinFetcherBuilder builder;
  
  public SkinFetcher(SkinFetcherBuilder builder) {
    this.builder = builder;
  }
  
  public CompletableFuture<JsonObject> doReadSkin(SkinFetcherResult skinFetcherResult) {
    CompletableFuture<JsonObject> completableFuture = new CompletableFuture<>();
    SKIN_EXECUTOR_SERVICE.submit(() -> {
          try {
            HttpURLConnection connection = (HttpURLConnection)(new URL(this.builder.getAPIServer().getURL() + getData())).openConnection();
            connection.setRequestMethod(this.builder.getAPIServer().getMethod());
            if (this.builder.isUrlType()) {
              connection.setDoOutput(true);
              DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
              try {
                outputStream.writeBytes("url=" + URLEncoder.encode(this.builder.getData(), StandardCharsets.UTF_8));
                outputStream.close();
              } catch (Throwable throwable) {
                try {
                  outputStream.close();
                } catch (Throwable throwable1) {
                  throwable.addSuppressed(throwable1);
                } 
                throw throwable;
              } 
            } 
            try {
              Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
              try {
                completableFuture.complete(JSON_PARSER.parse(reader).getAsJsonObject());
                reader.close();
              } catch (Throwable throwable) {
                try {
                  reader.close();
                } catch (Throwable throwable1) {
                  throwable.addSuppressed(throwable1);
                } 
                throw throwable;
              } 
            } finally {
              connection.disconnect();
            } 
          } catch (Throwable throwable) {
            completableFuture.completeExceptionally(throwable);
          } 
        });
    completableFuture.whenComplete((response, throwable) -> {
          if (completableFuture.isCompletedExceptionally()) {
            skinFetcherResult.onDone(null, throwable);
          } else {
            JsonArray properties = response.getAsJsonArray("properties");
            JsonObject textures = properties.get(0).getAsJsonObject();
            String value = textures.get("value").getAsString();
            String signature = textures.get("signature").getAsString();
            skinFetcherResult.onDone(new String[] { value, signature }, null);
          } 
        });
    return completableFuture;
  }
  
  private String getData() {
    return this.builder.isProfileType() ? ("/" + this.builder.getData() + "?unsigned=false") : "";
  }
}
