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
package org.talend.sdk.component.maven.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MavenThreadFactory implements ThreadFactory {

    private final String prefix;

    private final AtomicInteger id = new AtomicInteger(1);

    @Override
    public Thread newThread(final Runnable runnable) {
        return new Thread(runnable, String.format("%s-mojo-%d", prefix, id.getAndIncrement()));
    }
}
