package com.cpoopc.retrofitrxcache;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;

/**
 * User: cpoopc
 * Date: 2016/05/15
 * Time: 12:58
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
