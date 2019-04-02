package edu.buffalo.cse.cse486586.groupmessenger2;


import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 *
 * Please read:
 *
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 *
 * before you start to get yourself familiarized with ContentProvider.
 *
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 *
 * @author stevko, Daniel Amirtharaj
 *
 */

/*
 * Referred the following link on how to use SqlLite,
 * https://developer.android.com/training/data-storage/sqlite
 * with focus on how to specify a database schema in SqlLite with the
 * SqlLiteOpenHelper class and contract Class, and how to create the database
 * using the helper class and access the database for querying with
 * getReadableDatabase and updating and inserting with getWritableDatabase
 * functions of the helper Class.
 */

public class GroupMessengerProvider extends ContentProvider {

    private AppDatabase dbHelper;
    private SQLiteDatabase rdb;
    private SQLiteDatabase wdb;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
        Cursor query = (Cursor) rdb.query(
                Messages.FeedEntry.TABLE_NAME,   // The table to query
                new String[]{Messages.FeedEntry.COLUMN_NAME_KEY},             // The array of columns to return (pass null to get all)
                Messages.FeedEntry.COLUMN_NAME_KEY+"=?",              // The columns for the WHERE clause
                new String[]{values.getAsString(Messages.FeedEntry.COLUMN_NAME_KEY)},          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null                    // The sort order
        );
        if (query.getCount() == 0) {
            wdb.insert(Messages.FeedEntry.TABLE_NAME, null, values);
        }
        else {
            wdb.update(Messages.FeedEntry.TABLE_NAME, values, Messages.FeedEntry.COLUMN_NAME_KEY+"=?",
                    new String[]{values.getAsString(Messages.FeedEntry.COLUMN_NAME_KEY)});
        }
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        dbHelper = new AppDatabase(getContext());
        rdb = dbHelper.getReadableDatabase();
        wdb = dbHelper.getWritableDatabase();

        return true;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
        Cursor query = (Cursor) rdb.query(
                Messages.FeedEntry.TABLE_NAME,   // The table to query
                null,             // The array of columns to return (pass null to get all)
                Messages.FeedEntry.COLUMN_NAME_KEY+"=?",              // The columns for the WHERE clause
                new String[]{selection},          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                       // don't filter by row groups
                null// The sort order
        );
        return query;
    }
}

