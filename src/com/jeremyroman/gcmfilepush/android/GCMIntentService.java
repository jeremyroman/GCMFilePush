package com.jeremyroman.gcmfilepush.android;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService {
  private static final String LOG_TAG = GCMIntentService.class.getSimpleName();
  private static final String EXTRA_URL = "url";
  private static final String EXTRA_FILENAME = "filename";
  private static final String EXTRA_TITLE = "title";

  @Override
  protected void onRegistered(Context context, String registrationId) {
    URI uri = getRegistrationUri(context);
    if (uri != null) {
      HttpPut put = new HttpPut(uri);
      try {
        put.setEntity(new StringEntity(registrationId));
      } catch (UnsupportedEncodingException e) {
        Log.e(LOG_TAG, "unsupported registration ID encoding", e);
      }
      ClientUtils.executeHttpRequest(put);
    }
  }

  @Override
  protected void onUnregistered(Context context, String registrationId) {
    URI uri = getRegistrationUri(context);
    if (uri != null) {
      ClientUtils.executeHttpRequest(new HttpDelete(uri));
    }
  }

  @Override
  protected void onMessage(Context context, Intent intent) {
    // Check that the extras are present.
    String urlString = intent.getStringExtra(EXTRA_URL);
    String filename = intent.getStringExtra(EXTRA_FILENAME);
    String title = intent.getStringExtra(EXTRA_TITLE);
    if (urlString == null || filename == null) {
      return;
    }

    // Check that the URI is for HTTP or HTTPS.
    Uri uri = Uri.parse(urlString);
    if (uri == null ||
        (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme()))) {
      return;
    }

    // Construct a download manager request.
    DownloadManager.Request request = new DownloadManager.Request(uri);
    // TODO: make these configurable
    request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
    request.setNotificationVisibility(
        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
    request.allowScanningByMediaScanner();
    if (title != null) {
      request.setTitle(title);
    }

    // Choose the destination URI.
    File destination = new File(Environment.getExternalStorageDirectory(), filename);
    if (destination.exists() || destination.getParent() == null) {
      Log.e(LOG_TAG, "destination file invalid");
      return;
    }
    destination.getParentFile().mkdirs();
    request.setDestinationUri(Uri.fromFile(destination));

    // Pass the request to the download manager.
    DownloadManager downloadManager = (DownloadManager)
        context.getSystemService(DOWNLOAD_SERVICE);
    downloadManager.enqueue(request);
  }

  @Override
  protected void onError(Context context, String error) {
    Log.e(LOG_TAG, error);
  }

  private URI getRegistrationUri(Context context) {
    String path = "registration/" + ClientUtils.getDeviceId(context);
    return ClientUtils.getServerUri(context, path);
  }
}
