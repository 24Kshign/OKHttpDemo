/*
 * Copyright (C) 2014 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.jack.okhttp.okhttp3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.jack.okhttp.okhttp3.internal.NamedRunnable;
import cn.jack.okhttp.okhttp3.internal.cache.CacheInterceptor;
import cn.jack.okhttp.okhttp3.internal.connection.ConnectInterceptor;
import cn.jack.okhttp.okhttp3.internal.connection.StreamAllocation;
import cn.jack.okhttp.okhttp3.internal.http.BridgeInterceptor;
import cn.jack.okhttp.okhttp3.internal.http.CallServerInterceptor;
import cn.jack.okhttp.okhttp3.internal.http.RealInterceptorChain;
import cn.jack.okhttp.okhttp3.internal.http.RetryAndFollowUpInterceptor;
import cn.jack.okhttp.okhttp3.internal.platform.Platform;

import static cn.jack.okhttp.okhttp3.internal.platform.Platform.INFO;

final class RealCall implements Call {
    final OkHttpClient client;
    final RetryAndFollowUpInterceptor retryAndFollowUpInterceptor;

    /**
     * There is a cycle between the {@link Call} and {@link EventListener} that makes this awkward.
     * This will be set after we create the call instance then create the event listener instance.
     */
    private EventListener eventListener;

    /**
     * The application's original request unadulterated by redirects or auth headers.
     */
    final Request originalRequest;
    final boolean forWebSocket;

    // Guarded by this.
    private boolean executed;

    private RealCall(OkHttpClient client, Request originalRequest, boolean forWebSocket) {
        this.client = client;
        this.originalRequest = originalRequest;
        this.forWebSocket = forWebSocket;
        this.retryAndFollowUpInterceptor = new RetryAndFollowUpInterceptor(client, forWebSocket);
    }

    static RealCall newRealCall(OkHttpClient client, Request originalRequest, boolean forWebSocket) {
        // Safely publish the Call instance to the EventListener.
        RealCall call = new RealCall(client, originalRequest, forWebSocket);
        call.eventListener = client.eventListenerFactory().create(call);  //创建事件监听器
        return call;
    }

    @Override
    public Request request() {
        return originalRequest;
    }

    @Override
    public Response execute() throws IOException {
        synchronized (this) {
            if (executed) throw new IllegalStateException("Already Executed");
            executed = true;
        }
        captureCallStackTrace();
        eventListener.callStart(this);
        try {
            client.dispatcher().executed(this);
            Response result = getResponseWithInterceptorChain();
            if (result == null) throw new IOException("Canceled");
            return result;
        } catch (IOException e) {
            eventListener.callFailed(this, e);
            throw e;
        } finally {
            client.dispatcher().finished(this);
        }
    }

    private void captureCallStackTrace() {
        Object callStackTrace = Platform.get().getStackTraceForCloseable("response.body().close()");
        retryAndFollowUpInterceptor.setCallStackTrace(callStackTrace);
    }

    @Override
    public void enqueue(Callback responseCallback) {
        synchronized (this) {
            if (executed) throw new IllegalStateException("Already Executed");
            executed = true;
        }
        captureCallStackTrace();
        eventListener.callStart(this);
        client.dispatcher().enqueue(new AsyncCall(responseCallback));
    }

    @Override
    public void cancel() {
        retryAndFollowUpInterceptor.cancel();
    }

    @Override
    public synchronized boolean isExecuted() {
        return executed;
    }

    @Override
    public boolean isCanceled() {
        return retryAndFollowUpInterceptor.isCanceled();
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    // We are a final type & this saves clearing state.
    @Override
    public RealCall clone() {
        return RealCall.newRealCall(client, originalRequest, forWebSocket);
    }

    StreamAllocation streamAllocation() {
        return retryAndFollowUpInterceptor.streamAllocation();
    }

    final class AsyncCall extends NamedRunnable {
        private final Callback responseCallback;

        AsyncCall(Callback responseCallback) {
            super("OkHttp %s", redactedUrl());
            this.responseCallback = responseCallback;
        }

        String host() {
            return originalRequest.url().host();
        }

        Request request() {
            return originalRequest;
        }

        RealCall get() {
            return RealCall.this;
        }

        @Override
        protected void execute() {
            // 防止多次回调
            boolean signalledCallback = false;
            try {
                Response response = getResponseWithInterceptorChain();
                if (retryAndFollowUpInterceptor.isCanceled()) {
                    signalledCallback = true;
                    // 请求失败
                    responseCallback.onFailure(RealCall.this, new IOException("Canceled"));
                } else {
                    signalledCallback = true;
                    // 请求成功，将response回调
                    responseCallback.onResponse(RealCall.this, response);
                }
            } catch (IOException e) {
                if (signalledCallback) {
                    // Do not signal the callback twice!
                    Platform.get().log(INFO, "Callback failure for " + toLoggableString(), e);
                } else {
                    eventListener.callFailed(RealCall.this, e);
                    responseCallback.onFailure(RealCall.this, e);
                }
            } finally {
                // 最终让调度器finish此次请求（好让下一次请求开始）
                client.dispatcher().finished(this);
            }
        }
    }

    /**
     * Returns a string that describes this call. Doesn't include a full URL as that might contain
     * sensitive information.
     */
    String toLoggableString() {
        return (isCanceled() ? "canceled " : "")
                + (forWebSocket ? "web socket" : "call")
                + " to " + redactedUrl();
    }

    String redactedUrl() {
        return originalRequest.url().redact();
    }

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
}
