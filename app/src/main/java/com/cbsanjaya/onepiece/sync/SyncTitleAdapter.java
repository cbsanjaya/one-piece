package com.cbsanjaya.onepiece.sync;

import android.accounts.Account;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.JsonReader;
import android.util.Log;

import com.cbsanjaya.onepiece.ListImageActivity;
import com.cbsanjaya.onepiece.R;
import com.cbsanjaya.onepiece.provider.TitleContract;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SyncTitleAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "SyncTitleAdapter";

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "com.cbsanjaya.onepiece.title_channel";

    /**
     * URL to fetch content from during a sync.
     *
     * <p>This points to the Android Developers Blog. (Side note: We highly recommend reading the
     * Android Developer Blog to stay up to date on the latest Android platform developments!)
     */
    private static final String DOMAIN_URL = "https://www.cbsanjaya.com/";
    private static final String TITLE_URL = DOMAIN_URL + "onepiece/all.json";
    private static final String TITLE_LAST5_URL = DOMAIN_URL + "onepiece/last5.json";

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
            TitleContract.Title.COLUMN_NAME_CHAPTER };

    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    SyncTitleAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
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
            String location;
            InputStream stream = null;

            try {
                Cursor cursor = getContext()
                        .getContentResolver()
                        .query(
                                TitleContract.Title.CONTENT_URI,
                                new String[] {"count(*) AS count"},
                                null,
                                null,
                                null
                        );

                assert cursor != null;
                cursor.moveToFirst();
                int count = cursor.getInt(0);
                cursor.close();

                if ( count == 0 ) {
                    location = TITLE_URL;
                } else {
                    location = TITLE_LAST5_URL;
                }

                Log.i(TAG, "Streaming data from network: " + location);
                stream = downloadUrl(new URL(location));
                updateLocalFeedData(location, stream, syncResult);
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

    private void updateLocalFeedData(final String location, final InputStream stream,
                                     final SyncResult syncResult)
            throws IOException, RemoteException,
            OperationApplicationException {
        final ContentResolver contentResolver = getContext().getContentResolver();

        Log.i(TAG, "Parsing stream");
        final List<Title> titles = parseTitle(stream);
        Log.i(TAG, "Parsing complete. Found " + titles.size() + " titles");

        ArrayList<ContentProviderOperation> batch = new ArrayList<>();

        // Build hash table of incoming titles
        HashMap<Double, Title> entryMap = new HashMap<>();
        for (Title e : titles) {
            entryMap.put(e.chapter, e);
        }

        if (location.equals(TITLE_LAST5_URL)) {
            final HashMap<Double, Title> tempMap = new HashMap<>(entryMap);
            for (Title e : tempMap.values()) {
                Uri uri = TitleContract.Title.CONTENT_URI;
                Cursor c = contentResolver.query(
                        uri,
                        PROJECTION,
                        TitleContract.Title.COLUMN_NAME_CHAPTER + " = ?",
                        new String[] {String.valueOf(e.chapter)},
                        null);
                assert c != null;
                if (c.moveToFirst()) {
                    entryMap.remove(e.chapter);
                } else {
                    sendNotification(e);
                }
                c.close();
            }
        }

        // Add new items
        for (Title e : entryMap.values()) {
            Log.i(TAG, "Scheduling insert: chapter=" + e.chapter);
            batch.add(ContentProviderOperation.newInsert(TitleContract.Title.CONTENT_URI)
                    .withValue(TitleContract.Title.COLUMN_NAME_CHAPTER, e.chapter)
                    .withValue(TitleContract.Title.COLUMN_NAME_TITLE, e.title)
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

    private void sendNotification(Title title) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        DecimalFormat format = new DecimalFormat("0.#");
        String chapter = format.format(title.chapter);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getContext(), CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Chapter " + chapter)
                .setContentText(title.title)
                .setAutoCancel(true);

        Intent i = new Intent(getContext(), ListImageActivity.class);
        i.putExtra(ListImageActivity.EXTRA_CHAPTER, chapter);
        i.putExtra(ListImageActivity.EXTRA_TITLE, title.title);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getContext());
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(ListImageActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(i);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager =
                (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

        // mNotificationId is a unique integer your app uses to identify the
        // notification. For example, to cancel the notification, you can pass its ID
        // number to NotificationManager.cancel().
        assert mNotificationManager != null;
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationManager mNotificationManager =
                (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        int importance = NotificationManager.IMPORTANCE_HIGH;

        NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID,
                getContext().getString(R.string.channel_name), importance);
        // Configure the notification channel.
        mChannel.setDescription(getContext().getString(R.string.channel_description));
        mChannel.enableLights(true);
        // Sets the notification light color for notifications posted to this
        // channel, if the device supports this feature.
        mChannel.setLightColor(Color.RED);
        mChannel.enableVibration(true);
        mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

        assert mNotificationManager != null;
        mNotificationManager.createNotificationChannel(mChannel);
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    private List<Title> parseTitle(InputStream stream) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(stream, "UTF-8"));
        try {
            return readTitleArray(reader);
        } finally {
            reader.close();
        }
    }

    private List<Title> readTitleArray(JsonReader reader) throws IOException{
        List<Title> titles = new ArrayList<>();

        reader.beginArray();
        while (reader.hasNext()) {
            titles.add(readTitle(reader));
        }
        reader.endArray();
        return titles;
    }

    private Title readTitle(JsonReader reader) throws IOException {
        Double chapter = 0D;
        String title = "";

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case "chapter":
                    String _chapter = reader.nextString();
                    chapter = Double.valueOf(_chapter);
                    break;
                case "title":
                    title = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();

        return new Title(chapter, title);
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

    private class Title {
        private final Double chapter;
        private final String title;

        Title(Double chapter, String title) {
            this.chapter = chapter;
            this.title = title;
        }
    }

}
