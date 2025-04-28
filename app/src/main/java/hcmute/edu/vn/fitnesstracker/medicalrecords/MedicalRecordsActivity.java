package hcmute.edu.vn.fitnesstracker.medicalrecords;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import hcmute.edu.vn.fitnesstracker.DataBase;
import hcmute.edu.vn.fitnesstracker.HomeActivity;
import hcmute.edu.vn.fitnesstracker.LoginActivity;
import hcmute.edu.vn.fitnesstracker.R;
import hcmute.edu.vn.fitnesstracker.model.Record;
import hcmute.edu.vn.fitnesstracker.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MedicalRecordsActivity extends AppCompatActivity {
    private static final int STORAGE_PERMISSION_CODE = 100;
    private RecyclerView recyclerView;
    private RecordAdapter adapter;
    private ArrayList<Record> records;
    private Spinner spinnerRecordType;
    private ProgressBar progressBar;
    private Cloudinary cloudinary;
    private DataBase db;

    // Activity Result Launcher cho việc chọn tệp
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) {
                        uploadToCloudinary(fileUri);
                    } else {
                        Toast.makeText(this, R.string.error_file_path, Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_records);

        // Khởi tạo Cloudinary từ BuildConfig
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dh0jqp0gf",
                "api_key", "331468291844525",
                "api_secret", "csogYjTYqLZuH4oThYH8is9Tes8"
        ));

        // Khởi tạo DataBase
        db = new DataBase(this);

        // Khởi tạo Spinner
        spinnerRecordType = findViewById(R.id.spinner_record_type);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.record_types, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecordType.setAdapter(spinnerAdapter);

        // Khởi tạo RecyclerView
        recyclerView = findViewById(R.id.recycler_view_records);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        records = new ArrayList<>();
        adapter = new RecordAdapter(records);
        recyclerView.setAdapter(adapter);

        // Khởi tạo ProgressBar
        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);

        // Nút tải lên
        Button btnUpload = findViewById(R.id.btn_upload);
        btnUpload.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                openFilePicker();
            } else {
                requestStoragePermission();
            }
        });

        // Nút quay lại
        findViewById(R.id.btn_back).setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));

        // Tải danh sách bản ghi
        loadRecords();
    }

    private boolean checkStoragePermission() {
        // ACTION_OPEN_DOCUMENT không yêu cầu quyền lưu trữ, nhưng giữ logic để tương thích
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, R.string.storage_permission_denied_permanently, Toast.LENGTH_LONG).show();
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.storage_permission_title)
                .setMessage(R.string.storage_permission_message)
                .setPositiveButton(R.string.agree, (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ActivityCompat.requestPermissions(this, new String[]{
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.READ_MEDIA_AUDIO
                        }, STORAGE_PERMISSION_CODE);
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        }, STORAGE_PERMISSION_CODE);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE && grantResults.length > 0) {
            boolean readGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            boolean writeGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || grantResults.length > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED;
            boolean mediaGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || (grantResults.length > 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED);

            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && mediaGranted) ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && readGranted) ||
                    (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && readGranted && writeGranted)) {
                openFilePicker();
            } else {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, R.string.storage_permission_denied_permanently, Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } else {
                    Toast.makeText(this, R.string.storage_permission_denied, Toast.LENGTH_SHORT).show();
                }
            }
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

    private void uploadToCloudinary(Uri fileUri) {
        // Kiểm tra đăng nhập
        SharedPreferences prefs = getSharedPreferences("shared_prefs", MODE_PRIVATE);
        String username = prefs.getString("username", "");
        if (username.isEmpty()) {
            Toast.makeText(this, R.string.please_login, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class)); // Thay bằng activity đăng nhập
            finish();
            return;
        }

        // Kiểm tra kết nối mạng
        if (!isNetworkAvailable()) {
            Toast.makeText(this, R.string.no_network, Toast.LENGTH_SHORT).show();
            Log.e("MedicalRecordsActivity", "No network connection");
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

        String recordType = spinnerRecordType.getSelectedItem().toString().toLowerCase();
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
                        Toast.makeText(MedicalRecordsActivity.this, R.string.error_file_access, Toast.LENGTH_SHORT).show();
                    });
                    Log.e("MedicalRecordsActivity", "Failed to open InputStream for Uri: " + fileUri);
                    return;
                }
                Log.d("MedicalRecordsActivity", "Uploading with preset: medical_records");
                Map result = cloudinary.uploader().upload(
                        inputStream,
                        ObjectUtils.asMap(
                                "resource_type", "auto",
                                "folder", "medical_records/" + username,
                                "access_mode", "authenticated"
                        )
                );
                handler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    String publicId = (String) result.get("public_id");
                    String secureUrl = (String) result.get("secure_url");
                    String uploadDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                    saveToDatabase(publicId, secureUrl, uploadDate, recordType);
                    Toast.makeText(MedicalRecordsActivity.this, R.string.upload_success, Toast.LENGTH_SHORT).show();
                    loadRecords();
                });
            } catch (Exception e) { // Bắt mọi ngoại lệ
                handler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MedicalRecordsActivity.this, R.string.upload_failed, Toast.LENGTH_SHORT).show();
                });
                Log.e("MedicalRecordsActivity", "Upload failed: " + e.getMessage(), e);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Log.e("MedicalRecordsActivity", "Failed to close InputStream", e);
                    }
                }
            }
        });
    }

    private void saveToDatabase(String publicId, String secureUrl, String uploadDate, String recordType) {
        SharedPreferences prefs = getSharedPreferences("shared_prefs", MODE_PRIVATE);
        String username = prefs.getString("username", "");
        try {
            db.addMedicalRecord(username, recordType, publicId, secureUrl, uploadDate);
        } catch (Exception e) {
            Toast.makeText(this, R.string.database_error, Toast.LENGTH_SHORT).show();
            Log.e("MedicalRecordsActivity", "Database save failed", e);
        }
    }

    private void loadRecords() {
        SharedPreferences prefs = getSharedPreferences("shared_prefs", MODE_PRIVATE);
        String username = prefs.getString("username", "");
        try {
            ArrayList<String> recordData = db.getMedicalRecords(username);
            records.clear();
            for (String data : recordData) {
                String[] split = data.split("\\$");
                if (split.length == 3) {
                    records.add(new Record(split[0], split[1], split[2]));
                } else {
                    Log.e("MedicalRecordsActivity", "Invalid record data: " + data);
                }
            }
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            Toast.makeText(this, R.string.database_error, Toast.LENGTH_SHORT).show();
            Log.e("MedicalRecordsActivity", "Database load failed", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Đóng cơ sở dữ liệu nếu cần
        if (db != null) {
            db.close();
        }
    }
}