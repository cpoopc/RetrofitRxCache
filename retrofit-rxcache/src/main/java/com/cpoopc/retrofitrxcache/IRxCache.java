package com.cpoopc.retrofitrxcache;


import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Buffer;

/**
 * User: cpoopc
 * Date: 2016-01-18
 * Time: 10:19
 * Ver.: 0.1
 */
public interface IRxCache {

    void addInCache(Request request, Buffer buffer);
    ResponseBody getFromCache(Request request);

}
