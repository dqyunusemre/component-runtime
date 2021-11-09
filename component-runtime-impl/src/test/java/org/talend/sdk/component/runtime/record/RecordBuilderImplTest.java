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
package org.talend.sdk.component.runtime.record;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.runtime.record.SchemaImpl.BuilderImpl;

class RecordBuilderImplTest {

    @Test
    void providedSchemaGetSchema() {
        final Schema schema = new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.Entry.Builder()
                        .withName("name")
                        .withNullable(true)
                        .withType(Schema.Type.STRING)
                        .build())
                .build();
        assertEquals(schema, new RecordImpl.BuilderImpl(schema).withString("name", "ok").build().getSchema());
    }

    @Test
    void getValue() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        Assertions.assertNull(builder.getValue("name"));
        final Entry entry = new Entry.Builder() //
                .withName("name") //
                .withNullable(true) //
                .withType(Type.STRING) //
                .build();//
        assertThrows(IllegalArgumentException.class, () -> builder.with(entry, 234L));

        builder.with(entry, "value");
        Assertions.assertEquals("value", builder.getValue("name"));

        final Entry entryTime = new Entry.Builder() //
                .withName("time") //
                .withNullable(true) //
                .withType(Type.DATETIME) //
                .build();//
        final ZonedDateTime now = ZonedDateTime.now();
        builder.with(entryTime, now);
        Assertions.assertEquals(now.toInstant().toEpochMilli(), builder.getValue("time"));

        final Long next = now.toInstant().toEpochMilli() + 1000L;
        builder.with(entryTime, next);
        Assertions.assertEquals(next, builder.getValue("time"));

        Date date = new Date(next + TimeUnit.DAYS.toMillis(1));
        builder.with(entryTime, date);
        Assertions.assertEquals(date.toInstant().toEpochMilli(), builder.getValue("time"));
    }

    @Test
    void recordEntryFromName() {
        final Schema schema = new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.Entry.Builder()
                        .withName("name")
                        .withNullable(true)
                        .withType(Schema.Type.STRING)
                        .build())
                .build();
        assertEquals("{\"record\":{\"name\":\"ok\"}}",
                new RecordImpl.BuilderImpl()
                        .withRecord("record", new RecordImpl.BuilderImpl(schema).withString("name", "ok").build())
                        .build()
                        .toString());
    }

    @Test
    void providedSchemaNullable() {
        final Supplier<RecordImpl.BuilderImpl> builder = () -> new RecordImpl.BuilderImpl(new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.Entry.Builder()
                        .withName("name")
                        .withNullable(true)
                        .withType(Schema.Type.STRING)
                        .build())
                .build());
        { // normal/valued
            final Record record = builder.get().withString("name", "ok").build();
            assertEquals(1, record.getSchema().getEntries().size());
            assertEquals("ok", record.getString("name"));
        }
        { // null
            final Record record = builder.get().withString("name", null).build();
            assertEquals(1, record.getSchema().getEntries().size());
            assertNull(record.getString("name"));
        }
        { // missing entry in the schema
            assertThrows(IllegalArgumentException.class, () -> builder.get().withString("name2", null).build());
        }
        { // invalid type entry
            assertThrows(IllegalArgumentException.class, () -> builder.get().withInt("name", 2).build());
        }
    }

    @Test
    void providedSchemaNotNullable() {
        final Supplier<RecordImpl.BuilderImpl> builder = () -> new RecordImpl.BuilderImpl(new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.Entry.Builder()
                        .withName("name")
                        .withNullable(false)
                        .withType(Schema.Type.STRING)
                        .build())
                .build());
        { // normal/valued
            final Record record = builder.get().withString("name", "ok").build();
            assertEquals(1, record.getSchema().getEntries().size());
            assertEquals("ok", record.getString("name"));
        }
        { // null
            assertThrows(IllegalArgumentException.class, () -> builder.get().withString("name", null).build());
        }
        { // missing entry value
            assertThrows(IllegalArgumentException.class, () -> builder.get().build());
        }
    }

    @Test
    void nullSupportString() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder.withString("test", null);
        final Record record = builder.build();
        assertEquals(1, record.getSchema().getEntries().size());
        assertNull(record.getString("test"));
    }

    @Test
    void nullSupportDate() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder.withDateTime("test", (Date) null);
        final Record record = builder.build();
        assertEquals(1, record.getSchema().getEntries().size());
        assertNull(record.getDateTime("test"));
    }

    @Test
    void nullSupportBytes() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder.withBytes("test", null);
        final Record record = builder.build();
        assertEquals(1, record.getSchema().getEntries().size());
        assertNull(record.getBytes("test"));
    }

    @Test
    void nullSupportCollections() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        final Schema innerArray = new BuilderImpl().withType(Type.STRING).build();
        final Entry arrayEntry = new Entry.Builder() //
                .withName("test") //
                .withRawName("test") //
                .withType(Type.ARRAY) //
                .withNullable(true) //
                .withElementSchema(innerArray) //
                .build();
        builder.withArray(arrayEntry, null);
        final Record record = builder.build();
        assertEquals(1, record.getSchema().getEntries().size());
        assertNull(record.getArray(String.class, "test"));
    }

    @Test
    void notNullableNullBehavior() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        assertThrows(IllegalArgumentException.class, () -> builder
                .withString(new SchemaImpl.Entry.Builder().withNullable(false).withName("test").build(), null));
    }

    @Test
    void dateTime() {
        final Schema schema = new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.Entry.Builder()
                        .withName("date")
                        .withNullable(false)
                        .withType(Schema.Type.DATETIME)
                        .build())
                .build();
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);
        final Record record = builder.withDateTime("date", ZonedDateTime.now()).build();
        Assertions.assertNotNull(record.getDateTime("date"));

        final RecordImpl.BuilderImpl builder2 = new RecordImpl.BuilderImpl(schema);
        assertThrows(IllegalArgumentException.class, () -> builder2.withDateTime("date", (ZonedDateTime) null));
    }

    @Test
    void array() {
        final Schema schemaArray = new SchemaImpl.BuilderImpl().withType(Schema.Type.STRING).build();
        final Schema.Entry entry = new SchemaImpl.Entry.Builder()
                .withName("data")
                .withNullable(false)
                .withType(Schema.Type.ARRAY)
                .withElementSchema(schemaArray)
                .build();
        final Schema schema = new SchemaImpl.BuilderImpl().withType(Schema.Type.RECORD).withEntry(entry).build();
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);

        final Record record = builder.withArray(entry, Arrays.asList("d1", "d2")).build();
        final Collection<String> data = record.getArray(String.class, "data");
        assertEquals(2, data.size());
    }

    @Test
    void withProps() {
        final LinkedHashMap<String, String> rootProps = new LinkedHashMap<>();
        IntStream.range(0, 10).forEach(i -> rootProps.put("key" + i, "value" + i));
        final LinkedHashMap<String, String> fieldProps = new LinkedHashMap<>();
        fieldProps.put("org.talend.components.metadata.one", "one_1");
        fieldProps.put("org.talend.components.metadata.two", "two_2");
        final Schema schema = new BuilderImpl()
                .withType(Type.RECORD)
                .withProps(rootProps)
                .withEntry(new Entry.Builder().withName("f01").withType(Type.STRING).build())
                .withEntry(new Entry.Builder().withName("f02").withType(Type.STRING).withProps(fieldProps).build())
                .build();
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);
        final Record record = builder.withString("f01", "field-one").withString("f02", "field-two").build();
        final Schema rSchema = record.getSchema();
        assertEquals("field-one", record.getString("f01"));
        assertEquals("field-two", record.getString("f02"));
        assertEquals(rootProps, rSchema.getProps());
        assertEquals(0, schema.getEntries().get(0).getProps().size());
        assertEquals(2, schema.getEntries().get(1).getProps().size());
        assertEquals(fieldProps, schema.getEntries().get(1).getProps());
        assertEquals("one_1", schema.getEntries().get(1).getProp("org.talend.components.metadata.one"));
        assertEquals("two_2", schema.getEntries().get(1).getProp("org.talend.components.metadata.two"));
        assertEquals(schema, rSchema);
    }

    @Test
    void withProp() {
        final Schema schema = new BuilderImpl()
                .withType(Type.RECORD)
                .withProp("rootProp1", "rootPropValue1")
                .withProp("rootProp2", "rootPropValue2")
                .withEntry(new Entry.Builder()
                        .withName("f01")
                        .withType(Type.STRING)
                        .withProp("dqType", "semantic-test1")
                        .build())
                .withEntry(new Entry.Builder()
                        .withName("f02")
                        .withType(Type.STRING)
                        .withProp("dqType", "semantic-test2")
                        .build())
                .build();
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);
        final Record record = builder.withString("f01", "field-one").withString("f02", "field-two").build();
        final Schema rSchema = record.getSchema();
        assertEquals(schema, rSchema);
        assertEquals("field-one", record.getString("f01"));
        assertEquals("field-two", record.getString("f02"));
        assertEquals(3, rSchema.getProps().size());
        assertEquals("f01,f02", rSchema.getProp("talend.fields.order"));
        assertEquals("rootPropValue1", rSchema.getProp("rootProp1"));
        assertEquals("rootPropValue2", rSchema.getProp("rootProp2"));
        assertEquals(1, rSchema.getEntries().get(0).getProps().size());
        assertEquals("semantic-test1", rSchema.getEntries().get(0).getProp("dqType"));
        assertEquals(1, rSchema.getEntries().get(1).getProps().size());
        assertEquals("semantic-test2", rSchema.getEntries().get(1).getProp("dqType"));
    }

    @Test
    void withPropsMerging() {
        final LinkedHashMap<String, String> rootProps = new LinkedHashMap<>();
        IntStream.range(0, 10).forEach(i -> rootProps.put("key" + i, "value" + i));
        final LinkedHashMap<String, String> fieldProps = new LinkedHashMap<>();
        fieldProps.put("dqType", "one_1");
        fieldProps.put("org.talend.components.metadata.two", "two_2");
        final Schema schema = new BuilderImpl()
                .withType(Type.RECORD)
                .withProp("key9", "rootPropValue9")
                .withProps(rootProps)
                .withProp("key1", "rootPropValue1")
                .withProp("key2", "rootPropValue2")
                .withProp("rootProp2", "rootPropValue2")
                .withEntry(new Entry.Builder()
                        .withName("f01")
                        .withType(Type.STRING)
                        .withProp("dqType", "semantic-test1")
                        .withProps(fieldProps)
                        .build())
                .withEntry(new Entry.Builder()
                        .withName("f02")
                        .withType(Type.STRING)
                        .withProps(fieldProps)
                        .withProp("dqType", "semantic-test2")
                        .build())
                .build();
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);
        final Record record = builder.withString("f01", "field-one").withString("f02", "field-two").build();
        final Schema rSchema = record.getSchema();
        assertEquals(schema, rSchema);
        assertEquals("field-one", record.getString("f01"));
        assertEquals("field-two", record.getString("f02"));
        assertEquals(12, rSchema.getProps().size());
        assertEquals("f01,f02", rSchema.getProp("talend.fields.order"));
        assertEquals("rootPropValue1", rSchema.getProp("key1"));
        assertEquals("rootPropValue2", rSchema.getProp("key2"));
        assertEquals("value3", rSchema.getProp("key3"));
        assertEquals("value9", rSchema.getProp("key9"));
        assertEquals("rootPropValue2", rSchema.getProp("rootProp2"));
        assertEquals(2, rSchema.getEntries().get(0).getProps().size());
        assertEquals("one_1", rSchema.getEntries().get(0).getProp("dqType"));
        assertEquals("two_2", rSchema.getEntries().get(0).getProp("org.talend.components.metadata.two"));
        assertEquals(2, rSchema.getEntries().get(1).getProps().size());
        assertEquals("semantic-test2", rSchema.getEntries().get(1).getProp("dqType"));
        assertEquals("two_2", rSchema.getEntries().get(1).getProp("org.talend.components.metadata.two"));
    }

    @Test
    void entries() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder.withString("field1", "Hello").withInt("fieldInt", 20);
        final List<Entry> entries = builder.getCurrentEntries();
        Assertions.assertEquals(2, entries.size());
        final Entry entry = entries.stream().filter((Entry e) -> "field1".equals(e.getName())).findFirst().get();
        Assertions.assertSame(Schema.Type.STRING, entry.getType());

        final Entry entry1 = entries.stream().filter((Entry e) -> "fieldInt".equals(e.getName())).findFirst().get();
        Assertions.assertSame(Schema.Type.INT, entry1.getType());

        final Schema schema = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(new Entry.Builder()
                        .withName("field1")
                        .withRawName("field1")
                        .withType(Type.INT)
                        .withNullable(true)
                        .withDefaultValue(5)
                        .withComment("Comment")
                        .build())
                .build();
        final RecordImpl.BuilderImpl builder1 = new RecordImpl.BuilderImpl(schema);
        final List<Entry> entries1 = builder1.getCurrentEntries();
        Assertions.assertEquals(1, entries1.size());
    }

    @Test
    void removeEntries() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder.withString("field1", "Hello").withInt("fieldInt", 20);
        final List<Entry> entries = builder.getCurrentEntries();
        Assertions.assertEquals(2, entries.size());

        final Entry entry = entries.stream().filter((Entry e) -> "field1".equals(e.getName())).findFirst().get();
        builder.removeEntry(entry);
        Assertions.assertEquals(1, builder.getCurrentEntries().size());
        Assertions.assertTrue(entries.stream().anyMatch((Entry e) -> "fieldInt".equals(e.getName())));

        Schema.Entry unknownEntry = newEntry("fieldUnknown", "fieldUnknown", Type.STRING, true, "unknown", "Comment");
        assertThrows(IllegalArgumentException.class, () -> builder.removeEntry(unknownEntry));

        final Schema schema = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(newEntry("field1", "field1", Type.INT, true, 5, "Comment"))
                .withEntry(newMetaEntry("meta1", "meta1", Type.INT, true, 5, "Comment"))
                .build();
        final RecordImpl.BuilderImpl builder1 = new RecordImpl.BuilderImpl(schema);
        final List<Entry> entries1 = builder1.getCurrentEntries();
        Assertions.assertEquals(2, entries1.size());
        final Entry entry1 = entries1.stream().filter((Entry e) -> "field1".equals(e.getName())).findFirst().get();
        Record.Builder newBuilder = builder1.removeEntry(entry1);
        final Entry meta1 = entries1.stream().filter((Entry e) -> "meta1".equals(e.getName())).findFirst().get();
        Record.Builder newBuilder2 = newBuilder.removeEntry(meta1);
        Assertions.assertEquals(0, newBuilder2.getCurrentEntries().size());
    }

    @Test
    void updateEntryByName_fromEntries() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder.withString("field1", "Hello").withInt("fieldInt", 20);
        final List<Entry> entries = builder.getCurrentEntries();
        Assertions.assertEquals(2, entries.size());

        final Entry entry = newEntry("field2", "newFieldName", Type.STRING, true, 5, "Comment");
        builder.updateEntryByName("field1", entry);
        Assertions.assertEquals(2, builder.getCurrentEntries().size());
        Assertions
                .assertTrue(entries
                        .stream()
                        .anyMatch((Entry e) -> "field2".equals(e.getName()) && "newFieldName".equals(e.getRawName())));
        assertEquals("Hello", builder.getValue("field2"));

        final Entry entryTypeNotCompatible = newEntry("field3", "newFieldName", Type.INT, true, 5, "Comment");
        assertThrows(IllegalArgumentException.class, () -> builder.updateEntryByName("field2", entryTypeNotCompatible));

        Schema.Entry unknownEntry = newEntry("fieldUnknown", "fieldUnknown", Type.STRING, true, "unknown", "Comment");
        assertThrows(IllegalArgumentException.class, () -> builder.updateEntryByName("fieldUnknown", unknownEntry));
    }

    @Test
    void updateEntryByName_fromProvidedSchema() {
        final Schema schema = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(newEntry("field1", "field1", Type.STRING, true, 5, "Comment"))
                .build();
        final RecordImpl.BuilderImpl builder1 = new RecordImpl.BuilderImpl(schema);
        builder1.with(schema.getEntry("field1"), "10");
        final List<Entry> entries1 = builder1.getCurrentEntries();
        Assertions.assertEquals(1, entries1.size());
        final Entry entry1 = newEntry("field2", "newFieldName", Type.STRING, true, 5, "Comment");
        Record.Builder newBuilder = builder1.updateEntryByName("field1", entry1);
        Assertions.assertEquals(1, newBuilder.getCurrentEntries().size());
        Assertions
                .assertTrue(newBuilder
                        .getCurrentEntries()
                        .stream()
                        .anyMatch((Entry e) -> "field2".equals(e.getName()) && "newFieldName".equals(e.getRawName())
                                && Type.STRING.equals(e.getType())));
        assertEquals("10", newBuilder.getValue("field2"));
    }

    @Test
    void testSimpleCollision() {
        final Record record = new RecordImpl.BuilderImpl() //
                .withString("goodName", "v1") //
                .withString("goodName", "v2") //
                .build();
        Assertions.assertEquals("v2", record.getString("goodName"));

        // Case with collision and sanitize.
        final Record recordSanitize = new RecordImpl.BuilderImpl() //
                .withString("70歳以上", "value70") //
                .withString("60歳以上", "value60") //
                .build();
        Assertions.assertEquals(2, recordSanitize.getSchema().getEntries().size());
        final String name1 = Schema.sanitizeConnectionName("70歳以上");
        Assertions.assertEquals("value70", recordSanitize.getString(name1));
        Assertions.assertEquals("value60", recordSanitize.getString(name1 + "_1"));
    }

    @Test
    void testUsedSameEntry() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        final Schema.Entry e1 = new Entry.Builder().withName("_0000").withType(Type.STRING).build();

        builder.withString(e1, "value1");
        final Schema.Entry e2 =
                new Entry.Builder().withName("_0001").withRawName("_0000_number").withType(Type.STRING).build();
        builder.withString(e2, "value2");
        builder.withString(e2, "value3");
        final Record record = builder.build();
        Assertions.assertNotNull(record);

        Assertions.assertEquals("value3", record.getString("_0001"));
        Assertions.assertEquals(2, record.getSchema().getEntries().size());
    }

    @Test
    void testRecordWithNewSchema() {
        final Schema schema0 = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(dataEntry1) //
                .withEntryBefore("data1", meta1) //
                .withEntry(dataEntry2) //
                .withEntryAfter("meta1", meta2) //
                .build();
        final RecordImpl.BuilderImpl builder0 = new RecordImpl.BuilderImpl(schema0);
        builder0.withInt("data1", 101)
                .withString("data2", "102")
                .withInt("meta1", 103)
                .withString("meta2", "104");
        final Record record0 = builder0.build();
        assertEquals(101, record0.getInt("data1"));
        assertEquals("102", record0.getString("data2"));
        assertEquals(103, record0.getInt("meta1"));
        assertEquals("104", record0.getString("meta2"));
        assertEquals("meta1,meta2,data1,data2", getSchemaFields(record0.getSchema()));
        assertEquals("103,104,101,102", getRecordValues(record0));
        // get a new schema from record
        final Schema schema1 = record0
                .getSchema() //
                .toBuilder() //
                .withEntryBefore("data1", newMetaEntry("meta3", Type.STRING)) //
                .withEntryAfter("meta3", newEntry("data3", Type.STRING)) //
                .build();
        assertEquals("meta1,meta2,meta3,data3,data1,data2", getSchemaFields(schema1));
        // test new record1
        final Record record1 = record0 //
                .withNewSchema(schema1) //
                .withString("data3", "data3") //
                .withString("meta3", "meta3") //
                .build();
        assertEquals(101, record1.getInt("data1"));
        assertEquals("102", record1.getString("data2"));
        assertEquals(103, record1.getInt("meta1"));
        assertEquals("104", record1.getString("meta2"));
        assertEquals("data3", record1.getString("data3"));
        assertEquals("meta3", record1.getString("meta3"));
        assertEquals("meta1,meta2,meta3,data3,data1,data2", getSchemaFields(record1.getSchema()));
        assertEquals("103,104,meta3,data3,101,102", getRecordValues(record1));
        // remove latest additions
        final Schema schema2 = record1
                .getSchema()
                .toBuilder()
                .withEntryBefore("data1", newEntry("data0", Type.STRING))
                .withEntryBefore("meta1", newEntry("meta0", Type.STRING))
                .remove("data3")
                .remove("meta3")
                .build();
        assertEquals("meta0,meta1,meta2,data0,data1,data2", getSchemaFields(schema2));
        final Record record2 = record1 //
                .withNewSchema(schema2) //
                .withString("data0", "data0") //
                .withString("meta0", "meta0") //
                .build();
        assertEquals("meta0,103,104,data0,101,102", getRecordValues(record2));
    }

    private String getSchemaFields(final Schema schema) {
        return schema.getEntriesOrdered().stream().map(e -> e.getName()).collect(joining(","));
    }

    private String getRecordValues(final Record record) {
        return record
                .getSchema()
                .getEntriesOrdered()
                .stream()
                .map(e -> record.get(String.class, e.getName()))
                .collect(joining(","));
    }

    private final Schema.Entry dataEntry1 = new SchemaImpl.Entry.Builder() //
            .withName("data1") //
            .withType(Schema.Type.INT) //
            .build();

    private final Schema.Entry dataEntry2 = new SchemaImpl.Entry.Builder() //
            .withName("data2") //
            .withType(Schema.Type.STRING) //
            .withNullable(true) //
            .build();

    private final Schema.Entry meta1 = new SchemaImpl.Entry.Builder() //
            .withName("meta1") //
            .withType(Schema.Type.INT) //
            .withMetadata(true) //
            .build();

    private final Schema.Entry meta2 = new SchemaImpl.Entry.Builder() //
            .withName("meta2") //
            .withType(Schema.Type.STRING) //
            .withMetadata(true) //
            .withNullable(true) //
            .build();

    private Entry newEntry(final String name, String rawname, Schema.Type type, boolean nullable, Object defaultValue,
            String comment) {
        return new Entry.Builder()
                .withName(name)
                .withRawName(rawname)
                .withType(type)
                .withNullable(nullable)
                .withDefaultValue(defaultValue)
                .withComment(comment)
                .build();
    }

    private Entry newEntry(final String name, Schema.Type type) {
        return newEntry(name, name, type, true, "", "");
    }

    private Entry newMetaEntry(final String name, String rawname, Schema.Type type, boolean nullable,
            Object defaultValue, String comment) {
        return new Entry.Builder()
                .withName(name)
                .withRawName(rawname)
                .withType(type)
                .withNullable(nullable)
                .withDefaultValue(defaultValue)
                .withComment(comment)
                .withMetadata(true)
                .build();
    }

    private Entry newMetaEntry(final String name, Schema.Type type) {
        return newMetaEntry(name, name, type, true, "", "");
    }

}
