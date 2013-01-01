package com.jeremyroman.gcmfilepush.android;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gcm.GCMRegistrar;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {
  private static final String LOG_TAG = MainActivity.class.getSimpleName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_activity);
    TextView deviceIdView = (TextView) findViewById(R.id.device_id);
    deviceIdView.setText(ClientUtils.getDeviceId(this));

    GCMRegistrar.checkDevice(this);
    GCMRegistrar.checkManifest(this);
    if (!GCMRegistrar.isRegistered(this)) {
      new RegistrationTask().execute();
    }
  }

  private class RegistrationTask extends AsyncTask<Void, Void, Void> {
    private String senderId;

    @Override
    protected Void doInBackground(Void... params) {
      JSONObject config = ClientUtils.getClientConfiguration(MainActivity.this);
      if (config == null) {
        cancel(false /* mayInterruptIfRunning */);
        return null;
      }

      try {
        senderId = config.getString("sender_id");
      } catch (JSONException e) {
        Log.e(LOG_TAG, "configuration missing sender_id");
        cancel(false /* mayInterruptIfRunning */);
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      GCMRegistrar.register(MainActivity.this, senderId);
    }
  }
}
