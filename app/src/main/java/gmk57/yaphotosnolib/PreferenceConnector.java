package gmk57.yaphotosnolib;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Helper class to store and retrieve preferences via SharedPreferences
 */

public class PreferenceConnector {
    private static final String PREF_ALBUM_TYPE = "albumTypeInt";
    private SharedPreferences mSharedPreferences;

    public PreferenceConnector(Context context) {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public int getAlbumType() {
        return mSharedPreferences.getInt(PREF_ALBUM_TYPE, 0);
    }

    public void setAlbumType(int albumType) {
        mSharedPreferences.edit()
                .putInt(PREF_ALBUM_TYPE, albumType)
                .apply();
    }
}
