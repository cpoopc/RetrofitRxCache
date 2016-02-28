package com.cpoopc.rxcache;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.cpoopc.retrofitrxcache.RxCacheResult;
import com.cpoopc.rxcache.model.User;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends Activity {

    @Bind(R.id.avatarIv)
    ImageView mAvatarIv;
    @Bind(R.id.userNameTv)
    TextView mUserNameTv;
    @Bind(R.id.companyTv)
    TextView mCompanyTv;
    @Bind(R.id.locationTv)
    TextView mLocationTv;
    @Bind(R.id.createAtTv)
    TextView mCreateAtTv;
    @Bind(R.id.followersCountTv)
    TextView mFollowersCountTv;
    @Bind(R.id.starredCountTv)
    TextView mStarredCountTv;
    @Bind(R.id.followingCountTv)
    TextView mFollowingCountTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        getUserDetail();
        mUserNameTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("cp:", "onclick");
                getUserDetail();

            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
    }

    public void getUserDetail() {
        Observable<RxCacheResult<User>> userObservable = App.getInstance().getGithubService().userDetail("cpoopc");
        userObservable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<RxCacheResult<User>>() {
                    @Override
                    public void onCompleted() {
                        Log.e("cp:", "onCompleted");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("cp:", "onError:" + e.getMessage());
                    }

                    @Override
                    public void onNext(RxCacheResult<User> user) {
                        Log.e("cp:", user.isCache() + " onNext:" + user.getResultModel());
                        bindUser(user.getResultModel());
                    }

                });

    }


    private void bindUser(User user) {
        if (user != null) {
            Glide.with(this).load(user.getAvatar_url()).into(mAvatarIv);
            mUserNameTv.setText(user.getLogin());
            mCompanyTv.setText(user.getCompany());
            mLocationTv.setText(user.getLocation());
            mCreateAtTv.setText(user.getCreated_at());
            mFollowersCountTv.setText(String.valueOf(user.getFollowers()));
            mStarredCountTv.setText("?");
            mFollowingCountTv.setText(String.valueOf(user.getFollowing()));
        } else {
            mAvatarIv.setImageResource(R.mipmap.ic_launcher);
            mUserNameTv.setText("?");
            mCompanyTv.setText("?");
            mLocationTv.setText("?");
            mCreateAtTv.setText("?");
            mFollowersCountTv.setText("?");
            mStarredCountTv.setText("?");
            mFollowingCountTv.setText("?");
        }

    }
}
