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
package org.talend.sdk.component.api.record;

import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.time.temporal.Temporal;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.bind.annotation.JsonbTransient;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

public interface Schema {

    /**
     * @return the type of this schema.
     */
    Type getType();

    /**
     * @return the nested element schema for arrays.
     */
    Schema getElementSchema();

    /**
     * @return the data entries for records (not contains meta data entries).
     */
    List<Entry> getEntries();

    /**
     * @return the metadata entries for records (not contains ordinary data entries).
     */
    List<Entry> getMetadata();

    /**
     * @return All entries, including data and metadata, of this schema.
     */
    Stream<Entry> getAllEntries();

    /**
     * Get a Builder from the current schema.
     *
     * @return a {@link Schema.Builder}
     */
    Schema.Builder toBuilder();

    /**
     * Get all entries sorted by schema designed order.
     *
     * @return all entries ordered
     */
    List<Entry> getEntriesOrdered();

    /**
     * Get all entries sorted using a custom comparator.
     *
     * @param comparator the comparator
     * @return all entries ordered with provided comparator
     */

    List<Entry> getEntriesOrdered(Comparator<Entry> comparator);

    /**
     * Get all entries sorted using a custom EntriesOrder .
     *
     * @param entriesOrder
     * @return all entries ordered with provided EntriesOrder
     */
    List<Entry> getEntriesOrdered(EntriesOrder entriesOrder);

    /**
     * Get the EntriesOrder defined with Builder.
     *
     * @return the EntriesOrder
     */

    EntriesOrder naturalOrder();

    default Entry getEntry(final String name) {
        return Optional
                .ofNullable(getEntries()) //
                .orElse(Collections.emptyList()) //
                .stream() //
                .filter((Entry e) -> Objects.equals(e.getName(), name)) //
                .findFirst() //
                .orElse(null);
    }

    /**
     * @return the metadata props
     */
    Map<String, String> getProps();

    /**
     * @param property : property name.
     * @return the requested metadata prop
     */
    String getProp(String property);

    /**
     * Get a property values from schema with its name.
     * 
     * @param name : property's name.
     * @return property's value.
     */
    default JsonValue getJsonProp(final String name) {
        final String prop = this.getProp(name);
        if (prop == null) {
            return null;
        }
        try {
            return Json.createParser(new StringReader(prop)).getValue();
        } catch (RuntimeException ex) {
            return Json.createValue(prop);
        }
    }

    enum Type {

        RECORD(new Class<?>[] { Record.class }),
        ARRAY(new Class<?>[] { Collection.class }),
        STRING(new Class<?>[] { String.class }),
        BYTES(new Class<?>[] { byte[].class, Byte[].class }),
        INT(new Class<?>[] { Integer.class }),
        LONG(new Class<?>[] { Long.class }),
        FLOAT(new Class<?>[] { Float.class }),
        DOUBLE(new Class<?>[] { Double.class }),
        BOOLEAN(new Class<?>[] { Boolean.class }),
        DATETIME(new Class<?>[] { Long.class, Date.class, Temporal.class });

        /** All compatibles Java classes */
        private final Class<?>[] classes;

        Type(final Class<?>[] classes) {
            this.classes = classes;
        }

