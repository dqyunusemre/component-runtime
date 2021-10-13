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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.runtime.record.SchemaImpl.BuilderImpl;
import org.talend.sdk.component.runtime.record.SchemaImpl.EntryImpl;

class RecordBuilderImplTest {

    @Test
    void providedSchemaGetSchema() {
        final Schema schema = new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
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
        final Entry entry = new EntryImpl.BuilderImpl() //
                .withName("name") //
                .withNullable(true) //
                .withType(Type.STRING) //
                .build();//
        assertThrows(IllegalArgumentException.class, () -> builder.with(entry, 234L));

        builder.with(entry, "value");
        Assertions.assertEquals("value", builder.getValue("name"));

        final Entry entryTime = new EntryImpl.BuilderImpl() //
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
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
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
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
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
        { // entry not nullable
            Entry notNull = newEntry("name2", "name2", Type.STRING, false, "", "");
            assertThrows(IllegalArgumentException.class, () -> builder.get().withString(notNull, null).build());
        }
        { // invalid type entry
            assertThrows(IllegalArgumentException.class, () -> builder.get().withInt("name", 2).build());
        }
    }

    @Test
    void providedSchemaNotNullable() {
        final Supplier<RecordImpl.BuilderImpl> builder = () -> new RecordImpl.BuilderImpl(new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
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
        final Entry arrayEntry = new EntryImpl.BuilderImpl() //
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
                .withString(new SchemaImpl.EntryImpl.BuilderImpl().withNullable(false).withName("test").build(), null));
    }

