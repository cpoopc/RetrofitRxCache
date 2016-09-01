# RetrofitRxCache
[![](https://jitpack.io/v/cpoopc/RetrofitRxCache.svg)](https://jitpack.io/#cpoopc/RetrofitRxCache)  
###update node:  
2016/9/1:
兼容升级到retrofit2.1.0
2016/5/15:
RxCacheCallAdapterFactory create(IRxCache cachingSystem, boolean sync),sync指定同步获取缓存,网络还是异步获取
2016/2/28:
1. 更新到retrofit2-beta4版本,对应retrofit进行了点改动  
2. 由注解方式(@UseRxCache)指定使用cache改为返回类型指定,返回类型为Observable<RxCacheResult<T>>即可启用cache.  
且可以根据RxCacheResult.isCache()判断是缓存还是网络返回结果.  

trans normal Observable into Observable with cache,support retrofit

retrofit是个网络请求利器,帮助我们快速编写http请求.而且retrofit支持 RxJava,配合Rxjava实在十分强大.
retrofit2.0之后,结构更加清晰,定制性更强了.此repo就是基于retrofit 2.0,为Observable提供了Cache支持.  
接口方法加上@UseCache,即可实现Cache.   

### cache的一些表现:   
1. Cache获取与网络获取是异步进行的.   
2. 若网络获取比Cache获取速度快,Cache不会发射出去.   
3. 网络获取完毕后,异步进行保存Cache操作,不阻塞结果的发射.   

### 引用方法

此lib加入了JitPack
引用方法
根目录下的build.gradle  
```
allprojects {
		repositories {
			...
			maven { url "https://jitpack.io" }
		}
	}
```
项目中  
```

	dependencies {
	        compile 'com.github.cpoopc:RetrofitRxCache:v1.0.2'
	}


```

### Usage

```

Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com")
                .addCallAdapterFactory(RxCacheCallAdapterFactory.create(BasicCache.fromCtx(this), false))// 分开读取缓存,网络
//                .addCallAdapterFactory(RxCacheCallAdapterFactory.create(BasicCache.fromCtx(this), true))// 先读取缓存,再获取网络
                .addConverterFactory(GsonConverterFactory.create())
                .build();

public interface GithubService {

    @GET("users/{username}")
    Observable<RxCacheResult<User> userDetail(@Path("username") String userName); // Observable泛型为RxCacheResult<T>,即可使用cache

}

```
