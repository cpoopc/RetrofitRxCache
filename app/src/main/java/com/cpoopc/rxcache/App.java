package com.cpoopc.rxcache;

import android.app.Application;

import com.cpoopc.rxcache.api.GithubService;

import com.cpoopc.retrofitrxcache.BasicCache;
import retrofit2.Retrofit;
import com.cpoopc.retrofitrxcache.RxCacheCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * User: cpoopc
 * Date: 2016-01-18
 * Time: 13:53
 * Ver.: 0.1
 */
public class App extends Application {

    private static App instance;
    private Retrofit mRetrofit;
    private GithubService mGithubService;

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        mRetrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com")
                .addCallAdapterFactory(RxCacheCallAdapterFactory.create(BasicCache.fromCtx(this), false))// 分开读取缓存,网络
//                .addCallAdapterFactory(RxCacheCallAdapterFactory.create(BasicCache.fromCtx(this), true))// 先读取缓存,再获取网络
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public GithubService getGithubService() {
        if (mGithubService == null) {
            mGithubService = mRetrofit.create(GithubService.class);
        }
        return mGithubService;
    }
}
