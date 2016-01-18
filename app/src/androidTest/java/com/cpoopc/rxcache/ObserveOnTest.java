package com.cpoopc.rxcache;

import android.test.AndroidTestCase;

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
    public void testObserveOnAndroidMainThread() {
        final PublishSubject<Integer> subject = PublishSubject.create();
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        final int count = 20;
        subject
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        android.util.Log.e("cc:", "onError:" + atomicInteger.get());
                        assertEquals(atomicInteger.get(), count);
                    }

                    @Override
                    public void onNext(Integer o) {
                        android.util.Log.e("cc:", "onNext:" + o);
                        atomicInteger.getAndSet(o);
                    }
                });
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < count; i++) {
                    subject.onNext(i);
                }
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                subject.onError(new MokeException("moke exception"));
            }
        }).start();
        while (true) {

        }
    }
}
