package com.jeremyroman.gcmfilepush.android;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.Scanner;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class ClientUtils {
  private static final String LOG_TAG = ClientUtils.class.getSimpleName();

  /**
   * Performs an HTTP request to the server.
   */
  public static HttpEntity executeHttpRequest(HttpUriRequest request) {
    HttpClient http = new DefaultHttpClient();
    try {
      HttpResponse response = http.execute(request);
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode < 200 || statusCode >= 300) {
        Log.e(LOG_TAG, String.format("unexpected %d %s",
            statusCode, response.getStatusLine().getReasonPhrase()));
        return null;
      }
      return response.getEntity();
    } catch (IOException e) {
      Log.e(LOG_TAG, "error during HTTP request", e);
      return null;
    }
  }

  /**
   * Retrieves the client configuration object from the server.
   * Unfortunately, this is blocking.
   */
  public static JSONObject getClientConfiguration(Context context) {
    URI configUri = getServerUri(context, "client-config");
    HttpEntity body = executeHttpRequest(new HttpGet(configUri));
    if (body == null) {
      Log.e(LOG_TAG, "config empty");
      return null;
    }

    try {
      Scanner scanner = new Scanner(body.getContent()).useDelimiter("\\A");
      if (scanner.hasNext()) {
        return new JSONObject(scanner.next());
      } else {
        Log.e(LOG_TAG, "config empty");
      }
    } catch (IOException e) {
      Log.e(LOG_TAG, "config reading error", e);
    } catch (JSONException e) {
      Log.e(LOG_TAG, "config parsing error", e);
    }
    return null;
  }

  /**
   * Generates a device ID.
   */
  public synchronized static String getDeviceId(Context context) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    String deviceId = preferences.getString("device_id", null);
    if (deviceId == null) {
      String chars = "123456789ABCDEFGHJKLMNPQRSTUVWXY";
      byte[] randomBytes = new byte[12];
      new SecureRandom().nextBytes(randomBytes);
      StringBuilder builder = new StringBuilder(12);
      for (int i = 0; i < 12; i++) {
        builder.append(chars.charAt((randomBytes[i] & 0xFF) % chars.length()));
      }
      deviceId = builder.toString();
      preferences.edit().putString("device_id", deviceId).apply();
    }
    return deviceId;
  }

  /**
   * Returns the base URI of the server.
   */
  public static URI getServerUri(Context context, String path) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    String base = preferences.getString("server_url", "https://gcmfilepush.appspot.com/");
    try {
      return new URI(base).resolve(path);
    } catch (URISyntaxException e) {
      Log.e(LOG_TAG, "invalid URI");
      return null;
    }
  }
}
