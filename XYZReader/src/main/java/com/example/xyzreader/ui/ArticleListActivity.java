package com.example.xyzreader.ui;

import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Adapter;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {


    public static final String CURRENT_ELEMENT_KEY ="CURR" ;
    public static final String START_ELEMENT_KEY = "START";
    private Context mContext;
    private static final String TAG = ArticleListActivity.class.toString();
    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;


    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);
    private boolean sw600=false;
    private long startItemPos=0;

    private Bundle mTmpReenterState;
    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (mTmpReenterState != null) {
                int mCurrentItem=mTmpReenterState.getInt(CURRENT_ELEMENT_KEY);
                int startItemPos=mTmpReenterState.getInt(START_ELEMENT_KEY);
                if(startItemPos!=mCurrentItem&&mRecyclerView.getAdapter()!=null){
                    View view=mRecyclerView.getChildAt(mCurrentItem);
//                    view.setTransitionName(getResources().getString(R.string.article_img_transition)+mRecyclerView.getAdapter().getItemId(mCurrentItem));
                    Log.d(ArticleListActivity.class.getSimpleName(),"Fuck Return Transition Name :: "+getResources().getString(R.string.article_img_transition)+mRecyclerView.getAdapter().getItemId(mCurrentItem));
                    if(view!=null){
                        //names.remove(mRecyclerView.getChildAt(startItemPos).getTransitionName());
                        names.clear();
                        names.add(getResources().getString(R.string.article_img_transition)+mRecyclerView.getAdapter().getItemId(mCurrentItem));

                        //sharedElements.remove(mRecyclerView.getChildAt(startItemPos));
                        sharedElements.clear();
                        sharedElements.put(getResources().getString
                                        (R.string.article_img_transition)+mRecyclerView.getAdapter().getItemId(mCurrentItem), view);
                    }

                }

                else if(mRecyclerView.getAdapter()==null) {
                    sharedElements.clear();
                    names.clear();
                    // If mTmpReenterState is null, then the activity is exiting.
                }
                mTmpReenterState = null;
            }


            }
    };

    @Override
    public void onActivityReenter(int resultCode, Intent data) {

        mTmpReenterState = new Bundle(data.getExtras());
        int mCurrentItem=mTmpReenterState.getInt(CURRENT_ELEMENT_KEY);
        int startItemPos=mTmpReenterState.getInt(START_ELEMENT_KEY);
        if(mCurrentItem!=startItemPos){
            mRecyclerView.scrollToPosition(mCurrentItem);
        }
        postPoneTransition();
        super.onActivityReenter(resultCode, data);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void postPoneTransition() {
        postponeEnterTransition();
        mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                mRecyclerView.requestLayout();
                startPostponedEnterTransition();
                return true;
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext=this;
        setContentView(R.layout.activity_article_list);
        setExitSharedElementCallback(mCallback);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);


        final View toolbarContainerView = findViewById(R.id.toolbar_container);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }
        sw600=getResources().getBoolean(R.bool.sw600);
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        GridLayoutManager gridLayoutManager=new GridLayoutManager(this,columnCount, LinearLayoutManager.VERTICAL,false);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
       // sglm.setGapStrategy(0);
        mRecyclerView.setLayoutManager(gridLayoutManager);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startItemPos=(vh.position);
                    if(!sw600){

                        Bundle bundle= null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                            bundle = ActivityOptions.makeSceneTransitionAnimation
                                    (ArticleListActivity.this,vh.thumbnailView,vh.thumbnailView.getTransitionName()).toBundle();
                            Log.d(ArticleListActivity.class.getSimpleName(),"Fuck Clicked Item :: "+getResources().getString(R.string.article_img_transition)+getItemId(vh.position));

                        }
                        Intent intent=new Intent(Intent.ACTION_VIEW,
                                ItemsContract.Items.buildItemUri(getItemId(vh.position)));
                        if(vh.bitmap!=null){
                            intent.putExtra(START_ELEMENT_KEY,vh.position);
                            intent.putExtra(ArticleDetailActivity.TRANSITION_NAME,getResources().getString(R.string.article_img_transition)+vh.position);
                        }
                        startActivity(intent,bundle);

                    }
                    else{

                        mCursor.moveToPosition(vh.position);
                        ArticleDetailFragmentDialog articleDetailFragmentDialog=ArticleDetailFragmentDialog.newInstance(mCursor.getString(ArticleLoader.Query.TITLE)
                        ,mCursor.getString(ArticleLoader.Query.BODY),mCursor.getString(ArticleLoader.Query.PHOTO_URL),
                                mCursor.getString(ArticleLoader.Query.AUTHOR),mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE),
                                getResources().getString(R.string.article_img_transition)+getItemId(vh.position));
                        getSupportFragmentManager().beginTransaction().add(articleDetailFragmentDialog,"ArticleDetailFragmentDialog").addToBackStack(null).commit();
                    }
                }
            });
            return vh;
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

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            holder.position=position;
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            Bundle bundle= null;

            //Log.d(ArticleListActivity.class.getSimpleName(),"Fuck Bind Transition Item :: "+getResources().getString(R.string.article_img_transition)+getItemId(holder.position));
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                holder.subtitleView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            } else {
                holder.subtitleView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate)
                        + "<br/>" + " by "
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            }
            ImageLoaderHelper.getInstance(mContext).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.THUMB_URL), new ImageLoader.ImageListener() {


                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                Palette p = Palette.generate(bitmap, 12);
                                holder.mutedColor=(p.getLightMutedColor(0xFF333333));
                                holder.bitmap=imageContainer.getBitmap();
                                holder.thumbnailView.setImageBitmap(imageContainer.getBitmap());
                                holder.bgView.setBackgroundColor(holder.mutedColor);
                                holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    holder.thumbnailView.setTransitionName(getResources().getString(R.string.article_img_transition)+getItemId(position));
                                }
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });
            holder.thumbnailView.setImageUrl(
                   mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());

        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;
        public View bgView;
        public int mutedColor;
        public Bitmap bitmap;
        public int position;
        public ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
            bgView=view.findViewById(R.id.textBg);
        }


    }
}
