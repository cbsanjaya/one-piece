package com.cbsanjaya.onepiece.sync;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.cbsanjaya.onepiece.provider.TitleContract;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class SyncTitleAdapter extends AbstractThreadedSyncAdapter {
    public static final String TAG = "SyncTitleAdapter";

    /**
     * URL to fetch content from during a sync.
     *
     * <p>This points to the Android Developers Blog. (Side note: We highly recommend reading the
     * Android Developer Blog to stay up to date on the latest Android platform developments!)
     */
    private static final String DOMAIN_URL = "http://www.mangacanblog.com/";
    private static final String TITLE_URL = DOMAIN_URL + "baca-komik-one_piece-bahasa-indonesia-online-terbaru.html";

    /**
     * Network connection timeout, in milliseconds.
     */
    private static final int NET_CONNECT_TIMEOUT_MILLIS = 15000;  // 15 seconds

    /**
     * Network read timeout, in milliseconds.
     */
    private static final int NET_READ_TIMEOUT_MILLIS = 10000;  // 10 seconds

    /**
     * Content resolver, for performing database operations.
     */
    private final ContentResolver mContentResolver;

    /**
     * Project used when querying content provider. Returns all known fields.
     */
    private static final String[] PROJECTION = new String[] {
            TitleContract.Title._ID,
            TitleContract.Title.COLUMN_NAME_TITLE,
            TitleContract.Title.COLUMN_NAME_LINK,
            TitleContract.Title.COLUMN_NAME_PUBLISHED};

    // Constants representing column positions from PROJECTION.
    public static final int COLUMN_ID = 0;
    public static final int COLUMN_TITLE = 1;
    public static final int COLUMN_LINK = 2;
    public static final int COLUMN_PUBLISHED = 3;

    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    public SyncTitleAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContentResolver = context.getContentResolver();
    }

    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public SyncTitleAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mContentResolver = context.getContentResolver();
    }

    /**
     * Called by the Android system in response to a request to run the sync adapter. The work
     * required to read data from the network, parse it, and store it in the content provider is
     * done here. Extending AbstractThreadedSyncAdapter ensures that all methods within SyncAdapter
     * run on a background thread. For this reason, blocking I/O and other long-running tasks can be
     * run <em>in situ</em>, and you don't have to set up a separate thread for them.
     .
     *
     * <p>This is where we actually perform any work required to perform a sync.
     * {@link android.content.AbstractThreadedSyncAdapter} guarantees that this will be called on a non-UI thread,
     * so it is safe to peform blocking I/O here.
     *
     * <p>The syncResult argument allows you to pass information back to the method that triggered
     * the sync.
     */
    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        Log.i(TAG, "Beginning network synchronization");
        try {
            final URL location = new URL(TITLE_URL);
            InputStream stream = null;

            try {
                Log.i(TAG, "Streaming data from network: " + location);
                stream = downloadUrl(location);
                updateLocalFeedData(stream, syncResult);
                // Makes sure that the InputStream is closed after the app is
                // finished using it.
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        } catch (MalformedURLException e) {
            Log.e(TAG, "Feed URL is malformed", e);
            syncResult.stats.numParseExceptions++;
            return;
        } catch (IOException e) {
            Log.e(TAG, "Error reading from network: " + e.toString());
            syncResult.stats.numIoExceptions++;
            return;
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Error parsing feed: " + e.toString());
            syncResult.stats.numParseExceptions++;
            return;
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing feed: " + e.toString());
            syncResult.stats.numParseExceptions++;
            return;
        } catch (RemoteException e) {
            Log.e(TAG, "Error updating database: " + e.toString());
            syncResult.databaseError = true;
            return;
        } catch (OperationApplicationException e) {
            Log.e(TAG, "Error updating database: " + e.toString());
            syncResult.databaseError = true;
            return;
        }
        Log.i(TAG, "Network synchronization complete");
    }

    public void updateLocalFeedData(final InputStream stream, final SyncResult syncResult)
            throws IOException, XmlPullParserException, RemoteException,
            OperationApplicationException, ParseException {
        final ContentResolver contentResolver = getContext().getContentResolver();

        Log.i(TAG, "Parsing stream");
        final List<Title> titles = parseTitle(stream);
        Log.i(TAG, "Parsing complete. Found " + titles.size() + " titles");

        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();

        // Build hash table of incoming titles
        HashMap<String, Title> entryMap = new HashMap<String, Title>();
        for (Title e : titles) {
            entryMap.put(e.link, e);
        }

        // Get list of all items
        Log.i(TAG, "Fetching local titles for merge");
        Uri uri = TitleContract.Title.CONTENT_URI; // Get all titles
        Cursor c = contentResolver.query(uri, PROJECTION, null, null, null);
        assert c != null;
        Log.i(TAG, "Found " + c.getCount() + " local titles. Computing merge solution...");

        // Find stale data
        int id;
        String title;
        String link;
        Long published;
        while (c.moveToNext()) {
            syncResult.stats.numEntries++;
            id = c.getInt(COLUMN_ID);
            title = c.getString(COLUMN_TITLE);
            link = c.getString(COLUMN_LINK);
            published = c.getLong(COLUMN_PUBLISHED);
            Title match = entryMap.get(link);
            if (match != null) {
                // Entry exists. Remove from entry map to prevent insert later.
                entryMap.remove(link);
                // Check to see if the entry needs to be updated
                Uri existingUri = TitleContract.Title.CONTENT_URI.buildUpon()
                        .appendPath(Integer.toString(id)).build();
                if ((match.title != null && !match.title.equals(title)) ||
                        (match.link != null && !match.link.equals(link)) ||
                        (match.published != published)) {
                    // Update existing record
                    Log.i(TAG, "Scheduling update: " + existingUri);
                    batch.add(ContentProviderOperation.newUpdate(existingUri)
                            .withValue(TitleContract.Title.COLUMN_NAME_TITLE, match.title)
                            .withValue(TitleContract.Title.COLUMN_NAME_LINK, match.link)
                            .withValue(TitleContract.Title.COLUMN_NAME_PUBLISHED, match.published)
                            .build());
                    syncResult.stats.numUpdates++;
                } else {
                    Log.i(TAG, "No action: " + existingUri);
                }
            } else {
                // Entry doesn't exist. Remove it from the database.
                Uri deleteUri = TitleContract.Title.CONTENT_URI.buildUpon()
                        .appendPath(Integer.toString(id)).build();
                Log.i(TAG, "Scheduling delete: " + deleteUri);
                batch.add(ContentProviderOperation.newDelete(deleteUri).build());
                syncResult.stats.numDeletes++;
            }
        }
        c.close();

        // Add new items
        for (Title e : entryMap.values()) {
            Log.i(TAG, "Scheduling insert: link=" + e.link);
            batch.add(ContentProviderOperation.newInsert(TitleContract.Title.CONTENT_URI)
                    .withValue(TitleContract.Title.COLUMN_NAME_TITLE, e.title)
                    .withValue(TitleContract.Title.COLUMN_NAME_LINK, e.link)
                    .withValue(TitleContract.Title.COLUMN_NAME_PUBLISHED, e.published)
                    .build());
            syncResult.stats.numInserts++;
        }
        Log.i(TAG, "Merge solution ready. Applying batch update");
        mContentResolver.applyBatch(TitleContract.CONTENT_AUTHORITY, batch);
        mContentResolver.notifyChange(
                TitleContract.Title.CONTENT_URI, // URI where data was modified
                null,                           // No local observer
                false);                         // IMPORTANT: Do not sync to network
        // This sample doesn't support uploads, but if *your* code does, make sure you set
        // syncToNetwork=false in the line above to prevent duplicate syncs.
    }

    private List<Title> parseTitle(InputStream stream) throws IOException {
        List<Title> titles = new ArrayList<>();

        Document doc = Jsoup.parse(stream, "UTF-8", TITLE_URL);
        Elements elements =  doc.select(".updates tr");
        for (Element element : elements) {
            titles.add(readTitle(element));
        }

        return titles;
    }

    private Title readTitle(Element element) {
        String title;
        String link;
        long published;

        title = element.select("td a[href]").get(0).text();
        String linkOri = element.select("td a[href]").get(0).attr("href");
        link = DOMAIN_URL + linkOri.replace("-terbaru-1.html", "-terbaru.html");
        String publishedOri = element.select(".c2").text();

        SimpleDateFormat format = new SimpleDateFormat("MMM, dd yyyy");
        Date date = null;
        try {
            date = format.parse(publishedOri);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        published = date.getTime();

        return new Title(title, link, published);
    }

    /**
     * Given a string representation of a URL, sets up a connection and gets an input stream.
     */
    private InputStream downloadUrl(final URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(NET_READ_TIMEOUT_MILLIS /* milliseconds */);
        conn.setConnectTimeout(NET_CONNECT_TIMEOUT_MILLIS /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Starts the query
        conn.connect();
        return conn.getInputStream();
    }

    private static class Title {
        public final String title;
        public final String link;
        public final long published;

        Title(String title, String link, long published) {
            this.title = title;
            this.link = link;
            this.published = published;
        }
    }

}
