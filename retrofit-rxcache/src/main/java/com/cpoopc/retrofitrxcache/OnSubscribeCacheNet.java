package com.cpoopc.retrofitrxcache;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Log;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * 缓存+网络请求
 * User: cpoopc
 * Date: 2016-01-18
 * Time: 10:19
 * Ver.: 0.1
 */
public class OnSubscribeCacheNet<T> implements Observable.OnSubscribe<T> {

    private final boolean debug = false;
    private final String TAG = "OnSubscribeCacheNet:";

    // 读取缓存
    private ObservableWrapper<T> cacheObservable;

    // 读取网络
    private ObservableWrapper<T> netObservable;

    // 网络读取完毕,缓存动作
    private Action1<T> storeCacheAction;

    private int stepIndex;
    private final int finalStepIndex;
    private AtomicInteger overCount = new AtomicInteger(0);
    private boolean sync = true;// true 同步,false:异步
    private CountDownLatch cacheLatch = new CountDownLatch(1);

    public OnSubscribeCacheNet(Observable<T> cacheObservable, Observable<T> netObservable, Action1<T> storeCacheAction) {
        if (cacheObservable != null) {
            this.cacheObservable = new ObservableWrapper<T>(1, cacheObservable);
        }
        this.netObservable = new ObservableWrapper<T>(2, netObservable);
        this.storeCacheAction = storeCacheAction;
        finalStepIndex = 2;
    }

    @Override
    public void call(Subscriber<? super T> subscriber) {
        if (cacheObservable != null) {
            if (sync) {
                cacheObservable.getObservable().unsafeSubscribe(new CacheNetObserver<T>(cacheObservable, subscriber, storeCacheAction));
            } else {
                cacheObservable.getObservable().subscribeOn(Schedulers.io()).unsafeSubscribe(new CacheNetObserver<T>(cacheObservable, subscriber, storeCacheAction));
            }
        }
        if (sync) {
            netObservable.getObservable().unsafeSubscribe(new CacheNetObserver<T>(netObservable, subscriber, storeCacheAction));
        } else {
            netObservable.getObservable().subscribeOn(Schedulers.newThread()).unsafeSubscribe(new CacheNetObserver<T>(netObservable, subscriber, storeCacheAction));
        }
    }

    class ObservableWrapper<T> {

        int index;
        Observable<T> observable;
        T data;

        public ObservableWrapper(int index, Observable<T> observable) {
            this.index = index;
            this.observable = observable;
        }

        public int getIndex() {
            return index;
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

    class CacheNetObserver<T> extends Subscriber<T> {

        ObservableWrapper<T> observableWrapper;
        Subscriber<? super T> subscriber;
        Action1<T> storeCacheAction;

        public CacheNetObserver(ObservableWrapper<T> observableWrapper, Subscriber<? super T> subscriber, Action1<T> storeCacheAction) {
            this.observableWrapper = observableWrapper;
            this.subscriber = subscriber;
            this.storeCacheAction = storeCacheAction;
        }

        @Override
        public void onCompleted() {
            if (finalStepIndex == observableWrapper.getIndex()) {
                if(debug) Log.i(TAG,"onCompleted stepIndex:" + observableWrapper.getIndex());
                try {
                    if (storeCacheAction != null) {
                        storeCacheAction.call(observableWrapper.getData());
                    }
                } catch (Exception e) {
                    onError(e);
                }
                overCount.addAndGet(1);
                if (subscriber != null && !subscriber.isUnsubscribed()) {
                    subscriber.onCompleted();
                }
            } else {
                if(debug) Log.i(TAG,"onCompleted stepIndex:" + observableWrapper.getIndex() + " still not completed");
                overCount.addAndGet(1);
            }

        }

        @Override
        public void onError(Throwable e) {
            if(debug) Log.e(TAG, "onError stepIndex:" + observableWrapper.getIndex());
            overCount.addAndGet(1);
            if (finalStepIndex == observableWrapper.getIndex()) {
                while (overCount.get() < finalStepIndex) {
                    if(debug) Log.e(TAG,"onError stepIndex:" + observableWrapper.getIndex() + " overCount.get() < finalStepIndex");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
                if (!sync) {
                    try {
                        cacheLatch.await();
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
//                    try {// FIXME: 2016/1/18 issue A 临时解决办法
//                        Thread.sleep(500);
//                    } catch (InterruptedException e1) {
//                        e1.printStackTrace();
//                    }
                }
                if (subscriber != null && !subscriber.isUnsubscribed()) {
                    subscriber.onError(e);
                }
            }
        }

        @Override
        public void onNext(T o) {
            if(debug) Log.i(TAG,"stepIndex :" + stepIndex + " observableWrapper.getIndex():" + observableWrapper.getIndex() + " o:"+o);
            observableWrapper.setData(o);
            if (stepIndex < observableWrapper.getIndex()) {
                stepIndex = observableWrapper.getIndex();
                if(debug) Log.i(TAG,"stepIndex :" + stepIndex + " observableWrapper.getIndex():" + observableWrapper.getIndex());
                logThread("");
                if(debug) Log.e(TAG, " check subscriber :" + subscriber + " isUnsubscribed:" + subscriber.isUnsubscribed());
                if (subscriber != null && !subscriber.isUnsubscribed()) {
                    subscriber.onNext(o);// FIXME: issue A:外面如果ObservableOn 其他线程,将会是异步操作,如果在实际的onNext调用之前,发生了onComplete或者onError,将会unsubscribe,导致onNext没有被调用
                }
            }
        }
    }

    public void logThread(String tag) {
        if(debug) Log.i(TAG, tag + " : " + Thread.currentThread().getName());
    }

}
