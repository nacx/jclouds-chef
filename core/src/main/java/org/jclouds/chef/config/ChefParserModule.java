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
package org.jclouds.chef.config;

import java.lang.reflect.Type;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import org.jclouds.chef.domain.DatabagItem;
import org.jclouds.chef.functions.ParseCookbookDefinitionFromJson;
import org.jclouds.chef.functions.ParseCookbookVersionsV09FromJson;
import org.jclouds.chef.functions.ParseCookbookVersionsV10FromJson;
import org.jclouds.chef.functions.ParseKeySetFromJson;
import org.jclouds.chef.json.DatabagItemAdapter;
import org.jclouds.chef.json.PrivateKeyAdapter;
import org.jclouds.chef.json.PublicKeyAdapter;
import org.jclouds.chef.json.X509CertificateAdapter;
import org.jclouds.chef.json.internal.KeepLastRepeatedKeyMapTypeAdapter;
import org.jclouds.chef.suppliers.ChefVersionSupplier;
import org.jclouds.http.HttpResponse;
import org.jclouds.json.config.GsonModule.DateAdapter;
import org.jclouds.json.config.GsonModule.Iso8601DateAdapter;
import org.jclouds.json.internal.NullFilteringTypeAdapterFactories.MapTypeAdapterFactory;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/**
 * Provides custom adapter bindings for json serialization/deserialization.
 * 
 * @author Adrian Cole
 * @author Ignasi Barrera
 */
public class ChefParserModule extends AbstractModule {

   @Provides
   @Singleton
   public Map<Type, Object> provideCustomAdapterBindings(DatabagItemAdapter databagItemAdapter,
         PrivateKeyAdapter privateAdapter, PublicKeyAdapter publicAdapter, X509CertificateAdapter certAdapter) {
      return ImmutableMap.<Type, Object> builder() //
            .put(DatabagItem.class, databagItemAdapter) //
            .put(PrivateKey.class, privateAdapter) //
            .put(PublicKey.class, publicAdapter) //
            .put(X509Certificate.class, certAdapter) //
            .build();
   }

   @Provides
   @Singleton
   @CookbookParser
   public Function<HttpResponse, Set<String>> provideCookbookDefinitionAdapter(ChefVersionSupplier chefVersionSupplier,
         ParseCookbookDefinitionFromJson v10parser, ParseKeySetFromJson v09parser) {
      return chefVersionSupplier.get() >= 10 ? v10parser : v09parser;
   }

   @Provides
   @Singleton
   @CookbookVersionsParser
   public Function<HttpResponse, Set<String>> provideCookbookDefinitionAdapter(ChefVersionSupplier chefVersionSupplier,
         ParseCookbookVersionsV10FromJson v10parser, ParseCookbookVersionsV09FromJson v09parser) {
      return chefVersionSupplier.get() >= 10 ? v10parser : v09parser;
   }

   @Override
   protected void configure() {
      bind(DateAdapter.class).to(Iso8601DateAdapter.class);
      bind(MapTypeAdapterFactory.class).to(KeepLastRepeatedKeyMapTypeAdapter.Factory.class);
   }
}
