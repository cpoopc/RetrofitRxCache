package com.cpoopc.retrofitrxcache;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;

/**
 * @author cpoopc
 * @date 2016/05/15
 * @time 12:58
 * @description
 */
public class SyncOnSubscribeCacheNet<T> extends AsyncOnSubscribeCacheNet<T> {
    public SyncOnSubscribeCacheNet(Observable<T> cacheObservable, Observable<T> netObservable, Action1<T> storeCacheAction) {
        super(cacheObservable, netObservable, storeCacheAction);
    }

    @Override
    public void call(Subscriber<? super T> subscriber) {
        cacheObservable.getObservable().unsafeSubscribe(new CacheObserver<T>(cacheObservable, subscriber, storeCacheAction));
        netObservable.getObservable().unsafeSubscribe(new NetObserver<T>(netObservable, subscriber, storeCacheAction));
    }
}