    @Test
    void dateTime() {
        final Schema schema = new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
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
        final Schema.Entry entry = new SchemaImpl.EntryImpl.BuilderImpl()
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
                .withEntry(new EntryImpl.BuilderImpl().withName("f01").withType(Type.STRING).build())
                .withEntry(
                        new EntryImpl.BuilderImpl().withName("f02").withType(Type.STRING).withProps(fieldProps).build())
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
                .withEntry(new EntryImpl.BuilderImpl()
                        .withName("f01")
                        .withType(Type.STRING)
                        .withProp("dqType", "semantic-test1")
                        .build())
                .withEntry(new EntryImpl.BuilderImpl()
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
        assertEquals(2, rSchema.getProps().size());
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
                .withEntry(new EntryImpl.BuilderImpl()
                        .withName("f01")
                        .withType(Type.STRING)
                        .withProp("dqType", "semantic-test1")
                        .withProps(fieldProps)
                        .build())
                .withEntry(new EntryImpl.BuilderImpl()
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
        assertEquals(11, rSchema.getProps().size());
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
                .withEntry(new EntryImpl.BuilderImpl()
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
        assertEquals(2, entries.size());

        final Entry entry = entries.stream().filter((Entry e) -> "field1".equals(e.getName())).findFirst().get();
        builder.removeEntry(entry);
        assertEquals(1, builder.getCurrentEntries().size());
        assertTrue(entries.stream().anyMatch((Entry e) -> "fieldInt".equals(e.getName())));

        Schema.Entry unknownEntry = newEntry("fieldUnknown", "fieldUnknown", Type.STRING, true, "unknown", "Comment");
        assertThrows(IllegalArgumentException.class, () -> builder.removeEntry(unknownEntry));

        final Schema schema = new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(newEntry("field1", "field1", Type.INT, true, 5, "Comment"))
                .withEntry(newMetaEntry("meta1", "meta1", Type.INT, true, 5, "Comment"))
                .build();
        final RecordImpl.BuilderImpl builder1 = new RecordImpl.BuilderImpl(schema);
        final List<Entry> entries1 = builder1.getCurrentEntries();
        assertEquals(2, entries1.size());
        final Entry entry1 = entries1.stream().filter((Entry e) -> "field1".equals(e.getName())).findFirst().get();
        Record.Builder newBuilder = builder1.removeEntry(entry1);
        final Entry meta1 = entries1.stream().filter((Entry e) -> "meta1".equals(e.getName())).findFirst().get();
        Record.Builder newBuilder2 = newBuilder.removeEntry(meta1);
        assertEquals(0, newBuilder2.getCurrentEntries().size());
    }

    @Test
    void updateEntryByName_fromEntries() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder.withString("field1", "Hello").withInt("fieldInt", 20);
        final List<Entry> entries = builder.getCurrentEntries();
        assertEquals(2, entries.size());

        final Entry entry = newEntry("field2", "newFieldName", Type.STRING, true, 5, "Comment");
        builder.updateEntryByName("field1", entry);
        Assertions.assertEquals(2, builder.getCurrentEntries().size());
        assertTrue(entries
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
        assertTrue(newBuilder
                .getCurrentEntries()
                .stream()
                .anyMatch((Entry e) -> "field2".equals(e.getName()) && "newFieldName".equals(e.getRawName())
                        && Type.STRING.equals(e.getType())));
        assertEquals("10", newBuilder.getValue("field2"));
    }

    private Entry newEntry(final String name, String rawname, Schema.Type type, boolean nullable, Object defaultValue,
            String comment) {
        return new EntryImpl.BuilderImpl()
                .withName(name)
                .withRawName(rawname)
                .withType(type)
                .withNullable(nullable)
                .withDefaultValue(defaultValue)
                .withComment(comment)
                .build();
    }

    private Entry newMetaEntry(final String name, String rawname, Schema.Type type, boolean nullable,
            Object defaultValue, String comment) {
        return new EntryImpl.BuilderImpl()
                .withName(name)
                .withRawName(rawname)
                .withType(type)
                .withNullable(nullable)
                .withDefaultValue(defaultValue)
                .withComment(comment)
                .withMetadata(true)
                .build();
    }

    @Test
    void testInsertBeforeWithProvidedSchema() {
        testInsertBefore(createInsertSchema());
    }

    @Test
    void testInsertBeforeWithoutProvidedSchema() {
        testInsertBefore(null);
    }

    @Test
    void testInsertAfterWithProvidedSchema() {
        testInsertAfter(createInsertSchema());
    }

    @Test
    void testInsertAfterWithoutProvidedSchema() {
        testInsertAfter(null);
    }

    @Test
    void testMoveBefore() {
        final RecordImpl.BuilderImpl builder = createDefaultBuilder(null);
        assertEquals("f1,f2,f3,f4", getCurrentSchema(builder));
        builder.moveBefore("f1", "f4");
        assertEquals("f4,f1,f2,f3", getCurrentSchema(builder));
        builder.moveBefore("f1", "f3");
        assertEquals("f4,f3,f1,f2", getCurrentSchema(builder));
        builder.moveBefore("f1", "f2");
        assertEquals("f4,f3,f2,f1", getCurrentSchema(builder));
        // self move
        builder.moveBefore("f1", "f1");
        assertEquals("f4,f3,f2,f1", getCurrentSchema(builder));
        builder.moveBefore("f4", "f4");
        assertEquals("f4,f3,f2,f1", getCurrentSchema(builder));
        Record record = builder.build();
        assertEquals("4,3,2,1", getRecordValues(record));
    }

    @Test
    void testMoveAfter() {
        final RecordImpl.BuilderImpl builder = createDefaultBuilder(createInsertSchema());
        assertEquals("m1,f1,f2,f3,f4", getCurrentSchema(builder));
        builder.moveAfter("f1", "f4");
        assertEquals("m1,f1,f4,f2,f3", getCurrentSchema(builder));
        builder.moveAfter("f1", "f3");
        assertEquals("m1,f1,f3,f4,f2", getCurrentSchema(builder));
        builder.moveAfter("f1", "f2");
        assertEquals("m1,f1,f2,f3,f4", getCurrentSchema(builder));
        // self move
        builder.moveAfter("f1", "f1");
        assertEquals("m1,f1,f2,f3,f4", getCurrentSchema(builder));
        builder.moveAfter("f4", "f1");
        assertEquals("m1,f2,f3,f4,f1", getCurrentSchema(builder));
        Record record = builder.build();
        assertEquals("2,3,4,1", getRecordValues(record));
    }

    @Test
    void testSwap() {
        final RecordImpl.BuilderImpl builder = createDefaultBuilder(null);
        assertEquals("f1,f2,f3,f4", getCurrentSchema(builder));
        builder.swap("f1", "f4");
        assertEquals("f4,f2,f3,f1", getCurrentSchema(builder));
        builder.swap("f1", "f3");
        assertEquals("f4,f2,f1,f3", getCurrentSchema(builder));
        builder.swap("f1", "f2");
        assertEquals("f4,f1,f2,f3", getCurrentSchema(builder));
        // self move
        builder.swap("f1", "f1");
        assertEquals("f4,f1,f2,f3", getCurrentSchema(builder));
        builder.swap("f4", "f1");
        assertEquals("f1,f4,f2,f3", getCurrentSchema(builder));
        Record record = builder.build();
        assertEquals("1,4,2,3", getAllRecordValues(record));
    }

    @Test
    void testMixedOperation() {
        final RecordImpl.BuilderImpl builder = createDefaultBuilder(createInsertSchema());
        Entry f5 = newMetaEntry("f5", "f5", Type.STRING, false, "", "");
        Entry f6 = newMetaEntry("f6", "", Type.STRING, true, "", "");
        Entry m2 = newMetaEntry("m2", "Columns Checks", Type.INT, true, 102, "");
        Entry m3 = newMetaEntry("m3", "Columns Checks", Type.INT, true, 103, "");
        //
        builder.withInt("m1", 101);
        assertEquals("m1,f1,f2,f3,f4", getCurrentSchema(builder));
        builder.swap("m1", "f3");
        assertEquals("f3,f1,f2,m1,f4", getCurrentSchema(builder));
        builder.insertAfter("m1", m2, 102);
        assertEquals("f3,f1,f2,m1,m2,f4", getCurrentSchema(builder));
        builder.removeEntry(m2);
        assertEquals("f3,f1,f2,m1,f4", getCurrentSchema(builder));
        builder.with(m2, 102);
        assertEquals("f3,f1,f2,m1,f4,m2", getCurrentSchema(builder));
        builder.moveBefore("f3", "m2");
        assertEquals("m2,f3,f1,f2,m1,f4", getCurrentSchema(builder));
        builder.updateEntryByName("m2", m3);
        assertNull(builder.getValue("m2"));
        assertEquals(102, builder.getValue("m3"));
        builder.withInt("m3", 103);
        assertEquals(103, builder.getValue("m3"));
        assertEquals("m3,f3,f1,f2,m1,f4", getCurrentSchema(builder));
        assertThrows(IllegalArgumentException.class, () -> builder.insertBefore("f2", f5, null));
        assertThrows(IllegalArgumentException.class, () -> builder.insertAfter("f2", f5, null));
        assertEquals("m3,f3,f1,f2,m1,f4", getCurrentSchema(builder));
        builder.insertBefore("m3", f5, "5");
        assertEquals("f5,m3,f3,f1,f2,m1,f4", getCurrentSchema(builder));
        builder.moveBefore("f5", "m1");
        assertEquals("m1,f5,m3,f3,f1,f2,f4", getCurrentSchema(builder));
        builder.moveAfter("m1", "m3");
        assertEquals("m1,m3,f5,f3,f1,f2,f4", getCurrentSchema(builder));
        builder.insertAfter("m1", m2, 102);
        assertEquals("m1,m2,m3,f5,f3,f1,f2,f4", getCurrentSchema(builder));
        builder.moveBefore("f5", "f1");
        assertEquals("m1,m2,m3,f1,f5,f3,f2,f4", getCurrentSchema(builder));
        builder.moveAfter("f1", "f2");
        assertEquals("m1,m2,m3,f1,f2,f5,f3,f4", getCurrentSchema(builder));
        builder.removeEntry(f5);
        assertEquals("m1,m2,m3,f1,f2,f3,f4", getCurrentSchema(builder));
        // record checks
        Record record = builder.build();
        assertEquals(101, record.getInt("m1"));
        assertEquals(102, record.getInt("m2"));
        assertEquals(103, record.getInt("m3"));
        assertEquals("1,2,3,4", getRecordValues(record));
        assertEquals("101,102,103,1,2,3,4", getAllRecordValues(record));
    }

    private String getCurrentSchema(final RecordImpl.BuilderImpl builder) {
        return builder.getCurrentEntries().stream().map(entry -> entry.getName()).collect(Collectors.joining(","));
    }

    private String getRecordValues(final Record record) {
        return record
                .getSchema()
                .getEntries()
                .stream()
                .map(e -> e.getName())
                .map(f -> record.getString(f))
                .collect(Collectors.joining(","));
    }

    private String getAllRecordValues(final Record record) {
        return record
                .getSchema()
                .getAllEntries()
                .map(e -> e.getName())
                .map(f -> record.getString(f))
                .collect(Collectors.joining(","));
    }

    private Schema createInsertSchema() {
        return new BuilderImpl() //
                .withType(Type.RECORD) //
                .withEntry(newEntry("f1", "f1", Type.STRING, true, 5, "Comment"))
                .withEntry(newEntry("f2", "f2", Type.STRING, true, 5, "Comment"))
                .withEntry(newEntry("f3", "f3", Type.STRING, true, 5, "Comment"))
                .withEntry(newEntry("f4", "f4", Type.STRING, true, 5, "Comment"))
                .withEntry(newMetaEntry("m1", "m1", Type.INT, true, 101, "metadata"))
                .withProp("rootProp1", "rootPropValue1")
                .withProp("rootProp2", "rootPropValue2")
                .withProp("rootProp3", "rootPropValue3")
                .build();
    }

    private RecordImpl.BuilderImpl createDefaultBuilder(final Schema schema) {
        final RecordImpl.BuilderImpl builder =
                schema == null ? new RecordImpl.BuilderImpl() : new RecordImpl.BuilderImpl(schema);
        builder.withString("f1", "1");
        builder.withString("f2", "2");
        builder.withString("f3", "3");
        builder.withString("f4", "4");

        return builder;
    }

    void testInsertBefore(final Schema schema) {
        final RecordImpl.BuilderImpl builder = createDefaultBuilder(schema);
        final Entry e5 = new EntryImpl.BuilderImpl().withName("f5").withType(Type.STRING).build();
        final Entry e6 = new EntryImpl.BuilderImpl().withName("f6").withType(Type.STRING).build();
        builder.insertBefore("f1", e5, "5");
        builder.insertBefore("f5", e6, "6");
        Record record = builder.build();
        assertEquals("6,5,1,2,3,4", getRecordValues(record));
    }

    void testInsertAfter(final Schema schema) {
        final RecordImpl.BuilderImpl builder = createDefaultBuilder(schema);
        final Entry e5 = new EntryImpl.BuilderImpl().withName("f5").withType(Type.STRING).build();
        final Entry e6 = new EntryImpl.BuilderImpl().withName("f6").withType(Type.STRING).build();
        final Entry e7 = new EntryImpl.BuilderImpl().withName("f7").withType(Type.STRING).build();
        builder.insertAfter("f1", e5, "5");
        builder.insertAfter("f5", e6, "6");
        builder.insertAfter("f4", e7, "7");
        Record record = builder.build();
        assertEquals("1,5,6,2,3,4,7", getRecordValues(record));
    }

    void testMoveBefore(final Schema schema) {
        final RecordImpl.BuilderImpl builder = createDefaultBuilder(schema);
    }

    void testMoveAfter(final Schema schema) {
        final RecordImpl.BuilderImpl builder = createDefaultBuilder(schema);
    }

}
