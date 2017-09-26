package gmk57.yaphotosnolib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Spinner;

import java.util.List;

/**
 * Main app fragment to display album thumbnails, with scrolling (endless, if
 * possible)
 */
public class AlbumFragment extends BaseFragment implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "AlbumFragment";
    private static final String KEY_ALBUM_TYPE = "albumType";

    private boolean mFetchRunning;
    private int mAlbumType = -1;  // Sentinel. Valid values = YaDownloader.ALBUM_PATHS indexes
    private Album mCurrentAlbum = new Album(null);  // To keep current album on rotation
    private FetchAlbumTask mFetchAlbumTask;
    private PhotoAdapter mPhotoAdapter;
    private ThumbnailDownloadThread<PhotoHolder> mThumbnailDownloadThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);  // To keep image cache in ThumbnailDownloadThread on rotation
        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            mAlbumType = savedInstanceState.getInt(KEY_ALBUM_TYPE, -1);
        }
        if (mAlbumType < 0) {
            mAlbumType = new PreferenceConnector(getActivity()).getAlbumType();
        }

        setupProgressState(STATE_LOADING);
        startFetchingAlbum();

        Handler responseHandler = new Handler();
        mThumbnailDownloadThread = new ThumbnailDownloadThread<>(responseHandler);
        mThumbnailDownloadThread.setThumbnailDownloadListener(
                new ThumbnailDownloadThread.ThumbnailDownloadListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        photoHolder.bindDrawable(drawable);
                    }
                });
        mThumbnailDownloadThread.start();
        mThumbnailDownloadThread.getLooper();
    }

    @Override
    public View createView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_album, container, false);
        GridView gridView = (GridView) view.findViewById(R.id.album_grid_view);

        mPhotoAdapter = new PhotoAdapter(getActivity(), mCurrentAlbum.getPhotos());
        gridView.setAdapter(mPhotoAdapter);

        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {/* not needed */}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {
                if (!mFetchRunning && mCurrentAlbum.getNextPage() != null &&
                        firstVisibleItem + visibleItemCount + 40 > totalItemCount) {
                    startFetchingAlbum(mCurrentAlbum);
                }
            }
        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_album, menu);

        Spinner albumTypeSpinner = (Spinner) menu.findItem(R.id.album_type_spinner).getActionView();
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.album_names, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        albumTypeSpinner.setAdapter(adapter);
        albumTypeSpinner.setSelection(mAlbumType);
        albumTypeSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position != mAlbumType) {
            mAlbumType = position;
            new PreferenceConnector(getActivity()).setAlbumType(position);
            mThumbnailDownloadThread.clearQueue();
            mCurrentAlbum = new Album(null);
            mPhotoAdapter.clear();
            setupProgressState(STATE_LOADING);
            startFetchingAlbum();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {/* not needed */}

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ALBUM_TYPE, mAlbumType);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloadThread.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloadThread.quit();
    }

    @Override
    protected void tryAgain() {
        startFetchingAlbum();
    }

    /**
     * Cancels current fetching and starts a new one via <code>FetchAlbumTask</code>.
     * <p>
     * If old album is provided, new album will be built on top of it, appending
     * photos of its <code>getNextPage()</code>.
     * Otherwise, new album will be built from scratch, according to current album type.
     *
     * @param oldAlbumVararg Old album (to append) or empty (to create from scratch)
     */
    private void startFetchingAlbum(Album... oldAlbumVararg) {
        mFetchRunning = true;
        if (mFetchAlbumTask != null) {
            mFetchAlbumTask.cancel(false);
        }
        mFetchAlbumTask = new FetchAlbumTask();
        mFetchAlbumTask.execute(oldAlbumVararg);
    }

    private class PhotoHolder implements View.OnClickListener {
        private ImageView mThumbnailImageView;
        private Photo mPhoto;

        public PhotoHolder(View itemView) {
            mThumbnailImageView = (ImageView) itemView.findViewById(R.id.thumbnail_image_view);
            itemView.setOnClickListener(this);
            itemView.setTag(this);
        }

        public void bindDrawable(Drawable drawable) {
            mThumbnailImageView.setImageDrawable(drawable);
        }

        public void bindPhoto(Photo photo) {
            mPhoto = photo;
        }

        @Override
        public void onClick(View v) {
            startActivity(PhotoActivity.newIntent(getActivity(), mPhoto.getImageUrl(),
                    mPhoto.getTitle()));
        }
    }

    private class PhotoAdapter extends ArrayAdapter<Photo> {
        private static final String TAG = "PhotoAdapter";

        public PhotoAdapter(Context context, List<Photo> objects) {
            super(context, R.layout.thumbnail, objects);
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PhotoHolder holder;
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                view = inflater.inflate(R.layout.thumbnail, parent, false);
                holder = new PhotoHolder(view);
            } else {
                holder = (PhotoHolder) view.getTag();
            }

            Photo photo = getItem(position);
            holder.bindPhoto(photo);
            Drawable placeholder = getResources().getDrawable(android.R.drawable.ic_menu_gallery);
            holder.bindDrawable(placeholder);
            mThumbnailDownloadThread.loadThumbnail(holder, photo.getThumbnailUrl());

            for (int offset : new int[]{8, -8, 16, -16, 24, -24}) {
                if (position + offset >= 0 && position + offset < getCount()) {
                    String url = getItem(position + offset).getThumbnailUrl();
                    mThumbnailDownloadThread.preloadThumbnail(url);
                }
            }

            return view;
        }
    }

    private class FetchAlbumTask extends AsyncTask<Album, Void, Album> {
        private static final String TAG = "FetchAlbumTask";
        private int mType;

        @Override
        protected void onPreExecute() {
            mType = mAlbumType;
        }

        @Override
        protected Album doInBackground(Album... oldAlbumVararg) {
            return new YaDownloader().fetchAlbum(mType, oldAlbumVararg);
        }

        @Override
        protected void onPostExecute(Album album) {
            mCurrentAlbum = album;
            if (isAdded() && mPhotoAdapter != null && album.getSize() > 0) {
                mPhotoAdapter.clear();
                mPhotoAdapter.addAll(album.getPhotos());
                setupProgressState(STATE_OK);
            } else {
                setupProgressState(STATE_ERROR);
            }
            mFetchRunning = false;
        }
    }
}
