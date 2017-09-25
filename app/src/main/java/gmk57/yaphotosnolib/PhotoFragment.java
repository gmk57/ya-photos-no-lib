package gmk57.yaphotosnolib;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Loader;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Fragment to display full-screen photo
 */
public class PhotoFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Bitmap> {
    private static final String TAG = "PhotoFragment";
    private static final String EXTRA_IMAGE_URL = "gmk57.yaphotosnolib.photoImageUrl";
    private static final String EXTRA_TITLE = "gmk57.yaphotosnolib.photoTitle";
    private static final String KEY_UI_VISIBLE = "uiVisible";
    private static final int LOADER_ID_PHOTOLOADER = 0;

    private boolean mUiVisible = true;
    private ImageView mImageView;
    private String mPhotoImageUrl;

    public static PhotoFragment newInstance(Bundle args) {
        PhotoFragment fragment = new PhotoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Creates arguments for hosting activity to use (for example, in its
     * <code>newIntent</code> method)
     *
     * @param photoImageUrl Url to load image from
     * @param photoTitle    Title to display in ActionBar
     * @return Bundle of arguments
     */
    public static Bundle createArguments(String photoImageUrl, String photoTitle) {
        Bundle args = new Bundle();
        args.putString(EXTRA_IMAGE_URL, photoImageUrl);
        args.putString(EXTRA_TITLE, photoTitle);
        return args;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mUiVisible = savedInstanceState.getBoolean(KEY_UI_VISIBLE, true);
        }
        mPhotoImageUrl = getArguments().getString(EXTRA_IMAGE_URL);
        String photoTitle = getArguments().getString(EXTRA_TITLE);
        getActivity().getActionBar().setSubtitle(photoTitle);
    }


    @Override
    public View createView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo, container, false);
        mImageView = (ImageView) view.findViewById(R.id.fullscreen_image_view);
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUiVisible = !mUiVisible;
                setupUiVisibility();
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupProgressState(STATE_LOADING);
        getLoaderManager().initLoader(LOADER_ID_PHOTOLOADER, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        setupUiVisibility(); // Reset flags to persist if the user navigates out and back in
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_UI_VISIBLE, mUiVisible);
    }

    @Override
    public Loader<Bitmap> onCreateLoader(int id, Bundle args) {
        return new PhotoLoader(getActivity(), mPhotoImageUrl);
    }

    @Override
    public void onLoadFinished(Loader<Bitmap> loader, Bitmap bitmap) {
        if (bitmap != null) {
            setupProgressState(STATE_OK);
            mImageView.setImageBitmap(bitmap);
        } else {
            setupProgressState(STATE_ERROR);
        }
    }

    @Override
    public void onLoaderReset(Loader<Bitmap> loader) {/* not needed */}

    @Override
    protected void tryAgain() {
        getLoaderManager().restartLoader(LOADER_ID_PHOTOLOADER, null, PhotoFragment.this);
    }

    /**
     * Hides or shows system UI and ActionBar according to current
     * <code>mUiVisible</code> value. Status bar is completely hidden
     * (on API >= 16) and navigation bar is dimmed.
     */
    private void setupUiVisibility() {
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        Activity activity = getActivity();
        if (!mUiVisible) {
            activity.getActionBar().hide();
            uiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
        } else {
            activity.getActionBar().show();
        }
        mImageView.setSystemUiVisibility(uiOptions);
    }
}
