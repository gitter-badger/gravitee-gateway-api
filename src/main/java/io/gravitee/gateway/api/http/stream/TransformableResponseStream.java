/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.api.http.stream;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.stream.TransformableStream;
import io.gravitee.gateway.api.stream.exception.TransformationException;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public abstract class TransformableResponseStream extends TransformableStream {

    private final Response response;

    public TransformableResponseStream(Response response) {
        this(response, -1);
    }

    public TransformableResponseStream(Response response, int contentLength) {
        super(contentLength);
        this.response = response;
    }

    @Override
    public void end() {
        Buffer content;

        try {
            content = transform().apply(buffer);

            // Set content length (remove useless transfer encoding header)
            response.headers().remove(HttpHeaders.TRANSFER_ENCODING);
            response.headers().set(HttpHeaders.CONTENT_LENGTH, Integer.toString(content.length()));

            // Set the content-type if settled
            String contentType = to();
            if (contentType != null && !contentType.isEmpty()) {
                response.headers().set(HttpHeaders.CONTENT_TYPE, contentType);
            }
        } catch (TransformationException tex) {
            content = Buffer.buffer(tex.getMessage());
            response.status(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            response.headers().set(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN);
            response.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
        }

        super.flush(content);
        super.end();
    }

    protected abstract String to();
}
