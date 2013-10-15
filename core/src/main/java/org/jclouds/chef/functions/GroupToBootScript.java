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
package org.jclouds.chef.functions;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.transform;
import static org.jclouds.scriptbuilder.domain.Statements.appendFile;
import static org.jclouds.scriptbuilder.domain.Statements.exec;
import static org.jclouds.scriptbuilder.domain.Statements.newStatementList;

import java.net.URI;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jclouds.chef.config.InstallChef;
import org.jclouds.chef.config.Validator;
import org.jclouds.chef.domain.BootstrapConfig;
import org.jclouds.crypto.Pems;
import org.jclouds.location.Provider;
import org.jclouds.scriptbuilder.ExitInsteadOfReturn;
import org.jclouds.scriptbuilder.domain.Statement;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * 
 * Generates a bootstrap script relevant for a particular group
 * 
 * @author Adrian Cole
 */
@Singleton
public class GroupToBootScript implements Function<String, Statement> {
   private static final Pattern newLinePattern = Pattern.compile("(\\r\\n)|(\\n)");

   private final Supplier<URI> endpoint;
   private final CacheLoader<String, BootstrapConfig> bootstrapConfigForGroup;
   private final Statement installChef;
   private final Optional<String> validatorName;
   private final Optional<PrivateKey> validatorCredential;

   @Inject
   public GroupToBootScript(@Provider Supplier<URI> endpoint,
         CacheLoader<String, BootstrapConfig> bootstrapConfigForGroup, @InstallChef Statement installChef,
         @Validator Optional<String> validatorName, @Validator Optional<PrivateKey> validatorCredential) {
      this.endpoint = checkNotNull(endpoint, "endpoint");
      this.bootstrapConfigForGroup = checkNotNull(bootstrapConfigForGroup, "bootstrapConfigForGroup");
      this.installChef = checkNotNull(installChef, "installChef");
      this.validatorName = checkNotNull(validatorName, "validatorName");
      this.validatorCredential = checkNotNull(validatorCredential, validatorCredential);
   }

   @Override
   public Statement apply(String group) {
      checkNotNull(group, "group");
      String validatorClientName = validatorName.get();
      PrivateKey validatorKey = validatorCredential.get();

      BootstrapConfig bootstrapConfig = null;
      try {
         bootstrapConfig = bootstrapConfigForGroup.load(group);
      } catch (Exception e) {
         throw propagate(e);
      }

      String chefConfigDir = "{root}etc{fs}chef";
      Statement createChefConfigDir = exec("{md} " + chefConfigDir);
      Statement createClientRb = appendFile(chefConfigDir + "{fs}client.rb", ImmutableList.of("require 'rubygems'",
            "require 'ohai'", "o = Ohai::System.new", "o.all_plugins",
            String.format("node_name \"%s-\" + o[:ipaddress]", group), "log_level :info", "log_location STDOUT",
            String.format("validation_client_name \"%s\"", validatorClientName),
            String.format("chef_server_url \"%s\"", endpoint.get())));

      Statement createValidationPem = appendFile(chefConfigDir + "{fs}validation.pem", Splitter.on(newLinePattern)
            .split(Pems.pem(validatorKey)));

      String chefBootFile = chefConfigDir + "{fs}first-boot.json";
      Statement createFirstBoot = appendFile(chefBootFile,
            Collections.singleton(createNodeConfiguration(bootstrapConfig)));

      ImmutableMap.Builder<String, String> options = ImmutableMap.builder();
      options.put("-j", chefBootFile);
      if (bootstrapConfig.getEnvironment().isPresent()) {
         options.put("-E", bootstrapConfig.getEnvironment().get().toString());
      }
      if (bootstrapConfig.getInterval().isPresent()) {
         options.put("-i", bootstrapConfig.getInterval().get().toString());
      }
      if (bootstrapConfig.getSplay().isPresent()) {
         options.put("-s", bootstrapConfig.getSplay().get().toString());
      }
      if (bootstrapConfig.isDaemonize()) {
         options.put("-d", "");
      }
      String strOptions = Joiner.on(' ').withKeyValueSeparator(" ").join(options.build());
      Statement runChef = exec("chef-client " + strOptions);

      return newStatementList(new ExitInsteadOfReturn(installChef), createChefConfigDir, createClientRb,
            createValidationPem, createFirstBoot, runChef);
   }

   private String createNodeConfiguration(BootstrapConfig config) {
      StringBuilder json = new StringBuilder();

      if (config.getAttributes().isPresent()) {
         // Start the node configuration with the attributes, but remove the
         // last bracket to append the run list to the json configuration
         String attribtues = config.getAttributes().get().toString();
         json.append(attribtues.substring(0, attribtues.lastIndexOf('}')));
         json.append(",");
      } else {
         json.append("{");
      }
      json.append("\"run_list\": [");
      Joiner.on(',').appendTo(json, transform(config.getRunList(), new Function<String, String>() {
         @Override
         public String apply(String input) {
            return "\"" + input + "\"";
         }
      }));
      json.append("]}");

      return json.toString();
   }

}
