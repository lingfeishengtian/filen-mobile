package io.filen.app;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.json.*;

class APIRequest {
    public interface APICallback {
        void onSuccess(JSONObject result);
        void onError(Throwable throwable);
    }

    public interface UploadCallback {
        void onSuccess(JSONObject result);
        void onError(Throwable throwable);
    }

    private static OkHttpClient client;

    public APIRequest () {
        client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    final Request request = chain.request();
                    Response response = null;
                    int tryCount = 0;
                    int maxLimit = 32;

                    while (tryCount < maxLimit) {
                        try {
                            response = chain.proceed(request);
                            if (response.isSuccessful()) {
                                return response;
                            }
                        } catch (IOException e) {
                            tryCount++;
                        }
                    }

                    assert response != null;

                    return response;
                })
                .connectTimeout(180, TimeUnit.SECONDS)
                .readTimeout(900, TimeUnit.SECONDS)
                .writeTimeout(900, TimeUnit.SECONDS)
                .build();
    }

    public void request (Request request, final APICallback callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure (@NonNull Call call, @NonNull IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse (@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    try {
                        assert response.body() != null;

                        final String responseData = response.body().string();

                        final JSONObject jsonObject = new JSONObject(responseData);

                        callback.onSuccess(jsonObject);
                    } catch (JSONException | IOException e) {
                        callback.onError(e);
                    }
                } else {
                    callback.onError(new IOException("Unexpected code " + response));
                }
            }
        });
    }

    public void upload (Request request, final UploadCallback callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure (@NonNull Call call, @NonNull IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse (@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    try {
                        assert response.body() != null;

                        final String responseData = response.body().string();

                        final JSONObject jsonObject = new JSONObject(responseData);

                        callback.onSuccess(jsonObject);
                    } catch (JSONException | IOException e) {
                        callback.onError(e);
                    }
                } else {
                    callback.onError(new IOException("Unexpected code " + response));
                }
            }
        });
    }
}

public class FilenAPI {
    private static final APIRequest apiRequest = new APIRequest();
    private static final String API_URL = "https://gateway.filen.io";
    private static final String INGEST_URL = "https://ingest.filen.io";
    private static final String EGEST_URL = "https://egest.filen.io";

    private static Request buildPostRequest (String apiKey, String endpoint, String json) throws Exception {
        final RequestBody body = RequestBody.create(json, null);

        return new Request.Builder()
                .url(API_URL + endpoint)
                .post(body)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("Checksum", FilenCrypto.hash(json, "SHA-512"))
                .build();
    }

    public static void fetchFolderContent (String apiKey, String uuid, APIRequest.APICallback callback) {
        try {
            // We unfortunately have to manually serialize the JSON for the checksum validation to work. org.json.toString() does not work here.
            final String json = "{\"uuid\":\"" + uuid + "\"}";

            final Request request = buildPostRequest(apiKey, "/v3/dir/content", json);

            Log.d("FilenDocumentsProvider", "fetchFolderContent: " + request);

            apiRequest.request(request, callback);
        } catch (Exception e) {
            callback.onError(e);

            Log.d("FilenDocumentsProvider", "fetchFolderContent error: " + e.getMessage());

            e.printStackTrace();
        }
    }

    public static void trashItem (String apiKey, String uuid, String type, APIRequest.APICallback callback) {
        try {
            // We unfortunately have to manually serialize the JSON for the checksum validation to work. org.json.toString() does not work here.
            final String json = "{\"uuid\":\"" + uuid + "\"}";

            final Request request = buildPostRequest(apiKey, type.equals("folder") ? "/v3/dir/trash" : "/v3/file/trash", json);

            Log.d("FilenDocumentsProvider", "trashItem: " + request);

            apiRequest.request(request, callback);
        } catch (Exception e) {
            callback.onError(e);

            Log.d("FilenDocumentsProvider", "trashItem error: " + e.getMessage());

            e.printStackTrace();
        }
    }

    public static void markUploadAsDone (String apiKey, String uuid, String nameEncrypted, String nameHashed, String sizeEncrypted, long chunks, String mimeEncrypted, String rm, String encryptedMetadata, int version, String uploadKey, APIRequest.APICallback callback) {
        try {
            // We unfortunately have to manually serialize the JSON for the checksum validation to work. org.json.toString() does not work here.
            final String json = "{\"uuid\":\"" + uuid + "\",\"name\":\"" + nameEncrypted + "\",\"nameHashed\":\"" + nameHashed + "\",\"size\":\"" + sizeEncrypted + "\",\"chunks\":" + chunks + ",\"mime\":\"" + mimeEncrypted + "\",\"rm\":\"" + rm + "\",\"metadata\":\"" + encryptedMetadata + "\",\"version\":" + version + ",\"uploadKey\":\"" + uploadKey + "\"}";

            final Request request = buildPostRequest(apiKey, "/v3/upload/done", json);

            Log.d("FilenDocumentsProvider", "markUploadAsDone: " + request);

            apiRequest.request(request, callback);
        } catch (Exception e) {
            callback.onError(e);

            Log.d("FilenDocumentsProvider", "markUploadAsDone error: " + e.getMessage());

            e.printStackTrace();
        }
    }

