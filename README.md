## OkHttp 源码解析

```
OkHttpClient client = new OkHttpClient();
Request request = new Request.Builder()
        .url("xxx.xxx.xxx")
        .build();
Call call = client.newCall(request);
call.enqueue(new Callback() {
    @Override
    public void onFailure(Call call, IOException e) {
    }
    @Override
    public void onResponse(Call call, Response response) {
    }
});
```

可以将整个OKHttp请求分解成三个模块来进行：


### OkHttpClient

根据字面意思理解，请求的客户端，我理解他是请求的 “需求”，`OkHttpClient` 用来配置请求的硬性配置（比较底层的一些东西），比如请求的协议、线程池、缓存、拦截器等等，这些是一个请求必须要有的基本东西

```
this.dispatcher = builder.dispatcher;  // 所有请求任务的调度器，负责调度请求的执行顺序
this.proxy = builder.proxy;  //代理
this.protocols = builder.protocols;  //协议
this.interceptors = Util.immutableList(builder.interceptors);  //拦截器
this.eventListenerFactory = builder.eventListenerFactory;
this.cache = builder.cache;  //缓存
this.socketFactory = builder.socketFactory;
connectionPool = new ConnectionPool();
retryOnConnectionFailure = true;
connectTimeout = 10_000;
readTimeout = 10_000;
writeTimeout = 10_000;
```

### Request

根据字面意思理解，一个请求，我理解他为 “配置”，`Request` 用来配置请求的软配置（比较上层的一些东西，可理解为肉眼可见），比如请求的地址、参数、头部信息、请求方式等等信息。

```
this.url = builder.url;
this.method = builder.method;
this.headers = builder.headers.build();
this.body = builder.body;
this.tag = builder.tag != null ? builder.tag : this;
```

### Call

我理解他为 “执行”，`Call` 用来真正的发起请求，用 “配置” 完成 “需求方” 的需求。

### 中心，重点，精髓

拦截器是OKHttp网络请求框架中的精髓所在，OKHttp提供自定义拦截器，当然整个网络请求部分都是通过拦截器来完成的。

```
Response getResponseWithInterceptorChain() throws IOException {
    // Build a full stack of interceptors.
    List<Interceptor> interceptors = new ArrayList<>();
    // 添加我们自定义的拦截器
    interceptors.addAll(client.interceptors());
    // 处理请求错误和重定向的拦截器
    interceptors.add(retryAndFollowUpInterceptor);
    // 桥拦截器，将网络层与应用层联系起来，添加一些请求头信息
    interceptors.add(new BridgeInterceptor(client.cookieJar()));
    // 缓存拦截器，判断是否有缓存
    interceptors.add(new CacheInterceptor(client.internalCache()));
    // 连接拦截器GET / POST，RealConnection连接，三次握手过程
    interceptors.add(new ConnectInterceptor(client));
    if (!forWebSocket) {
        interceptors.addAll(client.networkInterceptors());
    }
    // 最终发起请求的拦截器
    interceptors.add(new CallServerInterceptor(forWebSocket));
    Interceptor.Chain chain = new RealInterceptorChain(interceptors, null, null, null, 0,
            originalRequest, this, eventListener, client.connectTimeoutMillis(),
            client.readTimeoutMillis(), client.writeTimeoutMillis());
    return chain.proceed(originalRequest);
}
```

### HTTP1.0，HTTP1.1，HTTP2.0

1.0协议浏览器与服务器之间只能保持短暂连接，1.1协议浏览器与服务器之间可以保持长久连接，但是1.x协议浏览器客户端同一时间对同一域名下的请求数量会有限制，超过请求数就会阻塞，而2.0不会有这样的问题

## Retrofit

```
OkHttpClient client = new OkHttpClient();
Retrofit retrofit = new Retrofit.Builder()
        .client(client)
        .baseUrl("http://xxx.xxx.xxx/")
        .addConverterFactory(GsonConverterFactory.create())
        .build();
ApiService service = retrofit.create(ApiService.class);
service.getUserInfo().enqueue(new Callback<UserInfo>() {
    @Override
    public void onResponse(Call<UserInfo> call, Response<UserInfo> response) {
        
    }
    @Override
    public void onFailure(Call<UserInfo> call, Throwable t) {
        
    }
});
```

### 动态代理模式

通过动态代理模式生成网络请求接口的实例，拿到网络请求接口实例中的所有注解，当调用接口实例进行网络请求的时候，动态代理会进行拦截，通过自身的invoke方法处理某一个网络请求（通过OKHttpCall来处理）

### CallAdapter.Factory

网络请求执行器工厂，目的是生成网络请求执行器（默认OKHttpCall，可以自定义RxJava/Java8/Guava）

### Converter.Factory

数据转换器工厂，目的是生成数据转换器（一般我们都用GsonConverterFactory，你也可以自定义，比如fastjson）
