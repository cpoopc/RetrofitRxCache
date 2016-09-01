package com.cpoopc.retrofitrxcache;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import rx.Observable;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

/**
 * User: cpoopc
 * Date: 2016/1/16
 * Time: 20:33
 */
public class RxCacheCallAdapterFactory extends CallAdapter.Factory {
    private final IRxCache cachingSystem;
    private final CacheNetOnSubscribeFactory cacheNetOnSubscribeFactory;

    public RxCacheCallAdapterFactory(IRxCache cachingSystem) {
        this.cachingSystem = cachingSystem;
        this.cacheNetOnSubscribeFactory = new CacheNetOnSubscribeFactory();
    }

    public RxCacheCallAdapterFactory(IRxCache cachingSystem, boolean sync) {
        this.cachingSystem = cachingSystem;
        this.cacheNetOnSubscribeFactory = new CacheNetOnSubscribeFactory(sync);
    }

    public static RxCacheCallAdapterFactory create(IRxCache cachingSystem) {
        return new RxCacheCallAdapterFactory(cachingSystem);
    }

    public static RxCacheCallAdapterFactory create(IRxCache cachingSystem, boolean sync) {
        return new RxCacheCallAdapterFactory(cachingSystem, sync);
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

        Type observableType = getParameterUpperBound(0, (ParameterizedType) returnType);
        Class<?> genRawType = getRawType(observableType);
        if (genRawType == RxCacheResult.class) {
            Type actualType = ((ParameterizedType) observableType).getActualTypeArguments()[0];
            android.util.Log.d("cp:RxCache", "RxCacheResult<T>  T:" + actualType);
            // TODO: 2016/2/28 添加Response类支持
            return new RxCacheCallAdapter(actualType, annotations, retrofit, cachingSystem);
        }
        return null;
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

    /**
     * 将Call转换成带有缓存功能的Observable<T>
     */
    final class RxCacheCallAdapter implements CallAdapter<Observable> {
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
        private Request buildRequestFromCall(Call call) {
            Request request = null;
            try {
                // 增加了call.request()方法:https://github.com/square/retrofit/commit/b3ea768567e9e1fb1ba987bea021dbc0ead4acd4
                request = call.request();
            }catch (NoSuchMethodError e) {
                try {
                    // 旧版本需要通过反射获取Request
                    Field argsField = call.getClass().getDeclaredField("args");
                    argsField.setAccessible(true);
                    Object[] args = (Object[]) argsField.get(call);

                    Field requestFactoryField = call.getClass().getDeclaredField("requestFactory");
                    requestFactoryField.setAccessible(true);
                    Object requestFactory = requestFactoryField.get(call);
                    Method createMethod = requestFactory.getClass().getDeclaredMethod("create", Object[].class);
                    createMethod.setAccessible(true);
                    request = (Request) createMethod.invoke(requestFactory, new Object[]{args});
                } catch (Exception exc) {
                    return null;
                }
            }
            return request;
        }

        @Override
        public Type responseType() {
            return responseType;
        }

        @Override
        public <RESULT> Observable<RxCacheResult<RESULT>> adapt(Call<RESULT> call) {
            final Request request = buildRequestFromCall(call);
            Observable<RxCacheResult<RESULT>> mCacheResultObservable = Observable.create(new Observable.OnSubscribe<RxCacheResult<RESULT>>() {
                @Override
                public void call(Subscriber<? super RxCacheResult<RESULT>> subscriber) {
                    // read cache
                    Converter<ResponseBody, RESULT> responseConverter = getResponseConverter(retrofit, responseType, annotations);
                    RESULT serverResult = getFromCache(request, responseConverter, cachingSystem);
                    if (subscriber.isUnsubscribed()) return;
                    if (serverResult != null) {
                        subscriber.onNext(new RxCacheResult<RESULT>(true, serverResult));
                    }
                    subscriber.onCompleted();
                }
            });
            Action1<RxCacheResult<RESULT>> mCacheAction = new Action1<RxCacheResult<RESULT>>() {
                @Override
                public void call(RxCacheResult<RESULT> serverResult) {
                    // store cache action
                    if (serverResult != null) {
                        RESULT resultModel = serverResult.getResultModel();
                        Converter<RESULT, RequestBody> requestConverter = getRequestConverter(retrofit, responseType, annotations);
                        addToCache(request, resultModel, requestConverter, cachingSystem);
                    }
                }
            };

            Observable<RxCacheResult<RESULT>> serverObservable = Observable.create(new CallOnSubscribe<>(call)) //
                    .flatMap(new Func1<Response<RESULT>, Observable<RxCacheResult<RESULT>>>() {
                        @Override
                        public Observable<RxCacheResult<RESULT>> call(Response<RESULT> response) {
                            boolean success = false;
                            try {
                                success = response.isSuccessful();
                            }catch (NoSuchMethodError e2) {
                                try {
                                    Method isSuccess = response.getClass().getDeclaredMethod("isSuccess");
                                    success = (boolean) isSuccess.invoke(response);
                                } catch (NoSuchMethodException e) {
                                    e.printStackTrace();
                                } catch (InvocationTargetException e) {
                                    e.printStackTrace();
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (success) {
                                return Observable.just(new RxCacheResult<RESULT>(false, response.body()));
                            }
                            return Observable.error(new RxCacheHttpException(response));
                        }
                    });
            return Observable.create(cacheNetOnSubscribeFactory.create(mCacheResultObservable, serverObservable, mCacheAction));
        }

    }

    final class CallOnSubscribe<T> implements Observable.OnSubscribe<Response<T>> {
        private final Call<T> originalCall;

        private CallOnSubscribe(Call<T> originalCall) {
            this.originalCall = originalCall;
        }

        @Override
        public void call(final Subscriber<? super Response<T>> subscriber) {
            // Since Call is a one-shot type, clone it for each new subscriber.
            final Call<T> call = originalCall.clone();

            // Attempt to cancel the call if it is still in-flight on unsubscription.
            subscriber.add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
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

    // This method is copyright 2008 Google Inc. and is taken from Gson under the Apache 2.0 license.
    public static Class<?> getRawType(Type type) {
        if (type instanceof Class<?>) {
            // Type is a normal class.
            return (Class<?>) type;

        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            // I'm not exactly sure why getRawType() returns Type instead of Class. Neal isn't either but
            // suspects some pathological case related to nested classes exists.
            Type rawType = parameterizedType.getRawType();
            if (!(rawType instanceof Class)) throw new IllegalArgumentException();
            return (Class<?>) rawType;

        } else if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            return Array.newInstance(getRawType(componentType), 0).getClass();

        } else if (type instanceof TypeVariable) {
            // We could use the variable's bounds, but that won't work if there are multiple. Having a raw
            // type that's more general than necessary is okay.
            return Object.class;

        } else if (type instanceof WildcardType) {
            return getRawType(((WildcardType) type).getUpperBounds()[0]);

        } else {
            String className = type == null ? "null" : type.getClass().getName();
            throw new IllegalArgumentException("Expected a Class, ParameterizedType, or "
                    + "GenericArrayType, but <" + type + "> is of type " + className);
        }
    }

}
