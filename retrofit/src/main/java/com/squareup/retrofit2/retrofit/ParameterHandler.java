/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.retrofit2.retrofit;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Map;

import cn.jack.okhttp.okhttp3.Headers;
import cn.jack.okhttp.okhttp3.MultipartBody;
import cn.jack.okhttp.okhttp3.RequestBody;

import static com.squareup.retrofit2.retrofit.Utils.checkNotNull;


abstract class ParameterHandler<T> {
  abstract void apply(RequestBuilder builder, @Nullable T value) throws IOException;

  final ParameterHandler<Iterable<T>> iterable() {
    return new ParameterHandler<Iterable<T>>() {
      @Override void apply(RequestBuilder builder, @Nullable Iterable<T> values)
          throws IOException {
        if (values == null) return; // Skip null values.

        for (T value : values) {
          ParameterHandler.this.apply(builder, value);
        }
      }
    };
  }

  final ParameterHandler<Object> array() {
    return new ParameterHandler<Object>() {
      @Override void apply(RequestBuilder builder, @Nullable Object values) throws IOException {
        if (values == null) return; // Skip null values.

        for (int i = 0, size = Array.getLength(values); i < size; i++) {
          //noinspection unchecked
          ParameterHandler.this.apply(builder, (T) Array.get(values, i));
        }
      }
    };
  }

  static final class RelativeUrl extends ParameterHandler<Object> {
    @Override void apply(RequestBuilder builder, @Nullable Object value) {
      checkNotNull(value, "@Url parameter is null.");
      builder.setRelativeUrl(value);
    }
  }

  static final class Header<T> extends ParameterHandler<T> {
    private final String name;
    private final Converter<T, String> valueConverter;

    Header(String name, Converter<T, String> valueConverter) {
      this.name = checkNotNull(name, "name == null");
      this.valueConverter = valueConverter;
    }

    @Override void apply(RequestBuilder builder, @Nullable T value) throws IOException {
      if (value == null) return; // Skip null values.

      String headerValue = valueConverter.convert(value);
      if (headerValue == null) return; // Skip converted but null values.

      builder.addHeader(name, headerValue);
    }
  }

  static final class Path<T> extends ParameterHandler<T> {
    private final String name;
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    Path(String name, Converter<T, String> valueConverter, boolean encoded) {
      this.name = checkNotNull(name, "name == null");
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override void apply(RequestBuilder builder, @Nullable T value) throws IOException {
      if (value == null) {
        throw new IllegalArgumentException(
            "Path parameter \"" + name + "\" value must not be null.");
      }
      builder.addPathParam(name, valueConverter.convert(value), encoded);
    }
  }

  static final class Query<T> extends ParameterHandler<T> {
    private final String name;
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    Query(String name, Converter<T, String> valueConverter, boolean encoded) {
      this.name = checkNotNull(name, "name == null");
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override void apply(RequestBuilder builder, @Nullable T value) throws IOException {
      if (value == null) return; // Skip null values.

      String queryValue = valueConverter.convert(value);
      if (queryValue == null) return; // Skip converted but null values

      builder.addQueryParam(name, queryValue, encoded);
    }
  }

  static final class QueryName<T> extends ParameterHandler<T> {
    private final Converter<T, String> nameConverter;
    private final boolean encoded;

    QueryName(Converter<T, String> nameConverter, boolean encoded) {
      this.nameConverter = nameConverter;
      this.encoded = encoded;
    }

    @Override void apply(RequestBuilder builder, @Nullable T value) throws IOException {
      if (value == null) return; // Skip null values.
      builder.addQueryParam(nameConverter.convert(value), null, encoded);
    }
  }

  static final class QueryMap<T> extends ParameterHandler<Map<String, T>> {
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    QueryMap(Converter<T, String> valueConverter, boolean encoded) {
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override void apply(RequestBuilder builder, @Nullable Map<String, T> value)
        throws IOException {
      if (value == null) {
        throw new IllegalArgumentException("Query map was null.");
      }

      for (Map.Entry<String, T> entry : value.entrySet()) {
        String entryKey = entry.getKey();
        if (entryKey == null) {
          throw new IllegalArgumentException("Query map contained null key.");
        }
        T entryValue = entry.getValue();
        if (entryValue == null) {
          throw new IllegalArgumentException(
              "Query map contained null value for key '" + entryKey + "'.");
        }

        String convertedEntryValue = valueConverter.convert(entryValue);
        if (convertedEntryValue == null) {
          throw new IllegalArgumentException("Query map value '"
              + entryValue
              + "' converted to null by "
              + valueConverter.getClass().getName()
              + " for key '"
              + entryKey
              + "'.");
        }

        builder.addQueryParam(entryKey, convertedEntryValue, encoded);
      }
    }
  }

  static final class HeaderMap<T> extends ParameterHandler<Map<String, T>> {
    private final Converter<T, String> valueConverter;

    HeaderMap(Converter<T, String> valueConverter) {
      this.valueConverter = valueConverter;
    }

