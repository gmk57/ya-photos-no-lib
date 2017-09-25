package gmk57.yaphotosnolib;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.IOException;

class PhotoLoader extends AsyncTaskLoader<Bitmap> {
    private static final String TAG = "PhotoLoader";
    private String mUrl;
    private Bitmap mBitmap;

    public PhotoLoader(Context context, String url) {
        super(context);
        mUrl = url;
    }

    @Override
    protected void onStartLoading() {
        if (mBitmap != null) {
            deliverResult(mBitmap);
        } else {
            forceLoad();
        }
    }

    @Override
    public Bitmap loadInBackground() {
        try {
            byte[] bitmapBytes = new YaDownloader().downloadBytes(mUrl);
            return BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
        } catch (IOException e) {
            Log.e(TAG, "Error downloading image: " + e);
        }
        return null;
    }

    @Override
    public void deliverResult(Bitmap bitmap) {
        mBitmap = bitmap;
        super.deliverResult(bitmap);
    }
}