    public static void createFolder (String apiKey, String uuid, String nameEncrypted, String nameHashed, String parent, APIRequest.APICallback callback) {
        try {
            // We unfortunately have to manually serialize the JSON for the checksum validation to work. org.json.toString() does not work here.
            final String json = "{\"uuid\":\"" + uuid + "\",\"name\":\"" + nameEncrypted + "\",\"nameHashed\":\"" + nameHashed + "\",\"parent\":\"" + parent + "\"}";

            final Request request = buildPostRequest(apiKey, "/v3/dir/create", json);

            Log.d("FilenDocumentsProvider", "createFolder: " + request);

            apiRequest.request(request, callback);
        } catch (Exception e) {
            callback.onError(e);

            Log.d("FilenDocumentsProvider", "createFolder error: " + e.getMessage());

            e.printStackTrace();
        }
    }

    public static void uploadFileChunk (String apiKey, File inputFile, String uuid, int index, String uploadKey, String parent, String inputFileChecksum, APIRequest.UploadCallback callback) {
        try {
            final String url = INGEST_URL + "/v3/upload?uuid=" + uuid + "&index=" + index + "&uploadKey=" + uploadKey + "&parent=" + parent + "&hash=" + inputFileChecksum;
            final RequestBody requestBody = RequestBody.create(inputFile, MediaType.parse("application/octet-stream"));

            // We unfortunately have to manually serialize the JSON for the checksum validation to work. org.json.toString() does not work here.
            final String queryItemsJSON = "{\"uuid\":\"" + uuid + "\",\"index\":\"" + index + "\",\"uploadKey\":\"" + uploadKey + "\",\"parent\":\"" + parent + "\",\"hash\":\"" + inputFileChecksum + "\"}";
            final String queryItemsJSONChecksum = FilenCrypto.hash(queryItemsJSON, "SHA-512");

            final Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Accept", "application/json")
                    .addHeader("Checksum", queryItemsJSONChecksum)
                    .post(requestBody)
                    .build();

            apiRequest.upload(request, callback);
        }  catch (Exception e) {
            callback.onError(e);

            Log.d("FilenDocumentsProvider", "uploadFileChunk error: " + e.getMessage());

            e.printStackTrace();
        }
    }

    public static File downloadFileChunk (Context context, Item item, int index) throws Exception {
        Log.d("FilenDocumentsProvider", "downloadChunk: " + item + ", " + index);

        final File outputFileDir = new File(context.getFilesDir(), "documentsProvider/downloadedChunks/" + item.uuid);

        if (!outputFileDir.exists()) {
            if (!outputFileDir.mkdirs()) {
                throw new Exception("Could not create parent dirs.");
            }
        }

        final File outputFile = new File(outputFileDir, UUID.randomUUID().toString() + "." + index);
        final URL url = new URL(EGEST_URL + "/" + item.region + "/" + item.bucket + "/" + item.uuid + "/" + index);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setReadTimeout(900000);
        connection.setConnectTimeout(60000);
        connection.setRequestMethod("GET");

        try {
            final int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream in = new BufferedInputStream(connection.getInputStream()); FileOutputStream out = new FileOutputStream(outputFile)) {
                    byte[] data = new byte[1024];
                    int count;

                    while ((count = in.read(data)) != -1) {
                        out.write(data, 0, count);
                    }
                }

                return outputFile;
            } else {
                throw new IOException("Egest returned HTTP response code: " + responseCode + " for URL: " + url);
            }
        } finally {
            connection.disconnect();
        }
    }

    public static void renameFolder (String apiKey, String uuid, String nameEncrypted, String nameHashed, APIRequest.APICallback callback) {
        try {
            // We unfortunately have to manually serialize the JSON for the checksum validation to work. org.json.toString() does not work here.
            final String json = "{\"uuid\":\"" + uuid + "\",\"name\":\"" + nameEncrypted + "\",\"nameHashed\":\"" + nameHashed + "\"}";

            final Request request = buildPostRequest(apiKey, "/v3/dir/rename", json);

            Log.d("FilenDocumentsProvider", "renameFolder: " + request);

            apiRequest.request(request, callback);
        } catch (Exception e) {
            callback.onError(e);

            Log.d("FilenDocumentsProvider", "renameFolder error: " + e.getMessage());

            e.printStackTrace();
        }
    }

    public static void renameFile (String apiKey, String uuid, String nameEncrypted, String nameHashed, String encryptedMetadata, APIRequest.APICallback callback) {
        try {
            // We unfortunately have to manually serialize the JSON for the checksum validation to work. org.json.toString() does not work here.
            final String json = "{\"uuid\":\"" + uuid + "\",\"name\":\"" + nameEncrypted + "\",\"nameHashed\":\"" + nameHashed + "\",\"metadata\":\"" + encryptedMetadata + "\"}";

            final Request request = buildPostRequest(apiKey, "/v3/file/rename", json);

            Log.d("FilenDocumentsProvider", "renameFile: " + request);

            apiRequest.request(request, callback);
        } catch (Exception e) {
            callback.onError(e);

            Log.d("FilenDocumentsProvider", "renameFile error: " + e.getMessage());

            e.printStackTrace();
        }
    }

    public static void moveItem (String apiKey, String type, String uuid, String parent, APIRequest.APICallback callback) {
        try {
            // We unfortunately have to manually serialize the JSON for the checksum validation to work. org.json.toString() does not work here.
            final String json = "{\"uuid\":\"" + uuid + "\",\"to\":\"" + parent + "\"}";

            final Request request = buildPostRequest(apiKey, "/v3/" + (type.equals("file") ? "file" : "folder") + "/move", json);

            Log.d("FilenDocumentsProvider", "moveItem: " + request);

            apiRequest.request(request, callback);
        } catch (Exception e) {
            callback.onError(e);

            Log.d("FilenDocumentsProvider", "moveItem error: " + e.getMessage());

            e.printStackTrace();
        }
    }
}
