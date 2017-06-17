package com.example.xyzreader.ui;


import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.ShareCompat;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import static android.content.ContentValues.TAG;

/**
 * A simple {@link Fragment} subclass.
 */
public class ArticleDetailFragmentDialog extends DialogFragment {
    private static final String BODY_KEY ="BODY_KEY" ;
    static private final String TITLE_KEY="TITLE_KEY";
    static private final String IMG_KEY="IMG_KEY";
    static private final String BY_KEY="BY_KEY";
    static private final String DATE_KEY="DATA_KEY";
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);
    String mTitle,mBody,mImgUrl,mBy;
    private View mRootView;
    CustomAspectImage mPhotoView;
    private String mDate;

    public ArticleDetailFragmentDialog() {
        // Required empty public constructor
    }

    public static ArticleDetailFragmentDialog newInstance(String Title,String Body,String ImgUrl,String By,String date){
        Bundle bundle=new Bundle();
        bundle.putString(BODY_KEY,Body);
        bundle.putString(TITLE_KEY,Title);
        bundle.putString(IMG_KEY,ImgUrl);
        bundle.putString(BY_KEY,By);
        bundle.putString(DATE_KEY,date);
        ArticleDetailFragmentDialog fragment=new ArticleDetailFragmentDialog();
        fragment.setArguments(bundle);
        return fragment;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if(getArguments()!=null){
            mBody=getArguments().getString(BODY_KEY);
            mTitle=getArguments().getString(TITLE_KEY);
            mBy=getArguments().getString(BY_KEY);
            mImgUrl=getArguments().getString(IMG_KEY);
            mDate=getArguments().getString(DATE_KEY);
            mRootView= inflater.inflate(R.layout.fragment_article_detail_fragment_dialog, container, false);
            setMetaData();
        }
        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });
        mRootView.findViewById(R.id.close_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              getActivity().onBackPressed();
            }
        });
        return  mRootView;
    }

    private void setMetaData() {
        TextView title=(TextView) mRootView.findViewById(R.id.title);
        TextView byLine=(TextView) mRootView.findViewById(R.id.byLine);
        mPhotoView=(CustomAspectImage) mRootView.findViewById(R.id.photo);
        mPhotoView.setAspectRatio(3,4);
        title.setText(mTitle);
        formatSubTitle();
        byLine.setText(mBy);
        ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                .get(mImgUrl, new ImageLoader.ImageListener() {
                    @Override
                    public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                        Bitmap bitmap = imageContainer.getBitmap();
                        if (bitmap != null) {
                            Palette p = Palette.generate(bitmap, 12);
                            mPhotoView.setImageBitmap(imageContainer.getBitmap());
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
    private Date parsePublishedDate() {
        try {
            String date = mDate;
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }
    private void formatSubTitle(){
        Date publishedDate = parsePublishedDate();
        if (!publishedDate.before(START_OF_EPOCH.getTime())) {
            mBy= Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            publishedDate.getTime(),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#ffffff'>"
                            + mBy
                            + "</font>").toString();

        } else {
            // If date is before 1902, just show the string
            mBy=(Html.fromHtml(
                    outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                            + mBy
                            + "</font>")).toString();

        }
    }
}
