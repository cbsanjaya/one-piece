package com.cbsanjaya.onepiece;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.cbsanjaya.onepiece.utils.NetUtils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ListImageActivity extends AppCompatActivity {

    public static String EXTRA_TITLE = "EXTRA_TITLE";
    public static String EXTRA_URL = "EXTRA_URL";
    RecyclerView mRecycler;
    CustomAdapter mAdapter;
    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_image);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        url = getIntent().getStringExtra(EXTRA_URL);

        String titleEpisode = title.substring(0, title.indexOf(" :"));

        setTitle(titleEpisode);
        mRecycler = (RecyclerView) findViewById(R.id.rvImage);
        mRecycler.setHasFixedSize(true);

        mAdapter = new CustomAdapter(this);
        mRecycler.setAdapter(mAdapter);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));

        new DownloadTask(mAdapter, url).execute();
    }

    final class CustomAdapter extends RecyclerView.Adapter<ImageViewHolder> {

        private final Context context;
        private final LayoutInflater mInflater;
        private List<String> urls;

        public CustomAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
            this.context = context;
            this.urls = new ArrayList<>();
        }

        public CustomAdapter(Context context, List<String> urls) {
            mInflater = LayoutInflater.from(context);
            this.context = context;
            this.urls = urls;
        }

        public void changeList(List<String> urls) {
            this.urls = urls;
            notifyDataSetChanged();
        }
        @Override
        public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = mInflater.inflate(R.layout.image_item, parent, false);

            return new ImageViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ImageViewHolder holder, int position) {
            holder.bindToHolder(this.context, this.urls.get(position));
        }

        @Override
        public int getItemCount() {
            return urls.size();
        }
    }

    class ImageViewHolder extends RecyclerView.ViewHolder {

        private static final String TAG = "ImageViewHolder";
        ImageView imageView;

        public ImageViewHolder(View view) {
            super(view);

            imageView = view.findViewById(R.id.imageView);
        }

        public void bindToHolder(Context context, String url) {
            Transformation transformation = new Transformation() {

                @Override
                public Bitmap transform(Bitmap source) {
                    Bitmap result = source;

                    int targetWidth = imageView.getWidth();

                    if (targetWidth != 0) {
                        Log.i(TAG, "sourceWidth: " + source.getWidth() + ", sourceHeight: " + source.getHeight());
                        double aspectRatio = (double) source.getHeight() / (double) source.getWidth();
                        int targetHeight = (int) (targetWidth * aspectRatio);
                        Log.i(TAG, "targetWidth: " + targetWidth + ", targetHeight: " + targetHeight);
                        result = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false);
                        if (result != source) {
                            // Same bitmap is returned if sizes are the same
                            source.recycle();
                        }
                    }

                    return result;
                }

                @Override
                public String key() {
                    return "transformation" + " desiredWidth";
                }
            };

            Picasso.with(context)
                    .load(url)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.error)
                    .transform(transformation)
                    .into(imageView);
        }

    }

    class DownloadTask extends AsyncTask<String, Void, List<String>> {

        private final CustomAdapter adapter;
        private final String url;

        public DownloadTask(CustomAdapter adapter, String url) {
            this.adapter = adapter;
            this.url = url;
        }

        @Override
        protected List<String> doInBackground(String... strings) {
            List<String> urls = null;
            try {
                InputStream inputStream = NetUtils.downloadUrl(url);
                urls = parseUrls(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return urls;
        }

        private List<String> parseUrls(InputStream stream) throws IOException {
            List<String> urls = new ArrayList<>();

            Document doc = Jsoup.parse(stream, "UTF-8", url);
            Elements elements =  doc.select("#imgholder a img[src]");
            for (Element element : elements) {
                String url = element.attr("src");
                urls.add(url);
            }

            return urls;

        }

        @Override
        protected void onPostExecute(List<String> strings) {
            super.onPostExecute(strings);
            this.adapter.changeList(strings);
        }

    }
}
