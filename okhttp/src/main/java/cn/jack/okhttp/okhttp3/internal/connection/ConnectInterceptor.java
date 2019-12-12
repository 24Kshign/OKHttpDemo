/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package cn.jack.okhttp.okhttp3.internal.connection;

import java.io.IOException;

import cn.jack.okhttp.okhttp3.Interceptor;
import cn.jack.okhttp.okhttp3.OkHttpClient;
import cn.jack.okhttp.okhttp3.Request;
import cn.jack.okhttp.okhttp3.Response;
import cn.jack.okhttp.okhttp3.internal.http.HttpCodec;
import cn.jack.okhttp.okhttp3.internal.http.RealInterceptorChain;

/** Opens a connection to the target server and proceeds to the next interceptor. */
public final class ConnectInterceptor implements Interceptor {
  public final OkHttpClient client;

  public ConnectInterceptor(OkHttpClient client) {
    this.client = client;
  }

  @Override public Response intercept(Chain chain) throws IOException {
    RealInterceptorChain realChain = (RealInterceptorChain) chain;
    Request request = realChain.request();
    // 负责管理连接、流和请求
    StreamAllocation streamAllocation = realChain.streamAllocation();

    // We need the network to satisfy this request. Possibly for validating a conditional GET.
    // 非GET请求会在后面做额外的判断
    boolean doExtensiveHealthChecks = !request.method().equals("GET");
    // 有两个实现类，分别是Http1Codec和Http2Codec，主要是用来进行Http请求和响应的编码/解码操作
    HttpCodec httpCodec = streamAllocation.newStream(client, chain, doExtensiveHealthChecks);
    RealConnection connection = streamAllocation.connection();

    return realChain.proceed(request, streamAllocation, httpCodec, connection);
  }
}
