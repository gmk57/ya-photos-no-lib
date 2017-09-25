package gmk57.yaphotosnolib;

import android.app.Fragment;

public class AlbumActivity extends BaseActivity {

    @Override
    protected Fragment createFragment() {
        return new AlbumFragment();
    }
}
