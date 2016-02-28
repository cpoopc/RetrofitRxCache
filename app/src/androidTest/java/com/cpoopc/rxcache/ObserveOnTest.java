package com.cpoopc.rxcache;

import android.test.AndroidTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.security.auth.Subject;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;


/**
 * @author cpoopc
 * @date 2016/1/18
 * @time 18:51
 * @description
 */
public class ObserveOnTest extends AndroidTestCase{

    private static class MokeException extends RuntimeException {
        public MokeException(String detailMessage) {
            super(detailMessage);
        }
    }

    public void testObserveOnAndroidMainThread() throws InterruptedException {
        final CountDownLatch completedLatch = new CountDownLatch(1);
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        final int count = 20;
        Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                subscriber.onNext(count);
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//
//                subscriber.onError(new MokeException("moke exception"));
            }
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.newThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        System.out.println("onError:" + atomicInteger.get());
                        android.util.Log.e("cc:", "onError:" + atomicInteger.get());
                        assertEquals(atomicInteger.get(), count);
                        completedLatch.countDown();
                    }

                    @Override
                    public void onNext(Integer o) {
                        android.util.Log.e("cc:", "onNext:" + o);
                        System.out.println("onNext:" + o);
                        atomicInteger.getAndSet(o);
                        completedLatch.countDown();
                    }
                });
        completedLatch.await();
    }
}
