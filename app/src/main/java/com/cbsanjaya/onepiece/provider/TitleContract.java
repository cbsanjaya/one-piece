package com.cbsanjaya.onepiece.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public class TitleContract {
    private TitleContract() {
    }

    /**
     * Content provider authority.
     */
    public static final String CONTENT_AUTHORITY = "com.cbsanjaya.onepiece";

    /**
     * Base URI. (content://com.cbsanjaya.onepice)
     */
    static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /**
     * Path component for "title"-type resources..
     */
    private static final String PATH_TITLES = "titles";

    /**
     * Columns supported by "titles" records.
     */
    public static class Title implements BaseColumns {
        /**
         * MIME type for lists of entries.
         */
        static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.onepiece.titles";
        /**
         * MIME type for individual entries.
         */
        static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.onepiece.title";

        /**
         * Fully qualified URI for "title" resources.
         */
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TITLES).build();

        /**
         * Table name where records are stored for "entry" resources.
         */
        static final String TABLE_NAME = "onepiece_chapter";
        /**
         * Chapter Number
         */
        public static final String COLUMN_NAME_CHAPTER = "chapter";
        /**
         * Title Chpater
         */
        public static final String COLUMN_NAME_TITLE = "title";
    }
}
