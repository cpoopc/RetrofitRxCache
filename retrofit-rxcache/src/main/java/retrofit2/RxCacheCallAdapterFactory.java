package retrofit2;

import com.cpoopc.retrofitrxcache.OnSubscribeCacheNet;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import rx.Observable;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

/**
 * @author cpoopc
 * @date 2016/1/16
 * @time 20:33
 * @description
 */
public class RxCacheCallAdapterFactory extends CallAdapter.Factory {
    private final IRxCache cachingSystem;

    public RxCacheCallAdapterFactory(IRxCache cachingSystem) {
        this.cachingSystem = cachingSystem;
    }

    public static RxCacheCallAdapterFactory create(IRxCache cachingSystem) {
        return new RxCacheCallAdapterFactory(cachingSystem);
    }

    @Override
    public CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        Class<?> rawType = getRawType(returnType);
        if (rawType != Observable.class) {
            return null;
        }
        if (!(returnType instanceof ParameterizedType)) {
            String name = "Observable";
            throw new IllegalStateException(name + " return type must be parameterized"
                    + " as " + name + "<Foo> or " + name + "<? extends Foo>");
        }
        for (Annotation annotation : annotations) {
            if (annotation instanceof UseRxCache) {
//                Type observableType = Utils.getSingleParameterUpperBound((ParameterizedType) returnType);
                Type observableType = getParameterUpperBound(0, (ParameterizedType) returnType);
                // TODO: 2016/2/28 添加Response类支持
                return new RxCacheCallAdapter(observableType,annotations,retrofit,cachingSystem);
            }

        }
        return null;
    }

    /**
     * 将Call转换成带有缓存功能的Observable<T>
     */
    static final class RxCacheCallAdapter implements CallAdapter<Observable<?>> {
        private final Type responseType;
        private final Annotation[] annotations;
        private final Retrofit retrofit;
        private final IRxCache cachingSystem;

        public RxCacheCallAdapter(Type responseType, Annotation[] annotations, Retrofit retrofit,
                                  IRxCache cachingSystem) {
            this.responseType = responseType;
            this.annotations = annotations;
            this.retrofit = retrofit;
            this.cachingSystem = cachingSystem;
        }


        /***
         * Inspects an OkHttp-powered Call<T> and builds a Request
         * * @return A valid Request (that contains query parameters, right method and endpoint)
         */
        private Request buildRequestFromCall(Call call){
            try {
                Field argsField = call.getClass().getDeclaredField("args");
                argsField.setAccessible(true);
                Object[] args = (Object[]) argsField.get(call);

                Field requestFactoryField = call.getClass().getDeclaredField("requestFactory");
                requestFactoryField.setAccessible(true);
                Object requestFactory = requestFactoryField.get(call);
                Method createMethod = requestFactory.getClass().getDeclaredMethod("create", Object[].class);
                createMethod.setAccessible(true);
                return (Request) createMethod.invoke(requestFactory, new Object[]{args});
            }catch(Exception exc){
                return null;
            }
        }

        @Override public Type responseType() {
            return responseType;
        }

        @Override public <R> Observable<R> adapt(Call<R> call) {
            final Request request = buildRequestFromCall(call);
            Observable<R> mCacheResultObservable = Observable.create(new Observable.OnSubscribe<R>() {
                @Override
                public void call(Subscriber<? super R> subscriber) {
                    // read cache
                    Converter<ResponseBody, R> responseConverter = getResponseConverter(retrofit, responseType, annotations);
                    R serverResult = getFromCache(request, responseConverter, cachingSystem);
                    if (subscriber.isUnsubscribed()) return;

                    if (serverResult != null) {
                        subscriber.onNext(serverResult);
                    }
                    subscriber.onCompleted();
                }
            });
            Action1<R> mCacheAction = new Action1<R>() {
                @Override
                public void call(R serverResult) {
                    // store cache action
                    if (serverResult != null) {
                        Converter<R, RequestBody> requestConverter = getRequestConverter(retrofit, responseType, annotations);
                        addToCache(request, serverResult, requestConverter, cachingSystem);
                    }
                }
            };

            Observable<R> serverObservable = Observable.create(new CallOnSubscribe<>(call)) //
                    .flatMap(new Func1<Response<R>, Observable<R>>() {
                        @Override
                        public Observable<R> call(Response<R> response) {
                            if (response.isSuccess()) {
                                return Observable.just(response.body());
                            }
                            return Observable.error(new RxCacheHttpException(response));
                        }
                    });
            return Observable.create(new OnSubscribeCacheNet<R>(mCacheResultObservable, serverObservable, mCacheAction));
        }

        public static <T> T getFromCache(Request request, Converter<ResponseBody, T> converter, IRxCache cachingSystem) {
            try {
                return converter.convert(cachingSystem.getFromCache(request));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        public static <T> void addToCache(Request request, T data, Converter<T, RequestBody> converter, IRxCache cachingSystem) {
            try {
                Buffer buffer = new Buffer();
                RequestBody requestBody = converter.convert(data);
                requestBody.writeTo(buffer);
                cachingSystem.addInCache(request, buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    static final class CallOnSubscribe<T> implements Observable.OnSubscribe<Response<T>> {
        private final Call<T> originalCall;

        private CallOnSubscribe(Call<T> originalCall) {
            this.originalCall = originalCall;
        }

        @Override public void call(final Subscriber<? super Response<T>> subscriber) {
            // Since Call is a one-shot type, clone it for each new subscriber.
            final Call<T> call = originalCall.clone();

            // Attempt to cancel the call if it is still in-flight on unsubscription.
            subscriber.add(Subscriptions.create(new Action0() {
                @Override public void call() {
                    call.cancel();
                }
            }));

            if (subscriber.isUnsubscribed()) {
                return;
            }

            try {
                Response<T> response = call.execute();
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(response);
                }
            } catch (Throwable t) {
                Exceptions.throwIfFatal(t);
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onError(t);
                }
                return;
            }

            if (!subscriber.isUnsubscribed()) {
                subscriber.onCompleted();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Converter<ResponseBody, T> getResponseConverter(Retrofit retrofit, Type dataType, Annotation[] annotations) {
        return retrofit.responseBodyConverter(dataType, annotations);
    }

    @SuppressWarnings("unchecked")
    public static <T> Converter<T, RequestBody> getRequestConverter(Retrofit retrofit, Type dataType, Annotation[] annotations) {
        // TODO gson beta4版本中paramsAnnotations与获取converter没有什么关系
        // 此处获取RequestBodyConverter,是为了将model转成Buffer,以便写入cache
        return retrofit.requestBodyConverter(dataType, new Annotation[0], annotations);
    }
}
