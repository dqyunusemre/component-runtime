/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.test.connectors.service;

import javax.json.JsonObject;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.http.Base;
import org.talend.sdk.component.api.service.http.Configurer;
import org.talend.sdk.component.api.service.http.Header;
import org.talend.sdk.component.api.service.http.HttpClient;
import org.talend.sdk.component.api.service.http.Path;
import org.talend.sdk.component.api.service.http.Request;
import org.talend.sdk.component.api.service.http.Response;
import org.talend.sdk.component.api.service.http.UseConfigurer;

public interface HttpAPIClient extends HttpClient {
    // sample taken from https://talend.github.io/component-runtime/main/latest/services-built-in.html#httpclient_usage

    // TODO implement one @Codec(decoder = RecordDecoder.class)
    // TODO implement DELETE/PUT...

    /**
     * Get API call
     */
    @Request(path = "api/records/{id}", method = "GET")
    Record getRecord(@Header("Authorization") String basicAuth,
                     @Path("id") int id);

    /**
     * Get API call with base as parameter
     **/
    @Request(path = "api/records/{id}", method = "GET")
    Record getRecord(@Header("Authorization") String basicAuth,
                     @Base String base,
                     @Path("id") int id);

    /**
     * Post API call
     **/
    @Request(path = "api/records", method = "POST")
    Record createRecord(@Header("Authorization") String basicAuth,
                        Record record);

    class TimeOoutConfigurer implements Configurer {

        @Override
        public void configure(final Connection connection, final ConfigurerConfiguration configuration) {
            connection.withConnectionTimeout(3000);
            connection.withHeader("Content-type", "application/json");
        }
    }
}
