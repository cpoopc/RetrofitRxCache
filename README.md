# RetrofitRxCache
[![](https://jitpack.io/v/cpoopc/RetrofitRxCache.svg)](https://jitpack.io/#cpoopc/RetrofitRxCache)  
trans normal Observable into Observable with cache,support retrofit

retrofit是个网络请求利器,帮助我们快速编写http请求.而且retrofit支持 RxJava,配合Rxjava实在十分强大.
retrofit2.0之后,结构更加清晰,定制性更强了.此repo就是基于retrofit 2.0,为Observable提供了Cache支持.  
接口方法加上@UseCache,即可实现Cache.   

### cache的一些表现:   
1. Cache获取与网络获取是异步进行的.   
2. 若网络获取比Cache获取速度快,Cache不会发射出去.   
3. 网络获取完毕后,异步进行保存Cache操作,不阻塞结果的发射.   
