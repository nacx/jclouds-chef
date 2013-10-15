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
package org.jclouds.chef.domain;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.jclouds.domain.JsonBall;
import org.jclouds.javax.annotation.Nullable;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.gson.annotations.SerializedName;

/**
 * Configures how the nodes in a group will bootstrap.
 * 
 * @author Ignasi Barrera
 * @since 1.7
 */
public class BootstrapConfig {
   public static Builder builder() {
      return new Builder();
   }

   public static class Builder {
      private String environment;
      private boolean daemonize;
      private Integer interval;
      private Integer splay;
      private ImmutableList.Builder<String> runList = ImmutableList.builder();
      private JsonBall attributes;

      /**
       * Sets the run list that will be executed in the nodes of the group.
       */
      public Builder runList(Iterable<String> runList) {
         this.runList.addAll(checkNotNull(runList, "runList"));
         return this;
      }

      /**
       * Sets the environment where the nodes in the group will be deployed.
       */
      public Builder environment(String environment) {
         this.environment = checkNotNull(environment, "environment");
         return this;
      }

      /**
       * Sets the attributes that will be populated to the deployed nodes.
       */
      public Builder attributes(JsonBall attributes) {
         this.attributes = checkNotNull(attributes, "attributes");
         return this;
      }

      /**
       * Configures the chef-client run to be executed in background.
       * <p>
       * Note that when using this flag, chef-client will be executed twice: The
       * first run will be executed in foreground, so the jclouds compute
       * service can properly poll and control script execution. Once the first
       * run is successful, a second run will be scheduled in background so the
       * node gets synchronized with the Chef server.
       * <p>
       * If you want the script to completely run in background, you have to
       * manually use nohup when calling the compute service.
       */
      public Builder daemonize(boolean daemonize) {
         this.daemonize = daemonize;
         return this;
      }

      /**
       * Sets the interval between chef-client runs, in seconds.
       * <p>
       * When using this parameter chef-client will run periodically. It is
       * often a good idea to use it in combination with the
       * {@link #daemonize(boolean)} flag, to avoid creating a script that never
       * terminates.
       */
      public Builder interval(int interval) {
         this.interval = interval;
         return this;
      }

      /**
       * The splay time for running at intervals, in seconds.
       */
      public Builder splay(int splay) {
         this.splay = splay;
         return this;
      }

      public BootstrapConfig build() {
         return new BootstrapConfig(runList.build(), environment, attributes, new ChefClientConfig(interval, splay,
               daemonize));
      }
   }

   private String environment;
   @SerializedName("chef-client")
   private ChefClientConfig chefClientConfig = new ChefClientConfig();
   @SerializedName("run_list")
   private List<String> runList;
   private JsonBall attributes;

   // Serialization support
   protected BootstrapConfig() {
      
   }

   protected BootstrapConfig(List<String> runList, @Nullable String environment, @Nullable JsonBall attributes,
         ChefClientConfig chefClientConfig) {
      this.runList = checkNotNull(runList, "runList");
      this.environment = environment;
      this.attributes = attributes;
      this.chefClientConfig = checkNotNull(chefClientConfig, "chefClientConfig");
   }

   public List<String> getRunList() {
      return runList;
   }

   public Optional<String> getEnvironment() {
      return Optional.fromNullable(environment);
   }

   public Optional<JsonBall> getAttributes() {
      return Optional.fromNullable(attributes);
   }

   public boolean isDaemonize() {
      return chefClientConfig != null && chefClientConfig.isDaemonize();
   }

   public Optional<Integer> getInterval() {
      return chefClientConfig == null ? Optional.<Integer> absent() : Optional.fromNullable(chefClientConfig
            .getInterval());
   }

   public Optional<Integer> getSplay() {
      return chefClientConfig == null ? Optional.<Integer> absent() : Optional
            .fromNullable(chefClientConfig.getSplay());
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
      result = prime * result + ((chefClientConfig == null) ? 0 : chefClientConfig.hashCode());
      result = prime * result + ((environment == null) ? 0 : environment.hashCode());
      result = prime * result + ((runList == null) ? 0 : runList.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      BootstrapConfig other = (BootstrapConfig) obj;
      if (attributes == null) {
         if (other.attributes != null)
            return false;
      } else if (!attributes.equals(other.attributes))
         return false;
      if (chefClientConfig == null) {
         if (other.chefClientConfig != null)
            return false;
      } else if (!chefClientConfig.equals(other.chefClientConfig))
         return false;
      if (environment == null) {
         if (other.environment != null)
            return false;
      } else if (!environment.equals(other.environment))
         return false;
      if (runList == null) {
         if (other.runList != null)
            return false;
      } else if (!runList.equals(other.runList))
         return false;
      return true;
   }

   @Override
   public String toString() {
      return "BootstrapConfig [environment=" + environment + ", chefClientConfig=" + chefClientConfig + ", runList="
            + runList + ", attributes=" + attributes + "]";
   }

   public static class ChefClientConfig {
      private Integer interval;
      private Integer splay;
      private boolean daemonize;

      // Serialization support
      protected ChefClientConfig() {

      }

      protected ChefClientConfig(@Nullable Integer interval, @Nullable Integer splay, boolean daemonize) {
         this.interval = interval;
         this.splay = splay;
         this.daemonize = daemonize;
      }

      public Integer getInterval() {
         return interval;
      }

      public Integer getSplay() {
         return splay;
      }

      public boolean isDaemonize() {
         return daemonize;
      }

      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + (daemonize ? 1231 : 1237);
         result = prime * result + ((interval == null) ? 0 : interval.hashCode());
         result = prime * result + ((splay == null) ? 0 : splay.hashCode());
         return result;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         ChefClientConfig other = (ChefClientConfig) obj;
         if (daemonize != other.daemonize)
            return false;
         if (interval == null) {
            if (other.interval != null)
               return false;
         } else if (!interval.equals(other.interval))
            return false;
         if (splay == null) {
            if (other.splay != null)
               return false;
         } else if (!splay.equals(other.splay))
            return false;
         return true;
      }

      @Override
      public String toString() {
         return "ChefClientConfig [interval=" + interval + ", splay=" + splay + ", daemonize=" + daemonize + "]";
      }

   }

}
