package hcmute.edu.vn.fitnesstracker.eventmedia;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import hcmute.edu.vn.fitnesstracker.DataBase;
import hcmute.edu.vn.fitnesstracker.HomeActivity;
import hcmute.edu.vn.fitnesstracker.LoginActivity;
import hcmute.edu.vn.fitnesstracker.R;
import hcmute.edu.vn.fitnesstracker.model.Media;
import hcmute.edu.vn.fitnesstracker.util.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventMediaActivity extends AppCompatActivity {
    private static final String TAG = "EventMediaActivity";
    private RecyclerView recyclerView;
    private MediaAdapter adapter;
    private ArrayList<Media> mediaList;
    private EditText editEventId;
    private ProgressBar progressBar;
    private Cloudinary cloudinary;
    private DataBase db;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) {
                        String eventId = editEventId.getText().toString().trim();
                        if (eventId.isEmpty()) {
                            Toast.makeText(this, R.string.enter_event_id, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        uploadToCloudinary(fileUri, eventId);
                    } else {
                        Toast.makeText(this, R.string.error_file_path, Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_media);

        // Khởi tạo Cloudinary từ BuildConfig
        try {
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", "dh0jqp0gf",
                    "api_key", "331468291844525",
                    "api_secret", "csogYjTYqLZuH4oThYH8is9Tes8"
            ));
            Log.d(TAG, "Cloudinary initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Cloudinary: " + e.getMessage(), e);
            Toast.makeText(this, R.string.cloudinary_init_failed, Toast.LENGTH_LONG).show();
            finish();
        }

        // Khởi tạo DataBase
        db = new DataBase(this);

        // Khởi tạo UI
        editEventId = findViewById(R.id.edit_event_id);
        recyclerView = findViewById(R.id.recycler_view_media);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mediaList = new ArrayList<>();
        adapter = new MediaAdapter(mediaList);
        recyclerView.setAdapter(adapter);

        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);

        Button btnUpload = findViewById(R.id.btn_upload);
        btnUpload.setOnClickListener(v -> {
            String eventId = editEventId.getText().toString().trim();
            if (eventId.isEmpty()) {
                Toast.makeText(this, R.string.enter_event_id, Toast.LENGTH_SHORT).show();
                return;
            }
            openFilePicker();
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));

        // Tải media khởi tạo nếu cần
        String eventId = editEventId.getText().toString().trim();
        if (!eventId.isEmpty()) {
            loadMedia(eventId);
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        filePickerLauncher.launch(intent);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void uploadToCloudinary(Uri fileUri, String eventId) {
        // Kiểm tra đăng nhập
        SharedPreferences prefs = getSharedPreferences("shared_prefs", MODE_PRIVATE);
        String username = prefs.getString("username", "");
        if (username.isEmpty()) {
            Toast.makeText(this, R.string.please_login, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Kiểm tra kết nối mạng
        if (!isNetworkAvailable()) {
            Toast.makeText(this, R.string.no_network, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No network connection");
            return;
        }

        // Kiểm tra kích thước tệp
        long fileSizeInMB = FileUtils.getFileSize(this, fileUri);
        if (fileSizeInMB > 100) {
            Toast.makeText(this, R.string.file_too_large, Toast.LENGTH_SHORT).show();
            return;
        } else if (fileSizeInMB == -1) {
            Toast.makeText(this, R.string.error_file_access, Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            InputStream inputStream = null;
            try {
                inputStream = FileUtils.getInputStream(this, fileUri);
                if (inputStream == null) {
                    handler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(EventMediaActivity.this, R.string.error_file_access, Toast.LENGTH_SHORT).show();
                    });
                    Log.e(TAG, "Failed to open InputStream for Uri: " + fileUri);
                    return;
                }
                Log.d(TAG, "Uploading with preset: event_media, eventId: " + eventId + ", folder: event_media/" + eventId);
                Map result = cloudinary.uploader().upload(
                        inputStream,
                        ObjectUtils.asMap(
                                "resource_type", "auto",
                                "folder", "event_media/" + eventId,
                                "public_id", "event_" + System.currentTimeMillis()
                        )
                );
                handler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    String publicId = (String) result.get("public_id");
                    String secureUrl = (String) result.get("secure_url");
                    String uploadDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                    String mediaType = secureUrl.contains(".mp4") ? "video" : "image";
                    saveToDatabase(eventId, publicId, secureUrl, uploadDate, mediaType);
                    Toast.makeText(EventMediaActivity.this, R.string.upload_success, Toast.LENGTH_SHORT).show();
                    shareMedia(secureUrl);
                    loadMedia(eventId);
                });
            } catch (Exception e) { // Bắt tất cả ngoại lệ
                handler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(EventMediaActivity.this, R.string.upload_failed, Toast.LENGTH_SHORT).show();
                });
                Log.e(TAG, "Upload failed: " + e.getMessage(), e);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to close InputStream", e);
                    }
                }
            }
        });
    }

    private void saveToDatabase(String eventId, String publicId, String secureUrl, String uploadDate, String mediaType) {
        try {
            db.addEventMedia(eventId, mediaType, publicId, secureUrl, uploadDate);
        } catch (Exception e) {
            Toast.makeText(this, R.string.database_error, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Database save failed", e);
        }
    }

    private void loadMedia(String eventId) {
        try {
            ArrayList<String> mediaData = db.getEventMedia(eventId);
            mediaList.clear();
            for (String data : mediaData) {
                String[] split = data.split("\\$");
                if (split.length == 3) {
                    mediaList.add(new Media(split[0], split[1], split[2]));
                } else {
                    Log.e(TAG, "Invalid media data: " + data);
                }
            }
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            Toast.makeText(this, R.string.database_error, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Database load failed", e);
        }
    }

    private void shareMedia(String url) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_media_text, url));
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_media_title)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
        }
    }
}