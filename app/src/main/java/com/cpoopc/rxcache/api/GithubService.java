package com.cpoopc.rxcache.api;


import com.cpoopc.retrofitrxcache.RxCacheResult;
import com.cpoopc.rxcache.model.User;

import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;

/**
 * User: cpoopc
 * Date: 2016-01-18
 * Time: 13:47
 * Ver.: 0.1
 */
public interface GithubService {

    @GET("users/{username}")
    Observable<RxCacheResult<User>> userDetail(@Path("username") String userName);

}
