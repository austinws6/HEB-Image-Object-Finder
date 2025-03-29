package org.example.hebimageobjectfinder;

import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import org.json.*;


public class ImaggaObjectDetection {
    static String credentialsToEncode = "acc_0ae489ea7473a1f" + ":" + "d61e5e52a86a639bf2763f4fd7267434";
    static String basicAuth = Base64.getEncoder().encodeToString(credentialsToEncode.getBytes(StandardCharsets.UTF_8));
    static String baseUrl = "https://api.imagga.com/v2";

    public static String ImageUpload(String fullPathToImage) throws IOException {
        File fileToUpload = new File(fullPathToImage);
        String endpoint = baseUrl + "/uploads";

        String crlf = "\r\n";
        String twoHyphens = "--";
        String boundary =  "Image Upload";

        URL urlObject = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
        connection.setRequestProperty("Authorization", "Basic " + basicAuth);
        connection.setUseCaches(false);
        connection.setDoOutput(true);

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

        DataOutputStream request = new DataOutputStream(connection.getOutputStream());

        request.writeBytes(twoHyphens + boundary + crlf);
        request.writeBytes("Content-Disposition: form-data; name=\"image\";filename=\"" + fileToUpload.getName() + "\"" + crlf);
        request.writeBytes(crlf);

        InputStream inputStream = new FileInputStream(fileToUpload);
        int bytesRead;
        byte[] dataBuffer = new byte[1024];
        while ((bytesRead = inputStream.read(dataBuffer)) != -1) {
            request.write(dataBuffer, 0, bytesRead);
        }

        request.writeBytes(crlf);
        request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);
        request.flush();
        request.close();

        InputStream responseStream = new BufferedInputStream(connection.getInputStream());

        BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(responseStream));

        String line = "";
        StringBuilder stringBuilder = new StringBuilder();

        while ((line = responseStreamReader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        responseStreamReader.close();

        String response = stringBuilder.toString();
        System.out.println("------> RESPONSE from Imagga ImageUpload call = " + response);

        // Get just the upload_id we need from response
        JSONObject jsonObject = new JSONObject(response);
        String upload_id = jsonObject.getJSONObject("result").getString("upload_id");

        responseStream.close();
        connection.disconnect();

        return upload_id;
    }

    public static ArrayList<String> getObjectNamesFromUploadID(String upload_id) throws IOException {
        String endpoint = baseUrl + "/tags?image_upload_id=" + upload_id;
        URL urlObject = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();

        connection.setRequestProperty("Authorization", "Basic " + basicAuth);

        int responseCode = connection.getResponseCode();

        System.out.println("------> Sending 'GET' request to URL : " + endpoint);
        System.out.println("------> Response Code : " + responseCode);

        BufferedReader connectionInput = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String jsonResponse = connectionInput.readLine();
        connectionInput.close();

        return createListOfObjectNames(jsonResponse);
    }

    public static ArrayList<String> getObjectNamesFromURL(String urlString) throws IOException {
        String endpoint = baseUrl + "/tags?image_url=" + urlString;

        URL urlObject = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();

        connection.setRequestProperty("Authorization", "Basic " + basicAuth);

        int responseCode = connection.getResponseCode();

        System.out.println("------> Sending 'GET' request to URL : " + endpoint);
        System.out.println("------> Response Code : " + responseCode);

        BufferedReader connectionInput = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        String jsonResponse = connectionInput.readLine();
        connectionInput.close();

        return createListOfObjectNames(jsonResponse);
    }

    public static ArrayList<String> createListOfObjectNames(String jsonResponse) throws IOException {
        // Create list of object names detected in image from jsonResponse
        JSONObject responseObject = new JSONObject(jsonResponse);

        JSONArray objectsFound = responseObject.getJSONObject("result").getJSONArray("tags");
        System.out.println("Number of objects found in image = " + objectsFound.length());

        ArrayList<String> objectNames = new ArrayList<String>();
        for (int i = 0; i < objectsFound.length(); i++) {
            objectNames.add(objectsFound.getJSONObject(i).getJSONObject("tag").getString("en"));
        }

        return objectNames;
    }
}