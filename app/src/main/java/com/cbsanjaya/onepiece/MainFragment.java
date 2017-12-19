package com.cbsanjaya.onepiece;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.cbsanjaya.onepiece.provider.TitleContract;
import com.cbsanjaya.onepiece.sync.SyncTitleUtils;
import com.cbsanjaya.onepiece.utils.GenericAccountService;

public class MainFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "MainFragment";

    /**
     * Cursor adapter for controlling ListView results.
     */
    private SimpleCursorAdapter mAdapter;

    /**
     * Handle to a SyncObserver. The ProgressBar element is visible until the SyncObserver reports
     * that the sync is complete.
     *
     * <p>This allows us to delete our SyncObserver once the application is no longer in the
     * foreground.
     */
    private Object mSyncObserverHandle;

    /**
     * Options menu used to populate ActionBar.
     */
    private Menu mOptionsMenu;

    /**
     * Projection for querying the content provider.
     */
    private static final String[] PROJECTION = new String[]{
            TitleContract.Title._ID,
            TitleContract.Title.COLUMN_NAME_CHAPTER,
            TitleContract.Title.COLUMN_NAME_TITLE
    };

    // Column indexes. The index of a column in the Cursor is the same as its relative position in
    // the projection.
    /** Column index for title */
    private static final int COLUMN_CHAPTER = 1;
    /** Column index for link */
    private static final int COLUMN_TITLE = 2;

    /**
     * List of Cursor columns to read from when preparing an adapter to populate the ListView.
     */
    private static final String[] FROM_COLUMNS = new String[]{
            TitleContract.Title.COLUMN_NAME_CHAPTER,
            TitleContract.Title.COLUMN_NAME_TITLE
    };

    /**
     * List of Views which will be populated by Cursor data.
     */
    private static final int[] TO_FIELDS = new int[]{
            android.R.id.text1,
            android.R.id.text2};

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MainFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    /**
     * Create SyncAccount at launch, if needed.
     *
     * <p>This will create a new account with the system for our application, register our
     * {@link com.cbsanjaya.onepiece.sync.SyncTitleService} with it, and establish a sync schedule.
     */
    @Override
    public void onAttach(Context context) {
        Log.i(TAG, "onAttach");
        super.onAttach(context);

        // Create account, if needed
        SyncTitleUtils.CreateSyncAccount(context);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        Log.i(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null) {
            mAdapter = new SimpleCursorAdapter(
                    getActivity(),       // Current context
                    android.R.layout.simple_list_item_activated_2,  // Layout for individual rows
                    null,                // Cursor
                    FROM_COLUMNS,        // Cursor columns to use
                    TO_FIELDS,           // Layout fields to use
                    0                    // No flags
            );
        }

        setListAdapter(mAdapter);
        setEmptyText(getText(R.string.loading));
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        mSyncStatusObserver.onStatusChanged(0);

        // Watch for sync state changes
        final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING |
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
        mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask, mSyncStatusObserver);
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        if (mSyncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        }
    }

    /**
     * Query the content provider for data.
     *
     * <p>Loaders do queries in a background thread. They also provide a ContentObserver that is
     * triggered when data in the content provider changes. When the sync adapter updates the
     * content provider, the ContentObserver responds by resetting the loader and then reloading
     * it.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.i(TAG, "onCreateLoader");
        // We only have one loader, so we can ignore the value of i.
        // (It'll be '0', as set in onCreate().)
        CursorLoader loader = null;
        if(getActivity() != null) {
            loader = new CursorLoader(getActivity(),  // Context
                    TitleContract.Title.CONTENT_URI, // URI
                    PROJECTION,                // Projection
                    null,                           // Selection
                    null,                           // Selection args
                    TitleContract.Title.COLUMN_NAME_CHAPTER + " desc"); // Sort
        }
        return loader;
    }

    /**
     * Move the Cursor returned by the query into the ListView adapter. This refreshes the existing
     * UI with the data in the Cursor.
     */
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.i(TAG, "onLoadFinished");
        mAdapter.changeCursor(cursor);
    }

    /**
     * Called when the ContentObserver defined for the content provider detects that data has
     * changed. The ContentObserver resets the loader, and then re-runs the loader. In the adapter,
     * set the Cursor value to null. This removes the reference to the Cursor, allowing it to be
     * garbage-collected.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        Log.i(TAG, "onLoaderReset");
        mAdapter.changeCursor(null);
    }

    /**
     * Create the ActionBar.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.i(TAG, "onCreateOptionsMenu");
        super.onCreateOptionsMenu(menu, inflater);
        mOptionsMenu = menu;
        inflater.inflate(R.menu.main, menu);
    }

    /**
     * Respond to user gestures on the ActionBar.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "onOptionsItemSelected: " + item.getItemId());
        switch (item.getItemId()) {
            // If the user clicks the "Refresh" button.
            case R.id.menu_refresh:
                SyncTitleUtils.TriggerRefresh();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Load an article in the default browser when selected by the user.
     */
    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        Log.i(TAG, "onListItemClick");
        super.onListItemClick(listView, view, position, id);

        // Get a URI for the selected item, then start an Activity that displays the URI. Any
        // Activity that filters for ACTION_VIEW and a URI can accept this. In most cases, this will
        // be a browser.

        // Get the item at the selected position, in the form of a Cursor.
        Cursor c = (Cursor) mAdapter.getItem(position);
        // Get the link to the article represented by the item.
        String chapterString = c.getString(COLUMN_CHAPTER);
        if (chapterString == null) {
            Log.e(TAG, "Attempt to launch entry with null link");
            return;
        }

        Log.i(TAG, "Opening URL: " + chapterString);
        Intent i = new Intent(this.getActivity(), ListImageActivity.class);
        i.putExtra(ListImageActivity.EXTRA_CHAPTER, c.getString(COLUMN_CHAPTER));
        i.putExtra(ListImageActivity.EXTRA_TITLE, c.getString(COLUMN_TITLE));
        startActivity(i);
    }

    /**
     * Set the state of the Refresh button. If a sync is active, turn on the ProgressBar widget.
     * Otherwise, turn it off.
     *
     * @param refreshing True if an active sync is occuring, false otherwise
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void setRefreshActionButtonState(boolean refreshing) {
        Log.i(TAG, "setRefreshActionButtonState");
        if (mOptionsMenu == null) {
            return;
        }

        final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
        if (refreshItem != null) {
            if (refreshing) {
                refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
            } else {
                refreshItem.setActionView(null);
            }
        }
    }

    /**
     * Crfate a new anonymous SyncStatusObserver. It's attached to the app's ContentResolver in
     * onResume(), and removed in onPause(). If status changes, it sets the state of the Refresh
     * button. If a sync is active or pending, the Refresh button is replaced by an indeterminate
     * ProgressBar; otherwise, the button itself is displayed.
     */
    private final SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        /** Callback invoked with the sync adapter status changes. */
        @Override
        public void onStatusChanged(int which) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    /**
                     * The SyncAdapter runs on a background thread. To update the UI, onStatusChanged()
                     * runs on the UI thread.
                     */
                    @Override
                    public void run() {
                        // Create a handle to the account that was created by
                        // SyncService.CreateSyncAccount(). This will be used to query the system to
                        // see how the sync status has changed.
                        Account account = GenericAccountService.GetAccount(SyncTitleUtils.ACCOUNT_TYPE);

                        // Test the ContentResolver to see if the sync adapter is active or pending.
                        // Set the state of the refresh button accordingly.
                        boolean syncActive = ContentResolver.isSyncActive(
                                account, TitleContract.CONTENT_AUTHORITY);
                        boolean syncPending = ContentResolver.isSyncPending(
                                account, TitleContract.CONTENT_AUTHORITY);
                        setRefreshActionButtonState(syncActive || syncPending);
                    }
                });
            }
        }
    };
}
