package cn.jack.okhttpdemo.interceptor;

import java.io.IOException;

import cn.jack.okhttp.okhttp3.CacheControl;
import cn.jack.okhttp.okhttp3.Interceptor;
import cn.jack.okhttp.okhttp3.Response;

/**
 * Created by cyg on 2019-09-09.
 */
public class ResponseCacheInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request()).newBuilder()
                .removeHeader("Pragma") //移除影响
                .removeHeader("Cache-Control") //移除影响
                .addHeader("Cache-Control", CacheControl.FORCE_CACHE.toString()).build();
        return response;
    }
}
