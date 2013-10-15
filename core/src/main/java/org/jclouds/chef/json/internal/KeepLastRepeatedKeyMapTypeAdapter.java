/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.chef.json.internal;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Objects.toStringHelper;

import java.io.IOException;
import java.util.Map;

import org.jclouds.json.internal.NullFilteringTypeAdapterFactories.MapTypeAdapterFactory;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.JsonReaderInternalAccess;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Custom map type adapter to prevent failures if there are repeated keys.
 * <p>
 * Some cookbooks are returned by the Chef server API with duplicate keys, being
 * the last one the one with the desired value. This parser just keeps the last
 * key when there are repeated ones.
 * <p>
 * The NullFilteringTypeAdapterFactories.MapTypeAdapter class is final, so we
 * are doing the same logic here.
 * 
 * @author Ignasi Barrera
 */
public class KeepLastRepeatedKeyMapTypeAdapter<K, V> extends TypeAdapter<Map<K, V>> {

   public static class Factory extends MapTypeAdapterFactory {
      public Factory() {
         super(Map.class);
      }

      @SuppressWarnings("unchecked")
      @Override
      protected <K, V, T> TypeAdapter<T> newAdapter(TypeAdapter<K> keyAdapter, TypeAdapter<V> valueAdapter) {
         return (TypeAdapter<T>) new KeepLastRepeatedKeyMapTypeAdapter<K, V>(keyAdapter, valueAdapter);
      }
   }

   protected final TypeAdapter<K> keyAdapter;
   protected final TypeAdapter<V> valueAdapter;

   protected KeepLastRepeatedKeyMapTypeAdapter(TypeAdapter<K> keyAdapter, TypeAdapter<V> valueAdapter) {
      this.keyAdapter = keyAdapter;
      this.valueAdapter = valueAdapter;
      nullSafe();
   }

   public void write(JsonWriter out, Map<K, V> value) throws IOException {
      if (value == null) {
         out.nullValue();
         return;
      }
      out.beginObject();
      for (Map.Entry<K, V> element : value.entrySet()) {
         out.name(String.valueOf(element.getKey()));
         valueAdapter.write(out, element.getValue());
      }
      out.endObject();
   }

   public Map<K, V> read(JsonReader in) throws IOException {
      Map<K, V> result = Maps.newHashMap();
      in.beginObject();
      while (in.hasNext()) {
         JsonReaderInternalAccess.INSTANCE.promoteNameToValue(in);
         K name = keyAdapter.read(in);
         V value = valueAdapter.read(in);
         if (value != null) {
            // If there are repeated keys, overwrite them to only keep the last
            // one
            result.put(name, value);
         }
      }
      in.endObject();
      return ImmutableMap.copyOf(result);
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(keyAdapter, valueAdapter);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null || getClass() != obj.getClass())
         return false;
      KeepLastRepeatedKeyMapTypeAdapter<?, ?> that = KeepLastRepeatedKeyMapTypeAdapter.class.cast(obj);
      return equal(this.keyAdapter, that.keyAdapter) && equal(this.valueAdapter, that.valueAdapter);
   }

   @Override
   public String toString() {
      return toStringHelper(this).add("keyAdapter", keyAdapter).add("valueAdapter", valueAdapter).toString();
   }
}