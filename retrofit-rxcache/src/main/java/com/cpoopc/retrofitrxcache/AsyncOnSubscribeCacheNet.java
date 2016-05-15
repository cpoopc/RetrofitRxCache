package com.cpoopc.retrofitrxcache;


import android.util.Log;

import java.util.concurrent.CountDownLatch;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * 缓存+网络请求
 * User: cpoopc
 * Date: 2016-01-18
 * Time: 10:19
 * Ver.: 0.1
 */
public class AsyncOnSubscribeCacheNet<T> implements Observable.OnSubscribe<T> {

    private final boolean debug = true;
    private final String TAG = "OnSubscribeCacheNet:";

    // 读取缓存
    protected ObservableWrapper<T> cacheObservable;

    // 读取网络
    protected ObservableWrapper<T> netObservable;

    // 网络读取完毕,缓存动作
    protected Action1<T> storeCacheAction;

    protected CountDownLatch cacheLatch = new CountDownLatch(1);
    protected CountDownLatch netLatch = new CountDownLatch(1);

    public AsyncOnSubscribeCacheNet(Observable<T> cacheObservable, Observable<T> netObservable, Action1<T> storeCacheAction) {
        this.cacheObservable = new ObservableWrapper<T>(cacheObservable);
        this.netObservable = new ObservableWrapper<T>(netObservable);
        this.storeCacheAction = storeCacheAction;
    }

    @Override
    public void call(Subscriber<? super T> subscriber) {
        cacheObservable.getObservable().subscribeOn(Schedulers.io()).unsafeSubscribe(new CacheObserver<T>(cacheObservable, subscriber, storeCacheAction));
        netObservable.getObservable().subscribeOn(Schedulers.io()).unsafeSubscribe(new NetObserver<T>(netObservable, subscriber, storeCacheAction));
    }

    class ObservableWrapper<T> {

        Observable<T> observable;
        T data;

        public ObservableWrapper(Observable<T> observable) {
            this.observable = observable;
        }

        public Observable<T> getObservable() {
            return observable;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }

    class NetObserver<T> extends Subscriber<T> {

        ObservableWrapper<T> observableWrapper;
        Subscriber<? super T> subscriber;
        Action1<T> storeCacheAction;

        public NetObserver(ObservableWrapper<T> observableWrapper, Subscriber<? super T> subscriber, Action1<T> storeCacheAction) {
            this.observableWrapper = observableWrapper;
            this.subscriber = subscriber;
            this.storeCacheAction = storeCacheAction;
        }

        @Override
        public void onCompleted() {
            if (debug) Log.i(TAG, "net onCompleted ");
            try {
                if (storeCacheAction != null) {
                    logThread("保存到本地缓存 ");
                    storeCacheAction.call(observableWrapper.getData());
                }
            } catch (Exception e) {
                onError(e);
            }
            if (subscriber != null && !subscriber.isUnsubscribed()) {
                subscriber.onCompleted();
            }
            netLatch.countDown();
        }

        @Override
        public void onError(Throwable e) {
            if (debug) Log.e(TAG, "net onError ");
            try {
                if (debug) Log.e(TAG, "net onError await if cache not completed.");
                cacheLatch.await();
                if (debug) Log.e(TAG, "net onError await over.");
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            if (subscriber != null && !subscriber.isUnsubscribed()) {
                subscriber.onError(e);
            }
        }

        @Override
        public void onNext(T o) {
            if (debug) Log.i(TAG, "net onNext o:" + o);
            observableWrapper.setData(o);
            logThread("");
            if (debug) Log.e(TAG, " check subscriber :" + subscriber + " isUnsubscribed:" + subscriber.isUnsubscribed());
            if (subscriber != null && !subscriber.isUnsubscribed()) {
                subscriber.onNext(o);// FIXME: issue A:外面如果ObservableOn 其他线程,将会是异步操作,如果在实际的onNext调用之前,发生了onComplete或者onError,将会unsubscribe,导致onNext没有被调用
            }
        }
    }

    class CacheObserver<T> extends Subscriber<T> {

        ObservableWrapper<T> observableWrapper;
        Subscriber<? super T> subscriber;
        Action1<T> storeCacheAction;

        public CacheObserver(ObservableWrapper<T> observableWrapper, Subscriber<? super T> subscriber, Action1<T> storeCacheAction) {
            this.observableWrapper = observableWrapper;
            this.subscriber = subscriber;
            this.storeCacheAction = storeCacheAction;
        }

        @Override
        public void onCompleted() {
            if (debug) Log.i(TAG, "cache onCompleted");
            cacheLatch.countDown();
        }

        @Override
        public void onError(Throwable e) {
            if (debug) Log.e(TAG, "cache onError");
            Log.e(TAG, "read cache error:" + e.getMessage());
            if (debug) e.printStackTrace();
            cacheLatch.countDown();
        }

        @Override
        public void onNext(T o) {
            if (debug) Log.i(TAG, "cache onNext o:" + o);
            observableWrapper.setData(o);
            logThread("");
            if (netLatch.getCount() > 0) {
                if (debug) Log.e(TAG, " check subscriber :" + subscriber + " isUnsubscribed:" + subscriber.isUnsubscribed());
                if (subscriber != null && !subscriber.isUnsubscribed()) {
                    subscriber.onNext(o);// FIXME: issue A:外面如果ObservableOn 其他线程,将会是异步操作,如果在实际的onNext调用之前,发生了onComplete或者onError,将会unsubscribe,导致onNext没有被调用
                }
            } else {
                if (debug) Log.e(TAG, "net result had been load,so cache is not need to load");
            }
        }
    }

    public void logThread(String tag) {
        if (debug) Log.i(TAG, tag + " : " + Thread.currentThread().getName());
    }

}
