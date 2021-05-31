/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.api.service.discovery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import lombok.Getter;
import lombok.Setter;

@Getter
public class DiscoverDatasetResult {

    private final List<DatasetDescription> datasetDescriptionList = new ArrayList<>();

    public DiscoverDatasetResult() {
    }

    public DiscoverDatasetResult(final List<DatasetDescription> datasetDescriptionList) {
        this.datasetDescriptionList.addAll(datasetDescriptionList);
    }

    @Getter
    public final static class DatasetDescription {

        public DatasetDescription(final String name) {
            this.name = name;
        }

        private final String name;

        private Map<String, String> metadata = new TreeMap<>();

        public void addMetadata(final String key, final String value) {
            this.metadata.put(key, value);
        }

        @Setter
        private String datasetType;

        @Setter
        private Object dataset;
    }

}
