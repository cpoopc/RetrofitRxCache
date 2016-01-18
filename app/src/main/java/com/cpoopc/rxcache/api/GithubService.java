package com.cpoopc.rxcache.api;


import com.cpoopc.rxcache.model.User;

import retrofit.UseRxCache;
import retrofit.http.GET;
import retrofit.http.Path;
import rx.Observable;

/**
 * User: cpoopc
 * Date: 2016-01-18
 * Time: 13:47
 * Ver.: 0.1
 */
public interface GithubService {

    @UseRxCache
    @GET("users/{username}")
    Observable<User> userDetail(@Path("username") String userName);

}
