package com.example.xyzreader.ui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.transition.Transition;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, AppBarLayout.OnOffsetChangedListener {
    private static final String DATA_LOADED = "LOADED";
    private static final String BODY_KEY ="BODY_KEY" ;
    private final String TITLE_KEY="TITLE_KEY";
    private final String IMG_KEY="IMG_KEY";
    private final String BY_KEY="BY_KEY";
    public final String DATE_KEY="DATE_KEY";

    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";
    private static final float PARALLAX_FACTOR = 1.25f;

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private ObservableScrollView mScrollView;
    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    private ColorDrawable mStatusBarColorDrawable;

    private int mTopInset;
    private View mPhotoContainerView;
    private CustomAspectImage mPhotoView;
    private int mScrollY;
    private boolean mIsCard = false;
    private int mStatusBarFullOpacityBottom;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);
    private CollapsingToolbarLayout mCollapssingToolbar;
    private Toolbar mToolbar;
    private String title="UnTitled";
    private String byLine="N/A";

    AlphaAnimation inAnimation;
    AlphaAnimation outAnimation;

    FrameLayout progressBarHolder;
    private boolean data_loaded;
    private String mBody;
    private String mImgUrl;
    private AppBarLayout mAppBarLayout;
    private boolean mExpanded;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mRootView=null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(
                R.dimen.detail_card_top_margin);
        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        if(savedInstanceState == null||!savedInstanceState.getBoolean(DATA_LOADED)) {
            getLoaderManager().initLoader(0, null, this);
        }


    }

    @Override
    public void onResume() {
        super.onResume();
        if(mAppBarLayout!=null)
            mAppBarLayout.addOnOffsetChangedListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        mPhotoView=(CustomAspectImage)mRootView.findViewById(R.id.photo);

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            PortraitViewSetup();
        mToolbar = (Toolbar) this.mRootView.findViewById(R.id.toolbar);
        mToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });
       mAppBarLayout= (AppBarLayout) mRootView.findViewById(R.id.app_bar_layout);

        if(savedInstanceState!=null&&savedInstanceState.getBoolean(DATA_LOADED)){
            mBody=savedInstanceState.getString(BODY_KEY);
            byLine=savedInstanceState.getString(BY_KEY);
            title=savedInstanceState.getString(TITLE_KEY);
            mImgUrl=savedInstanceState.getString(IMG_KEY);
            data_loaded=savedInstanceState.getBoolean(DATA_LOADED);
            setMetaData();


        }
        else
            bindViews();
       // updateStatusBar();
        return mRootView;
    }

    private void PortraitViewSetup() {
        mCollapssingToolbar = (CollapsingToolbarLayout) this.mRootView.findViewById(R.id.collapsing_toolbar_layout);
        mPhotoView.setAspectRatio(3,4);
    }


    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }


        if (mCursor != null) {
            Date publishedDate = parsePublishedDate();
            title=mCursor.getString(ArticleLoader.Query.TITLE);
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                 byLine=Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>").toString();

            } else {
                // If date is before 1902, just show the string
                byLine=(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>")).toString();

            }
            mImgUrl=mCursor.getString(ArticleLoader.Query.PHOTO_URL);
            mBody=Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />")).toString();

        setMetaData();
        }
    }

    private void setMetaData() {
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            ((TextView)mRootView.findViewById(R.id.byLine)).setText(byLine);
            //mCollapssingToolbar=((CollapsingToolbarLayout) this.mRootView.findViewById(R.id.collapsing_toolbar_layout));
            mCollapssingToolbar.setTitle(title);

        }
        else{
            mToolbar.setSubtitle(byLine);
            mToolbar.setTitle(title);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.d(ArticleDetailFragment.class.getSimpleName(),"Fuck Item :: "+getResources().getString(R.string.article_img_transition)+mItemId);
            mPhotoView.setTransitionName(getResources().getString(R.string.article_img_transition)+mItemId);
        }
        ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                .get(mImgUrl, new ImageLoader.ImageListener() {
                    @Override
                    public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                        Bitmap bitmap = imageContainer.getBitmap();
                        if (bitmap != null) {
                            mPhotoView.setImageBitmap(imageContainer.getBitmap());
                            scheduleStartPostponedTransition(mPhotoView);
                        }
                    }

                    @Override
                    public void onErrorResponse(VolleyError volleyError) {

                    }
                });

        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);
        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));
        bodyView.setText(mBody);
    }

    private void scheduleStartPostponedTransition(final View sharedElement) {
        sharedElement.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
                    ActivityCompat.startPostponedEnterTransition(getActivity());
                return true;
            }
        });
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }
        data_loaded=true;
        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }
        if (mRootView!=null)
            bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        //if (mRootView!=null)
        //bindViews();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(DATA_LOADED,data_loaded);
        outState.putString(BODY_KEY,mBody);
        outState.putString(TITLE_KEY,title);
        outState.putString(BY_KEY,byLine);
        outState.putString(IMG_KEY,mImgUrl);
        super.onSaveInstanceState(outState);

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAppBarLayout!=null)
            mAppBarLayout.removeOnOffsetChangedListener(this);
    }

    public int getUpButtonFloor() {
        if (mPhotoContainerView == null || mPhotoView.getHeight() == 0) {
            return Integer.MAX_VALUE;
        }

        // account for parallax
        return mIsCard
                ? (int) mPhotoContainerView.getTranslationY() + mPhotoView.getHeight() - mScrollY
                : mPhotoView.getHeight() - mScrollY;
    }

    public CustomAspectImage getImage() {

        if(mRootView.findViewById(R.id.photo)!=null){
            if(mExpanded|| !(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) )
                return mPhotoView;
        }
        return null;
    }
    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        if (Math.abs(verticalOffset) == appBarLayout.getTotalScrollRange()) {
            mExpanded=false;
        } else{
            mExpanded=true;
        }
    }
}
