package com.mottainai.cliente.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.mottainai.cliente.BuildConfig;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class CloudinaryProfilePhoto {
    public interface Result {
        void onSuccess(String secureUrl);
        void onFailure();
    }

    private static final String PREFERENCES = "cloudinary_profile_photo";
    private static final long MAX_FILE_BYTES = 5L * 1024L * 1024L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build();

    private CloudinaryProfilePhoto() {}

    public static boolean isConfigured() {
        return !BuildConfig.CLOUDINARY_CLOUD_NAME.trim().isEmpty()
                && !BuildConfig.CLOUDINARY_UPLOAD_PRESET.trim().isEmpty();
    }

    public static void upload(Context context, File file, Result result) {
        if (!isConfigured() || file == null || !file.isFile() || file.length() > MAX_FILE_BYTES) {
            failure(result);
            return;
        }
        RequestBody image = RequestBody.create(file, MediaType.get("image/jpeg"));
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", BuildConfig.CLOUDINARY_UPLOAD_PRESET)
                .addFormDataPart("file", "profile.jpg", image)
                .build();
        Request request = new Request.Builder()
                .url("https://api.cloudinary.com/v1_1/" + BuildConfig.CLOUDINARY_CLOUD_NAME + "/image/upload")
                .post(body)
                .build();
        HTTP.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException error) { failure(result); }

            @Override public void onResponse(Call call, Response response) throws IOException {
                try (Response ignored = response) {
                    if (!response.isSuccessful() || response.body() == null) { failure(result); return; }
                    String url = new JSONObject(response.body().string()).optString("secure_url");
                    if (url == null || !url.startsWith("https://")) { failure(result); return; }
                    MAIN.post(() -> result.onSuccess(url));
                } catch (Exception invalidResponse) { failure(result); }
            }
        });
    }

    public static String url(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getString("url_" + profileKey(context), "");
    }

    public static File photoFile(Context context) {
        return localFile(context, false);
    }

    public static File pendingFile(Context context) {
        return localFile(context, true);
    }

    public static void saveUrl(Context context, String url) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
                .putString("url_" + profileKey(context), url).apply();
    }

    private static File localFile(Context context, boolean pending) {
        return new File(context.getFilesDir(), "profile-photo-" + profileKey(context)
                + (pending ? "-pending" : "") + ".jpg");
    }

    private static String profileKey(Context context) {
        com.google.firebase.auth.FirebaseAuth auth = FirebaseSupport.authIfConfigured(context);
        if (auth == null) return "anonymous";
        return auth.getCurrentUser() == null ? "anonymous" : auth.getCurrentUser().getUid();
    }

    private static void failure(Result result) { MAIN.post(result::onFailure); }
}
