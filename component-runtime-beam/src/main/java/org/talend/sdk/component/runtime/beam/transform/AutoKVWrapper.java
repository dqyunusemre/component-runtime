/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.beam.transform;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.json.JsonObject;

import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.talend.sdk.component.runtime.beam.coder.JsonpJsonObjectCoder;

import lombok.AllArgsConstructor;

/**
 * Extract the value of a branch if exists (unwrap).
 */
@AllArgsConstructor
public class AutoKVWrapper extends DoFn<JsonObject, KV<String, JsonObject>> {

    private SerializableFunction<JsonObject, String> idGenerator;

    protected AutoKVWrapper() {
        // no-op
    }

    @ProcessElement
    public void onElement(final ProcessContext context) {
        final JsonObject jsonObject = context.element();
        context.output(KV.of(idGenerator.apply(jsonObject), jsonObject));
    }

    public static PTransform<PCollection<JsonObject>, PCollection<KV<String, JsonObject>>> of(final String plugin,
            final SerializableFunction<JsonObject, String> idGenerator) {
        return new JsonObjectParDoTransformCoderProvider<>(
                KvCoder.of(StringUtf8Coder.of(), JsonpJsonObjectCoder.of(plugin)), new AutoKVWrapper(idGenerator));
    }

    public static class LocalSequenceHolder {

        private static final Map<String, AtomicLong> GENERATORS = new HashMap<>();

        public static SerializableFunction<JsonObject, String> cleanAndGet(final String name) {
            GENERATORS.put(name, new AtomicLong(0));
            return o -> Long.toString(GENERATORS.get(name).incrementAndGet());
        }

        public static void clean(final String name) {
            GENERATORS.remove(name);
        }
    }
}
