package com.cpoopc.retrofitrxcache;

import rx.Observable;
import rx.functions.Action1;

/**
 * @author cpoopc
 * @date 2016/05/15
 * @time 11:36
 * @description
 */
public class CacheNetOnSubscribeFactory {

    private boolean syncMode;

    public CacheNetOnSubscribeFactory() {
        this.syncMode = true;
    }

    public CacheNetOnSubscribeFactory(boolean syncMode) {
        this.syncMode = syncMode;
    }

    public <T> Observable.OnSubscribe<T> create(Observable<T> cacheObservable, Observable<T> netObservable, Action1<T> storeCacheAction) {
        if (syncMode) {
            return new SyncOnSubscribeCacheNet<>(cacheObservable, netObservable, storeCacheAction);
        } else {
            return new AsyncOnSubscribeCacheNet<>(cacheObservable, netObservable, storeCacheAction);
        }
    }


}
