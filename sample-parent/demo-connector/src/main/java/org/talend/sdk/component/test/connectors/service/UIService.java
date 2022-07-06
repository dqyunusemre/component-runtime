/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.completion.SuggestionValues;
import org.talend.sdk.component.api.service.completion.SuggestionValues.Item;
import org.talend.sdk.component.api.service.completion.Suggestions;
import org.talend.sdk.component.api.service.update.Update;
import org.talend.sdk.component.test.connectors.config.DummySub;

@Service
public class UIService {

    public final static String LIST_ENTITIES = "LIST_ENTITIES";
    public final static String UPDATE_DUMMYSUB = "UPDATE_DUMMYSUB";

    /**
     * Suggestions action without any configuration.
     * 
     * @return
     */
    @Suggestions(LIST_ENTITIES)
    public SuggestionValues getListEntities() {
        final List<Item> entities =
                Arrays.stream(new String[] { "Account", "Customer", "Prospect", "Quotation", "Sale" })
                        .map(e -> new Item(e, e))
                        .collect(Collectors.toList());

        return new SuggestionValues(true, entities);
    }

    @Update(UPDATE_DUMMYSUB)
    public DummySub retrieveFeedback(final DummySub source) throws Exception {
        DummySub dest = new DummySub();
        dest.setAbc(source.getAbc());
        dest.setDef(source.getDef());
        return dest;
    }

}
