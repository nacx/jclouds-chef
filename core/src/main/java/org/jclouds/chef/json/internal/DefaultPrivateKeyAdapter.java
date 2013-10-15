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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jclouds.chef.json.PrivateKeyAdapter;
import org.jclouds.crypto.Crypto;
import org.jclouds.crypto.Pems;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

@Singleton
public class DefaultPrivateKeyAdapter implements PrivateKeyAdapter {
   private final Crypto crypto;

   @Inject
   DefaultPrivateKeyAdapter(Crypto crypto) {
      this.crypto = checkNotNull(crypto, "crypto");
   }

   @Override
   public PrivateKey deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
         throws JsonParseException {
      String keyText = json.getAsString().replaceAll("\\n", "\n");
      try {
         return crypto.rsaKeyFactory().generatePrivate(
               Pems.privateKeySpec(ByteStreams.newInputStreamSupplier(keyText.getBytes(Charsets.UTF_8))));
      } catch (UnsupportedEncodingException e) {
         throw Throwables.propagate(e);
      } catch (InvalidKeySpecException e) {
         throw Throwables.propagate(e);
      } catch (IOException e) {
         throw Throwables.propagate(e);
      }
   }
}
