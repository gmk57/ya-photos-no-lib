package gmk57.yaphotosnolib;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Background thread to handle thumbnail loading requests. Provides caching
 * bitmaps and preloading to cache for smoother UX. Returns result through
 * ThumbnailDownloadListener
 *
 * @param <T> Type of target to identify request and deliver result (for
 *            example, ViewHolder)
 */
public class ThumbnailDownloadThread<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloadThread";
    private static final int MESSAGE_DOWNLOAD = 0;
    private static final int MESSAGE_PRELOAD = 1;

    private boolean mHasQuit;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private Handler mRequestHandler;
    private Handler mResponseHandler;
    private LruCache<String, Bitmap> mBitmapLruCache;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    public ThumbnailDownloadThread(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
        mBitmapLruCache = new LruCache<>(100);
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    @SuppressWarnings("unchecked") T target = (T) msg.obj;
                    handleLoadRequest(target);
                } else if (msg.what == MESSAGE_PRELOAD) {
                    String url = (String) msg.obj;
                    handlePreloadRequest(url);
                }
            }
        };
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    /**
     * Gets thumbnail for given target from cache or queues it for download
     * from network. Called in main thread. Result is returned through
     * mThumbnailDownloadListener.
     *
     * @param target Target to deliver result
     * @param url    Url of thumbnail
     */
    public void loadThumbnail(T target, String url) {
        if (url == null) {
            mRequestMap.remove(target);
            Log.e(TAG, "Error in loadThumbnail: url is null");
        } else {
            mRequestMap.put(target, url);
            Bitmap cachedBitmap = mBitmapLruCache.get(url);
            if (cachedBitmap != null) {
                mThumbnailDownloadListener.onThumbnailDownloaded(target, cachedBitmap);
            } else {
                Message message = mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target);
                // LIFO for: 1) quicker reaction on fast scrolling,
                //           2) prioritizing downloads to PhotoHolder over preloads to cache
                mRequestHandler.sendMessageAtFrontOfQueue(message);
            }
        }
    }

    /**
     * Queues thumbnail to preload (with low priority) to cache, if not already
     * there. Called in main thread.
     *
     * @param url Url of thumbnail
     */
    public void preloadThumbnail(final String url) {
        if (mBitmapLruCache.get(url) == null) {
            mRequestHandler.obtainMessage(MESSAGE_PRELOAD, url).sendToTarget();
        }
    }

    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestHandler.removeMessages(MESSAGE_PRELOAD);
        mRequestMap.clear();
    }

    /**
     * Handles load request. Executed in background thread.
     * Double checks that requested URL is still relevant for this target.
     * Result is returned through mRequestHandler and mThumbnailDownloadListener.
     *
     * @param target Target to identify request and deliver result
     */
    private void handleLoadRequest(final T target) {
        try {
            final String url = mRequestMap.get(target);  // Get the last requested url for this target
            if (url == null) {
                return;   // We've already loaded bitmap for this target
            }
            final Bitmap bitmap = downloadImage(url);

            mResponseHandler.post(() -> {
                if (!url.equals(mRequestMap.get(target)) || mHasQuit) {
                    return;  // Another URL was requested for target or we're dying
                }

                mRequestMap.remove(target);  // This target is finally fed up
                mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
            });
        } catch (IOException e) {
            Log.e(TAG, "Error loading thumbnail: " + e);
        }
    }

    /**
     * Handles preload request. Executed in background thread. Result is put
     * into cache for future use.
     *
     * @param url URL to download
     */
    private void handlePreloadRequest(String url) {
        try {
            downloadImage(url);
        } catch (IOException e) {
            Log.e(TAG, "Error preloading thumbnail: " + e);
        }
    }

    /**
     * If image is not already in cache, actually downloads it from network and
     * puts it in cache. Executed in background thread.
     *
     * @param url URL to download
     * @return Decoded bitmap
     * @throws IOException
     */
    private Bitmap downloadImage(String url) throws IOException {
        Bitmap bitmap = mBitmapLruCache.get(url);  // Maybe is already downloaded while we waited in queue?
        if (bitmap == null) {
            byte[] bitmapBytes = new YaDownloader().downloadBytes(url);
            bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            mBitmapLruCache.put(url, bitmap);
        }
        return bitmap;
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownloadListener = listener;
    }

    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }
}
