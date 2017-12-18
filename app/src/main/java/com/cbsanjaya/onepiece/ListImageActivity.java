package com.cbsanjaya.onepiece;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ListImageActivity extends AppCompatActivity {

    public static String EXTRA_CHAPTER = "EXTRA_CHAPTER";
    public static String EXTRA_TITLE = "EXTRA_TITLE";
    WebView mWvImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_image);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        String chapter = getIntent().getStringExtra(EXTRA_CHAPTER);
        String title = getIntent().getStringExtra(EXTRA_TITLE);

        String titleEpisode = chapter + " : " + title;

        setTitle(titleEpisode);

        String url = "https://www.cbsanjaya.com/onepiece/chapter/" + chapter;
        mWvImage = findViewById(R.id.wvImage);
        WebSettings settings= mWvImage.getSettings();
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setAppCachePath(getCacheDir().getPath());
        settings.setAppCacheEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        mWvImage.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                String errorString = getString(R.string.connection_error);
                String html = "<html><body><center>"+ errorString +"</center></body></html>";
                view.loadData(html, "text/html", null);
            }
        });
        mWvImage.loadUrl(url);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mWvImage.restoreState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mWvImage.saveState(outState);
    }
}
