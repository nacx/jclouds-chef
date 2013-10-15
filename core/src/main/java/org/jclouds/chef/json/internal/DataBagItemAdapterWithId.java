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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import javax.inject.Singleton;

import org.jclouds.chef.domain.DatabagItem;
import org.jclouds.chef.json.DatabagItemAdapter;
import org.jclouds.json.internal.NullHackJsonLiteralAdapter;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Custom serialization and deserialization logic for data bags.
 * 
 * @author Ignasi Barrera
 */
@Singleton
public class DataBagItemAdapterWithId extends NullHackJsonLiteralAdapter<DatabagItem> implements DatabagItemAdapter {
   // Cannot use Guice to inject Gson because it is still not configured
   final Gson gson = new Gson();

   @Override
   protected DatabagItem createJsonLiteralFromRawJson(String text) {
      IdHolder idHolder = gson.fromJson(text, IdHolder.class);
      checkState(idHolder.id != null,
            "databag item must be a json hash ex. {\"id\":\"item1\",\"my_key\":\"my_data\"}; was %s", text);
      text = text.replaceFirst(String.format("\\{\"id\"[ ]?:\"%s\",", idHolder.id), "{");
      return new DatabagItem(idHolder.id, text);
   }

   @Override
   protected String toString(DatabagItem value) {
      String text = value.toString();

      try {
         IdHolder idHolder = gson.fromJson(text, IdHolder.class);
         if (idHolder.id == null) {
            text = text.replaceFirst("\\{", String.format("{\"id\":\"%s\",", value.getId()));
         } else {
            checkArgument(value.getId().equals(idHolder.id), "incorrect id in databagItem text, should be %s: was %s",
                  value.getId(), idHolder.id);
         }
      } catch (JsonSyntaxException e) {
         throw new IllegalArgumentException(e);
      }

      return text;
   }

   private static class IdHolder {
      private String id;
   }
}
