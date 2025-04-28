package hcmute.edu.vn.fitnesstracker.eventmedia;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import hcmute.edu.vn.fitnesstracker.R;
import hcmute.edu.vn.fitnesstracker.model.Media;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.ViewHolder> {
    private ArrayList<Media> mediaList;

    public MediaAdapter(ArrayList<Media> mediaList) {
        this.mediaList = mediaList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_record, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Media media = mediaList.get(position);
        holder.typeText.setText(media.getType());
        holder.dateText.setText(media.getDate());
        if (media.getType().equals("image")) {
            new LoadImageTask(holder.imageView).execute(media.getUrl());
        } else {
            holder.imageView.setImageResource(android.R.drawable.ic_media_play); // Placeholder cho video
        }
    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView typeText, dateText;
        ImageView imageView;

        ViewHolder(View itemView) {
            super(itemView);
            typeText = itemView.findViewById(R.id.text_record_type);
            dateText = itemView.findViewById(R.id.text_upload_date);
            imageView = itemView.findViewById(R.id.image_record);
        }
    }

    private static class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        private ImageView imageView;

        LoadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            try {
                URL imageUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
}