        /**
         * Check if input can be affected to an entry of this type.
         * 
         * @param input : object.
         * @return true if input is null or ok.
         */
        public boolean isCompatible(final Object input) {
            if (input == null) {
                return true;
            }
            for (final Class<?> clazz : classes) {
                if (clazz.isInstance(input)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Getter
    @EqualsAndHashCode
    @ToString
    class Entry {

        private Entry(final Entry.Builder builder) {
            this.name = builder.name;
            this.rawName = builder.rawName;
            this.type = builder.type;
            this.nullable = builder.nullable;
            this.metadata = builder.metadata;
            this.defaultValue = builder.defaultValue;
            this.elementSchema = builder.elementSchema;
            this.comment = builder.comment;
            this.props.putAll(builder.props);
        }

        /**
         * The name of this entry.
         */
        private final String name;

        /**
         * The raw name of this entry.
         */
        private final String rawName;

        /**
         * Type of the entry, this determine which other fields are populated.
         */
        private final Schema.Type type;

        /**
         * Is this entry nullable or always valued.
         */
        private final boolean nullable;

        private final boolean metadata;

        /**
         * Default value for this entry.
         */
        private final Object defaultValue;

        /**
         * For type == record, the element type.
         */
        private final Schema elementSchema;

        /**
         * Allows to associate to this field a comment - for doc purposes, no use in the runtime.
         */
        private final String comment;

        /**
         * metadata
         */
        private final Map<String, String> props = new LinkedHashMap<>(0);

        @JsonbTransient
        public String getOriginalFieldName() {
            return rawName != null ? rawName : name;
        }

        /**
         * @param property : property name.
         * @return the requested metadata prop
         */
        public String getProp(final String property) {
            return this.props.get(property);
        }

        /**
         * Get a property values from entry with its name.
         *
         * @param name : property's name.
         * @return property's value.
         */
        public JsonValue getJsonProp(final String name) {
            final String prop = this.getProp(name);
            if (prop == null) {
                return null;
            }
            try {
                return Json.createParser(new StringReader(prop)).getValue();
            } catch (RuntimeException ex) {
                return Json.createValue(prop);
            }
        }

        /**
         * @return Entry builder from this entry.
         */
        public Entry.Builder toBuilder() {
            return new Entry.Builder(this);
        }

        // Map<String, Object> metadata <-- DON'T DO THAT, ENSURE ANY META IS TYPED!

        /**
         * Plain builder matching {@link Entry} structure.
         */
        public static class Builder {

            private String name;

            private String rawName;

            private Schema.Type type;

            private boolean nullable;

            private boolean metadata = false;

            private Object defaultValue;

            private Schema elementSchema;

            private String comment;

            private final Map<String, String> props = new LinkedHashMap<>(0);

            public Builder() {
            }

            private Builder(final Entry entry) {
                this.name = entry.name;
                this.rawName = entry.rawName;
                this.nullable = entry.nullable;
                this.type = entry.type;
                this.comment = entry.comment;
                this.elementSchema = entry.elementSchema;
                this.defaultValue = entry.defaultValue;
                this.metadata = entry.metadata;
                this.props.putAll(entry.props);
            }

            public Builder withName(final String name) {
                this.name = sanitizeConnectionName(name);
                // if raw name is changed as follow name rule, use label to store raw name
                // if not changed, not set label to save space
                if (!name.equals(this.name)) {
                    this.rawName = name;
                }
                return this;
            }

            public Builder withRawName(final String rawName) {
                this.rawName = rawName;
                return this;
            }

            public Builder withType(final Type type) {
                this.type = type;
                return this;
            }

            public Builder withNullable(final boolean nullable) {
                this.nullable = nullable;
                return this;
            }

            public Builder withMetadata(final boolean metadata) {
                this.metadata = metadata;
                return this;
            }

            public <T> Builder withDefaultValue(final T value) {
                defaultValue = value;
                return this;
            }

            public Builder withElementSchema(final Schema schema) {
                elementSchema = schema;
                return this;
            }

            public Builder withComment(final String comment) {
                this.comment = comment;
                return this;
            }

            public Builder withProp(final String key, final String value) {
                props.put(key, value);
                return this;
            }

            public Builder withProps(final Map props) {
                if (props == null) {
                    return this;
                }
                this.props.putAll(props);
                return this;
            }

            public Entry build() {
                return new Entry(this);
            }

        }
    }

    /**
     * Allows to build a schema.
     */
    interface Builder {

        /**
         * @param type schema type.
         * @return this builder.
         */
        Builder withType(Type type);

        /**
         * @param entry element for either an array or record type.
         * @return this builder.
         */
        Builder withEntry(Entry entry);

        /**
         * Insert the entry after the specified entry.
         *
         * @param after the entry name reference
         * @param entry the entry name
         * @return this builder
         */
        Builder withEntryAfter(String after, Entry entry);

        /**
         * Insert the entry before the specified entry.
         *
         * @param before the entry name reference
         * @param entry the entry name
         * @return this builder
         */
        Builder withEntryBefore(String before, Entry entry);

        /**
         * Remove entry from builder.
         *
         * @param name the entry name
         * @return this builder
         */
        Builder remove(String name);

        /**
         * Remove entry from builder.
         *
         * @param entry the entry
         * @return this builder
         */
        Builder remove(Entry entry);

        /**
         * Move an entry after another one.
         *
         * @param after the entry name reference
         * @param name the entry name
         */
        Builder moveAfter(final String after, final String name);

        /**
         * Move an entry before another one.
         *
         * @param before the entry name reference
         * @param name the entry name
         */
        Builder moveBefore(final String before, final String name);

        /**
         * Swap two entries.
         *
         * @param name the entry name
         * @param with the other entry name
         */
        Builder swap(final String name, final String with);

        /**
         * @param schema nested element schema.
         * @return this builder.
         */
        Builder withElementSchema(Schema schema);

        /**
         * @param props schema properties
         * @return this builder
         */
        Builder withProps(Map<String, String> props);

        /**
         *
         * @param key the prop key name
         * @param value the prop value
         * @return this builder
         */
        Builder withProp(String key, String value);

        /**
         * @return the described schema.
         */
        Schema build();
    }

    /**
     * Sanitize name to be avro compatible.
     * 
     * @param name : original name.
     * @return avro compatible name.
     */
    static String sanitizeConnectionName(final String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        char current = name.charAt(0);
        final CharsetEncoder ascii = Charset.forName(StandardCharsets.US_ASCII.name()).newEncoder();
        final boolean skipFirstChar = ((!ascii.canEncode(current)) || (!Character.isLetter(current) && current != '_'))
                && name.length() > 1 && (!Character.isDigit(name.charAt(1)));

        final StringBuilder sanitizedBuilder = new StringBuilder();

        if (!skipFirstChar) {
            if (((!Character.isLetter(current)) && current != '_') || (!ascii.canEncode(current))) {
                sanitizedBuilder.append('_');
            } else {
                sanitizedBuilder.append(current);
            }
        }
        for (int i = 1; i < name.length(); i++) {
            current = name.charAt(i);
            if (!ascii.canEncode(current)) {
                if (Character.isLowerCase(current) || Character.isUpperCase(current)) {
                    sanitizedBuilder.append('_');
                } else {
                    final byte[] encoded =
                            Base64.getEncoder().encode(name.substring(i, i + 1).getBytes(StandardCharsets.UTF_8));
                    final String enc = new String(encoded);
                    if (sanitizedBuilder.length() == 0 && Character.isDigit(enc.charAt(0))) {
                        sanitizedBuilder.append('_');
                    }
                    for (int iter = 0; iter < enc.length(); iter++) {
                        if (Character.isLetterOrDigit(enc.charAt(iter))) {
                            sanitizedBuilder.append(enc.charAt(iter));
                        } else {
                            sanitizedBuilder.append('_');
                        }
                    }
                }
            } else if (Character.isLetterOrDigit(current)) {
                sanitizedBuilder.append(current);
            } else {
                sanitizedBuilder.append('_');
            }

        }
        return sanitizedBuilder.toString();
    }

    interface EntriesOrder extends Comparator<Entry> {

        List<String> getFieldsOrder();

        default String toFields() {
            return getFieldsOrder().stream().collect(Collectors.joining(","));
        }

        /**
         * Move a field after another one.
         *
         * @param after the field name reference
         * @param name the field name
         * @return this EntriesOrder
         */
        EntriesOrder moveAfter(final String after, final String name);

        /**
         * Move a field before another one.
         *
         * @param before the field name reference
         * @param name the field name
         * @return this EntriesOrder
         */
        EntriesOrder moveBefore(final String before, final String name);

        /**
         * Swap two fields.
         *
         * @param name the field name
         * @param with the other field
         * @return this EntriesOrder
         */
        EntriesOrder swap(final String name, final String with);

        @Override
        default int compare(final Entry e1, final Entry e2) {
            final String name1 = e1.getName();
            final String name2 = e2.getName();
            if (getFieldsOrder().contains(name1) && getFieldsOrder().contains(name2)) {
                return getFieldsOrder().indexOf(name1) - getFieldsOrder().indexOf(name2);
            }
            if (getFieldsOrder().contains(name1)) {
                return -1;
            }
            if (getFieldsOrder().contains(name2)) {
                return 1;
            }
            return 0;
        }
    }

    static Schema.Entry avoidCollision(final Schema.Entry newEntry,
            final Supplier<Stream<Schema.Entry>> allEntriesSupplier, final BiConsumer<String, Entry> replaceFunction) {
        final Optional<Entry> collisionedEntry = allEntriesSupplier //
                .get() //
                .filter((final Entry field) -> field.getName().equals(newEntry.getName())
                        && !Objects.equals(field, newEntry)) //
                .findFirst();
        if (!collisionedEntry.isPresent()) {
            // No collision, return new entry.
            return newEntry;
        }
        final Entry matchedEntry = collisionedEntry.get();
        final boolean matchedToChange = matchedEntry.getRawName() != null && !(matchedEntry.getRawName().isEmpty());
        if (matchedToChange) {
            // the rename has to be applied on entry already inside schema, so replace.
            replaceFunction.accept(matchedEntry.getName(), newEntry);
        } else if (newEntry.getRawName() == null || newEntry.getRawName().isEmpty()) {
            // try to add exactly same raw, skip the add here.
            return null;
        }
        final Entry fieldToChange = matchedToChange ? matchedEntry : newEntry;
        int indexForAnticollision = 1;
        final String baseName = Schema.sanitizeConnectionName(fieldToChange.rawName); // recalc primiti name.

        String newName = baseName + "_" + indexForAnticollision;
        final Set<String> existingNames = allEntriesSupplier //
                .get() //
                .map(Entry::getName) //
                .collect(Collectors.toSet());
        while (existingNames.contains(newName)) {
            indexForAnticollision++;
            newName = baseName + "_" + indexForAnticollision;
        }
        final Entry newFieldToAdd = fieldToChange.toBuilder().withName(newName).build();

        return newFieldToAdd; // matchedToChange ? newFieldToAdd : newEntry;
    }
}
