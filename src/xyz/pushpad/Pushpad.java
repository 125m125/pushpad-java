package xyz.pushpad;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Pushpad {
  public String authToken;
  public String projectId;

  public Pushpad(String authToken, String projectId) {
    this.authToken = authToken;
    this.projectId = projectId;
  }

  public String signatureFor(String data) {
    SecretKeySpec signingKey = new SecretKeySpec(this.authToken.getBytes(), "HmacSHA1");
    String encoded = null;
    try {
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(signingKey);
      byte[] rawHmac = mac.doFinal(data.getBytes());
      encoded = Base64.getEncoder().withoutPadding().encodeToString(rawHmac);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) { 
      e.printStackTrace();
    }

    return encoded;
  }

  public String path() {
    return "https://pushpad.xyz/projects/" + this.projectId + "/subscription/edit";
  }

  public String pathFor(String uid) {
    String uidSignature = this.signatureFor(uid);
    return this.path() + "?uid=" + uid + "&uid_signature=" + uidSignature;
  }

  public Notification buildNotification(String title, String body, String targetUrl) {
    return new Notification(this, title, body, targetUrl);
  }

  public boolean isSubscribed(final String uid) {
    final String endpoint = "https://pushpad.xyz/projects/" + this.projectId + "/subscriptions?uids[]=" + uid;
    HttpsURLConnection connection = null;
    int code;
    String responseBody;
    JSONArray json;

    try {
      // Create connection
      final URL url = new URL(endpoint);
      connection = (HttpsURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.setRequestProperty("Authorization", "Token token=\"" + this.authToken + "\"");
      connection.setRequestProperty("Accept", "application/json");
      connection.setUseCaches(true);
      connection.setDoOutput(false);

      // Get Response
      final InputStream is = connection.getInputStream();
      final BufferedReader rd = new BufferedReader(new InputStreamReader(is));
      final StringBuilder response = new StringBuilder();
      String line;
      while ((line = rd.readLine()) != null) {
        response.append(line);
        response.append('\r');
      }
      rd.close();
      code = connection.getResponseCode();
      responseBody = response.toString();
    } catch (final IOException e) {
      return false;
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }

    if (code != 200) {
      return false;
    }

    try {
      final JSONParser parser = new JSONParser();
      json = (JSONArray) parser.parse(responseBody);
    } catch (final ParseException e) {
      return false;
    }
    return !json.isEmpty();
  }
}
