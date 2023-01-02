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
package org.talend.sdk.component.runtime.di.record;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toMap;
import static org.talend.sdk.component.api.record.SchemaProperty.IS_KEY;
import static org.talend.sdk.component.api.record.SchemaProperty.PATTERN;
import static org.talend.sdk.component.api.record.SchemaProperty.SCALE;
import static org.talend.sdk.component.api.record.SchemaProperty.SIZE;
import static org.talend.sdk.component.api.record.SchemaProperty.STUDIO_TYPE;

import routines.system.Dynamic;
import routines.system.DynamicMetadata;
import routines.system.DynamicMetadata.sourceTypes;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.bind.JsonbConfig;
import javax.json.bind.spi.JsonbProvider;
import javax.json.spi.JsonProvider;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.api.service.record.RecordService;
import org.talend.sdk.component.api.service.record.RecordVisitor;
import org.talend.sdk.component.runtime.di.schema.StudioTypes;
import org.talend.sdk.component.runtime.manager.service.DefaultServiceProvider;
import org.talend.sdk.component.runtime.record.MappingUtils;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class DiRecordVisitor implements RecordVisitor<Object> {

    private final Class<?> clazz;

    private Object instance;

    private final Map<String, Field> fields;

    private final boolean hasDynamic;

    private final Dynamic dynamic;

    private final String dynamicColumn;

    private final int dynamicColumnLength;

    private final int dynamicColumnPrecision;

    private final String dynamicColumnPattern;

    private String recordPrefix = "";

    private String arrayOfRecordPrefix = "";

    private Set<String> recordFields;

    private static final RecordService RECORD_SERVICE = RecordService.class
            .cast(new DefaultServiceProvider(null, JsonProvider.provider(), Json.createGeneratorFactory(emptyMap()),
                    Json.createReaderFactory(emptyMap()), Json.createBuilderFactory(emptyMap()),
                    Json.createParserFactory(emptyMap()), Json.createWriterFactory(emptyMap()), new JsonbConfig(),
                    JsonbProvider.provider(), null, null, emptyList(), t -> new RecordBuilderFactoryImpl("di"), null)
                            .lookup(null, Thread.currentThread().getContextClassLoader(), null, null,
                                    RecordService.class, null, null));

    DiRecordVisitor(final Class<?> clzz, final java.util.Map<String, String> metadata) {
        clazz = clzz;
        try {
            instance = clazz.getConstructor().newInstance();
            fields = Arrays.stream(instance.getClass().getFields()).collect(toMap(Field::getName, identity()));
            hasDynamic = fields
                    .values()
                    .stream()
                    .anyMatch(field -> "routines.system.Dynamic".equals(field.getType().getName()));
            dynamicColumn = fields
                    .values()
                    .stream()
                    .filter(field -> "routines.system.Dynamic".equals(field.getType().getName()))
                    .map(Field::getName)
                    .findAny()
                    .orElse(null);
            if (hasDynamic) {
                dynamic = new Dynamic();
            } else {
                dynamic = null;
            }
            log
                    .debug("[DiRecordVisitor] {} dynamic? {} ({} {}).", clazz.getName(), hasDynamic, dynamicColumn,
                            metadata);
            dynamicColumnLength = Integer.valueOf(metadata.getOrDefault(SIZE, "-1"));
            dynamicColumnPrecision = Integer.valueOf(metadata.getOrDefault(SCALE, "-1"));
            dynamicColumnPattern = metadata.getOrDefault(PATTERN, "yyyy-MM-dd");
        } catch (final NoSuchMethodException | IllegalAccessException | InstantiationException
                | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    public Object visit(final Record record) {
        arrayOfRecordPrefix = "";
        recordPrefix = "";
        try {
            instance = clazz.getConstructor().newInstance();
        } catch (final InstantiationException | InvocationTargetException | NoSuchMethodException
                | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        if (hasDynamic) {
            dynamic.metadatas.clear();
            dynamic.clearColumnValues();
        }
        recordFields = record.getSchema().getAllEntries().filter(t -> t.getType().equals(Type.RECORD)).map(rcdEntry -> {
            final String root = rcdEntry.getName() + ".";
            final List<String> names = new ArrayList<>();
            rcdEntry.getElementSchema().getAllEntries().filter(e -> e.getType().equals(Type.RECORD)).map(sr -> {
                final String sub = root + sr.getName() + ".";
                return sr
                        .getElementSchema()
                        .getAllEntries()
                        .map(entry -> sub + entry.getName())
                        .collect(Collectors.toList());
            }).forEach(l -> l.stream().forEach(m -> names.add(m)));
            rcdEntry
                    .getElementSchema()
                    .getAllEntries()
                    .filter(e -> !e.getType().equals(Type.RECORD))
                    .map(entry -> root + entry.getName())
                    .forEach(sre -> names.add(sre));
            return names;
        }).flatMap(liststream -> liststream.stream()).collect(Collectors.toSet());
        recordFields
                .addAll(record
                        .getSchema()
                        .getAllEntries()
                        .filter(t -> !t.getType().equals(Type.RECORD))
                        .map(entry -> entry.getName())
                        .collect(Collectors.toSet()));
        if (hasDynamic) {
            prefillDynamic(record.getSchema());
        }

        return RECORD_SERVICE.visit(this, record);
    }

    @Override
    public Object get() {
        if (hasDynamic) {
            try {
                fields.get(dynamicColumn).set(instance, dynamic);
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        return instance;
    }

    /**
     * prefills Dynamic metadatas when the visitor do not visit it
     *
     * @param schema
     */
    private void prefillDynamic(final Schema schema) {
        schema
                .getAllEntries()
                .filter(entry -> (!fields.containsKey(entry.getName())) || dynamicColumn.equals(entry.getName()))
                .forEach(entry -> {
                    dynamic.metadatas.add(generateMetadata(entry));
                    dynamic.addColumnValue(null);
                });
    }

    private DynamicMetadata generateMetadata(final Entry entry) {
        final DynamicMetadata metadata = new DynamicMetadata();
        metadata
                .setName(recordFields
                        .stream()
                        .filter(f -> f.endsWith("." + entry.getName()))
                        .findFirst()
                        .orElse(entry.getName()));
        metadata.setDbName(entry.getOriginalFieldName());
        metadata.setNullable(entry.isNullable());
        metadata.setDescription(entry.getComment());
        metadata.setSourceType(sourceTypes.unknown);

        final Boolean isKey = ofNullable(entry.getProp(IS_KEY))
                .filter(l -> !l.isEmpty())
                .map(Boolean::valueOf)
                .orElse(false);
        final Integer length = ofNullable(entry.getProp(SIZE))
                .filter(l -> !l.isEmpty())
                .map(Integer::valueOf)
                .orElse(dynamicColumnLength);
        final Integer precision = ofNullable(entry.getProp(SCALE))
                .filter(l -> !l.isEmpty())
                .map(Integer::valueOf)
                .orElse(dynamicColumnPrecision);
        final String pattern = ofNullable(entry.getProp(PATTERN))
                .filter(l -> !l.isEmpty())
                .orElse(dynamicColumnPattern);
        final String studioType = entry.getProps()
                .getOrDefault(STUDIO_TYPE, StudioTypes.typeFromRecord(entry.getType()));
        metadata.setKey(isKey);
        metadata.setType(studioType);

        if (length != null) {
            metadata.setLength(length);
        }

        if (precision != null) {
            metadata.setPrecision(precision);
        }

        switch (studioType) {
        case StudioTypes.DATE:
            metadata.setLogicalType("timestamp-millis");
            metadata.setFormat(pattern);
            break;
        default:
            // nop
            break;
        }
        return metadata;
    }

    private void setField(final Entry entry, final Object value) {
        final Field field = fields.get(entry.getName());
        if (hasDynamic && (field == null || dynamicColumn.equals(entry.getName()))) {
            final String name = recordFields
                    .stream()
                    .filter(f -> f.endsWith("." + entry.getName()))
                    .findFirst()
                    .orElse(entry.getName());
            int index = dynamic.getIndex(name);
            final DynamicMetadata metadata;
            if (index < 0) {
                metadata = generateMetadata(entry);
                dynamic.metadatas.add(metadata);
                index = dynamic.getIndex(name);
            } else {
                metadata = dynamic.getColumnMetadata(index);
            }

            final Class<?> clazz = StudioTypes.classFromType(metadata.getType());
            if (clazz != null) {
                dynamic.setColumnValue(index, MappingUtils.coerce(clazz, value, name));
            } else {
                dynamic.setColumnValue(index, MappingUtils.coerce(value.getClass(), value, name));
            }
            log.debug("[setField] Dynamic#{}\t{}\t({})\t ==> {}.", index, name, metadata.getType(), value);
            return;
        }
        if (field == null) {
            return;
        }
        try {
            field.set(instance, MappingUtils.coerce(field.getType(), value, entry.getName()));
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void onInt(final Entry entry, final OptionalInt optionalInt) {
        log.debug("[onInt] visiting {}.", entry.getName());
        optionalInt.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onLong(final Entry entry, final OptionalLong optionalLong) {
        log.debug("[onLong] visiting {}.", entry.getName());
        optionalLong.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onFloat(final Entry entry, final OptionalDouble optionalFloat) {
        log.debug("[onFloat] visiting {}.", entry.getName());
        optionalFloat.ifPresent(value -> setField(entry, (float) value));
    }

    @Override
    public void onDouble(final Entry entry, final OptionalDouble optionalDouble) {
        log.debug("[onDouble] visiting {}.", entry.getName());
        optionalDouble.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onBoolean(final Entry entry, final Optional<Boolean> optionalBoolean) {
        log.debug("[onBoolean] visiting {}.", entry.getName());
        optionalBoolean.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onString(final Entry entry, final Optional<String> string) {
        log.debug("[onString] visiting {}.", entry.getName());
        string.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onObject(final Entry entry, final Optional<Object> object) {
        log.debug("[onObject] visiting {}.", entry.getName());
        object.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onDatetime(final Entry entry, final Optional<ZonedDateTime> dateTime) {
        log.debug("[onDatetime] visiting {}.", entry.getName());
        dateTime.ifPresent(value -> setField(entry, value.toInstant().toEpochMilli()));
    }

    @Override
    public void onDecimal(final Entry entry, final Optional<BigDecimal> decimal) {
        log.debug("[onDecimal] visiting {}.", entry.getName());
        decimal.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onBytes(final Entry entry, final Optional<byte[]> bytes) {
        log.debug("[onBytes] visiting {}.", entry.getName());
        bytes.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onIntArray(final Entry entry, final Optional<Collection<Integer>> array) {
        log.debug("[onIntArray] visiting {}.", entry.getName());
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onLongArray(final Entry entry, final Optional<Collection<Long>> array) {
        log.debug("[onLongArray] visiting {}.", entry.getName());
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onFloatArray(final Entry entry, final Optional<Collection<Float>> array) {
        log.debug("[onFloatArray] visiting {}.", entry.getName());
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onDoubleArray(final Entry entry, final Optional<Collection<Double>> array) {
        log.debug("[onDoubleArray] visiting {}.", entry.getName());
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onBooleanArray(final Entry entry, final Optional<Collection<Boolean>> array) {
        log.debug("[onBooleanArray] visiting {}.", entry.getName());
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onStringArray(final Entry entry, final Optional<Collection<String>> array) {
        log.debug("[onStringArray] visiting {}.", entry.getName());
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onDatetimeArray(final Entry entry, final Optional<Collection<ZonedDateTime>> array) {
        log.debug("[onDatetimeArray] visiting {}.", entry.getName());
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onDecimalArray(final Entry entry, final Optional<Collection<BigDecimal>> array) {
        log.debug("[onDecimalArray] visiting {}.", entry.getName());
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public void onBytesArray(final Entry entry, final Optional<Collection<byte[]>> array) {
        log.debug("[onBytesArray] visiting {}.", entry.getName());
        array.ifPresent(value -> setField(entry, value));
    }

    @Override
    public RecordVisitor<Object> onRecordArray(final Entry entry, final Optional<Collection<Record>> array) {
        log.debug("[onRecordArray] visiting {}.", entry.getName());
        arrayOfRecordPrefix = entry.getName() + ".";
        array.ifPresent(value -> setField(entry, value));
        return this;
    }

    @Override
    public RecordVisitor<Object> onRecord(final Entry entry, final Optional<Record> record) {
        log.debug("[onRecord] visiting {}.", entry.getName());
        recordPrefix = entry.getName() + ".";
        record.ifPresent(value -> setField(entry, value));
        return this;
    }

}