    @Override void apply(RequestBuilder builder, @Nullable Map<String, T> value)
        throws IOException {
      if (value == null) {
        throw new IllegalArgumentException("Header map was null.");
      }

      for (Map.Entry<String, T> entry : value.entrySet()) {
        String headerName = entry.getKey();
        if (headerName == null) {
          throw new IllegalArgumentException("Header map contained null key.");
        }
        T headerValue = entry.getValue();
        if (headerValue == null) {
          throw new IllegalArgumentException(
              "Header map contained null value for key '" + headerName + "'.");
        }
        builder.addHeader(headerName, valueConverter.convert(headerValue));
      }
    }
  }

  static final class Field<T> extends ParameterHandler<T> {
    private final String name;
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    Field(String name, Converter<T, String> valueConverter, boolean encoded) {
      this.name = checkNotNull(name, "name == null");
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override void apply(RequestBuilder builder, @Nullable T value) throws IOException {
      if (value == null) return; // Skip null values.

      String fieldValue = valueConverter.convert(value);
      if (fieldValue == null) return; // Skip null converted values

      builder.addFormField(name, fieldValue, encoded);
    }
  }

  static final class FieldMap<T> extends ParameterHandler<Map<String, T>> {
    private final Converter<T, String> valueConverter;
    private final boolean encoded;

    FieldMap(Converter<T, String> valueConverter, boolean encoded) {
      this.valueConverter = valueConverter;
      this.encoded = encoded;
    }

    @Override void apply(RequestBuilder builder, @Nullable Map<String, T> value)
        throws IOException {
      if (value == null) {
        throw new IllegalArgumentException("Field map was null.");
      }

      for (Map.Entry<String, T> entry : value.entrySet()) {
        String entryKey = entry.getKey();
        if (entryKey == null) {
          throw new IllegalArgumentException("Field map contained null key.");
        }
        T entryValue = entry.getValue();
        if (entryValue == null) {
          throw new IllegalArgumentException(
              "Field map contained null value for key '" + entryKey + "'.");
        }

        String fieldEntry = valueConverter.convert(entryValue);
        if (fieldEntry == null) {
          throw new IllegalArgumentException("Field map value '"
              + entryValue
              + "' converted to null by "
              + valueConverter.getClass().getName()
              + " for key '"
              + entryKey
              + "'.");
        }

        builder.addFormField(entryKey, fieldEntry, encoded);
      }
    }
  }

  static final class Part<T> extends ParameterHandler<T> {
    private final Headers headers;
    private final Converter<T, RequestBody> converter;

    Part(Headers headers, Converter<T, RequestBody> converter) {
      this.headers = headers;
      this.converter = converter;
    }

    @Override void apply(RequestBuilder builder, @Nullable T value) {
      if (value == null) return; // Skip null values.

      RequestBody body;
      try {
        body = converter.convert(value);
      } catch (IOException e) {
        throw new RuntimeException("Unable to convert " + value + " to RequestBody", e);
      }
      builder.addPart(headers, body);
    }
  }

  static final class RawPart extends ParameterHandler<MultipartBody.Part> {
    static final RawPart INSTANCE = new RawPart();

    private RawPart() {
    }

    @Override void apply(RequestBuilder builder, @Nullable MultipartBody.Part value)
        throws IOException {
      if (value != null) { // Skip null values.
        builder.addPart(value);
      }
    }
  }

  static final class PartMap<T> extends ParameterHandler<Map<String, T>> {
    private final Converter<T, RequestBody> valueConverter;
    private final String transferEncoding;

    PartMap(Converter<T, RequestBody> valueConverter, String transferEncoding) {
      this.valueConverter = valueConverter;
      this.transferEncoding = transferEncoding;
    }

    @Override void apply(RequestBuilder builder, @Nullable Map<String, T> value)
        throws IOException {
      if (value == null) {
        throw new IllegalArgumentException("Part map was null.");
      }

      for (Map.Entry<String, T> entry : value.entrySet()) {
        String entryKey = entry.getKey();
        if (entryKey == null) {
          throw new IllegalArgumentException("Part map contained null key.");
        }
        T entryValue = entry.getValue();
        if (entryValue == null) {
          throw new IllegalArgumentException(
              "Part map contained null value for key '" + entryKey + "'.");
        }

        Headers headers = Headers.of(
            "Content-Disposition", "form-data; name=\"" + entryKey + "\"",
            "Content-Transfer-Encoding", transferEncoding);

        builder.addPart(headers, valueConverter.convert(entryValue));
      }
    }
  }

  static final class Body<T> extends ParameterHandler<T> {
    private final Converter<T, RequestBody> converter;

    Body(Converter<T, RequestBody> converter) {
      this.converter = converter;
    }

    @Override void apply(RequestBuilder builder, @Nullable T value) {
      if (value == null) {
        throw new IllegalArgumentException("Body parameter value must not be null.");
      }
      RequestBody body;
      try {
        body = converter.convert(value);
      } catch (IOException e) {
        throw new RuntimeException("Unable to convert " + value + " to RequestBody", e);
      }
      builder.setBody(body);
    }
  }
}
