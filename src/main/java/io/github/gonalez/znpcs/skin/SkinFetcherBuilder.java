package io.github.gonalez.znpcs.skin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SkinFetcherBuilder {
  private static final Gson gson = new Gson();

  private final SkinServer apiServer;
  
  private final String name;
  
  protected SkinFetcherBuilder(SkinServer apiServer, String name) {
    this.apiServer = apiServer;
    this.name = name;
  }
  
  public SkinServer getAPIServer() {
    return this.apiServer;
  }
  
  public String getData() {
    if (isProfileType()) {
      try {
        HttpURLConnection connection = (HttpURLConnection)(new URL(SkinServer.UUID_API.getURL())).openConnection();
        connection.setRequestMethod(SkinServer.UUID_API.getMethod());
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
        connection.connect();

        String body = "[\"%s\"]".formatted(this.name);
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes(body);  // FUCK YOU URLEncoder.encode()
        outputStream.close();

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK){
          Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
          List<ProfileJson> profileJsonList = gson.fromJson(reader, new TypeToken<List<ProfileJson>>(){}.getType());
          ProfileJson profileJson = profileJsonList.get(0);
          assert profileJson.name.equals(this.name);
          return profileJson.id;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return this.name;
  }
  
  public boolean isUrlType() {
    return (this.apiServer == SkinServer.GENERATE_API);
  }
  
  public boolean isProfileType() {
    return (this.apiServer == SkinServer.PROFILE_API);
  }

  public boolean isUuidType() {
    return (this.apiServer == SkinServer.UUID_API);
  }
  
  public static SkinFetcherBuilder create(SkinServer skinAPIURL, String name) {
    return new SkinFetcherBuilder(skinAPIURL, name);
  }
  
  public static SkinFetcherBuilder withName(String name) {
    return create(name.startsWith("http") ? SkinServer.GENERATE_API : SkinServer.PROFILE_API, name);
  }
  
  public SkinFetcher toSkinFetcher() {
    return new SkinFetcher(this);
  }
  
  public enum SkinServer {
    UUID_API("POST", "https://mcskin.kazuhahub.com/api/yggdrasil/api/profiles/minecraft"),
    PROFILE_API("GET", "https://mcskin.kazuhahub.com/api/yggdrasil/sessionserver/session/minecraft/profile"),  // ?unsigned=false
    GENERATE_API("POST", "https://api.mineskin.org/generate/url");
    
    private final String method;
    
    private final String url;
    
    SkinServer(String method, String url) {
      this.method = method;
      this.url = url;
    }
    
    public String getMethod() {
      return this.method;
    }
    
    public String getURL() {
      return this.url;
    }
  }

  private static class ProfileJson {
    public String id;
    public String name;
  }
}
