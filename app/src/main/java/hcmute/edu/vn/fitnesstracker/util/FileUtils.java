package hcmute.edu.vn.fitnesstracker.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

public class FileUtils {

    /**
     * Lấy InputStream từ Uri để sử dụng trực tiếp (ví dụ: upload lên Cloudinary).
     *
     * @param context Context của ứng dụng
     * @param uri     Uri của tệp
     * @return InputStream nếu thành công, null nếu thất bại
     */
    public static InputStream getInputStream(Context context, Uri uri) {
        try {
            return context.getContentResolver().openInputStream(uri);
        } catch (Exception e) {
            Log.e("FileUtils", "Failed to open InputStream for Uri: " + uri, e);
            return null;
        }
    }

    /**
     * Lấy kích thước tệp (MB) từ Uri.
     *
     * @param context Context của ứng dụng
     * @param uri     Uri của tệp
     * @return Kích thước tệp (MB), hoặc -1 nếu thất bại
     */
    public static long getFileSize(Context context, Uri uri) {
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                long size = inputStream.available() / (1024 * 1024); // Chuyển sang MB
                return size;
            }
        } catch (IOException e) {
            Log.e("FileUtils", "Failed to get file size for Uri: " + uri, e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e("FileUtils", "Failed to close InputStream", e);
                }
            }
        }
        return -1;
    }

    /**
     * Lấy tên tệp từ Uri.
     *
     * @param context Context của ứng dụng
     * @param uri     Uri của tệp
     * @return Tên tệp nếu thành công, hoặc chuỗi mặc định nếu thất bại
     */
    public static String getFileName(Context context, Uri uri) {
        String fileName = null;

        // Xử lý Uri scheme "content"
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
                    fileName = cursor.getString(nameIndex);
                }
            } catch (Exception e) {
                Log.e("FileUtils", "Failed to get file name from content Uri: " + uri, e);
            }
        }

        // Xử lý Uri scheme "file" hoặc fallback
        if (fileName == null) {
            fileName = uri.getLastPathSegment();
            if (fileName == null) {
                fileName = "unknown_file_" + System.currentTimeMillis();
            }
        }

        return fileName;
    }
}