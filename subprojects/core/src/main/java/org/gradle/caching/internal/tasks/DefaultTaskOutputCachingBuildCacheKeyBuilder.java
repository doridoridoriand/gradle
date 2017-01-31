/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.caching.internal.tasks;

import com.google.common.hash.HashCode;
import org.gradle.api.Nullable;
import org.gradle.api.internal.tasks.execution.BuildCacheKeyInputs;
import org.gradle.caching.internal.BuildCacheHasher;
import org.gradle.caching.internal.DefaultBuildCacheHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DefaultTaskOutputCachingBuildCacheKeyBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTaskOutputCachingBuildCacheKeyBuilder.class);

    private BuildCacheHasher hasher = new DefaultBuildCacheHasher();
    private Map<String, HashCode> inputHashes = new HashMap<String, HashCode>();
    private Set<String> outputPropertyNames = new HashSet<String>();
    private HashCode classLoaderHash;
    private HashCode actionsClassLoaderHash;

    public DefaultTaskOutputCachingBuildCacheKeyBuilder appendClassloaderHash(@Nullable HashCode hashCode) {
        classLoaderHash = hashCode;
        if (hashCode != null) {
            hasher.putBytes(hashCode.asBytes());
        }
        log("classLoaderHash", hashCode);
        return this;
    }

    public DefaultTaskOutputCachingBuildCacheKeyBuilder appendActionsClassloaderHash(@Nullable HashCode hashCode) {
        actionsClassLoaderHash = hashCode;
        if (hashCode != null) {
            hasher.putBytes(hashCode.asBytes());
        }
        log("actionsClassLoaderHash", hashCode);
        return this;
    }

    public DefaultTaskOutputCachingBuildCacheKeyBuilder appendInputPropertyHash(String propertyName, HashCode hashCode) {
        hasher.putString(propertyName);
        hasher.putBytes(hashCode.asBytes());
        inputHashes.put(propertyName, hashCode);
        LOGGER.debug("Appending inputPropertyHash for '{}' to build cache key: {}", propertyName, hashCode);
        return this;
    }

    public DefaultTaskOutputCachingBuildCacheKeyBuilder appendOutputPropertyName(String propertyName) {
        outputPropertyNames.add(propertyName);
        hasher.putString(propertyName);
        log("outputPropertyName", propertyName);
        return this;
    }

    public DefaultTaskOutputCachingBuildCacheKeyBuilder appendTaskClass(String taskClass) {
        hasher.putString(taskClass);
        log("taskClass", taskClass);
        return this;
    }

    private static void log(String name, Object value) {
        LOGGER.debug("Appending {} to build cache key: {}", name, value);
    }

    public TaskOutputCachingBuildCacheKey build() {
        BuildCacheKeyInputs inputs = new BuildCacheKeyInputs(classLoaderHash, actionsClassLoaderHash, inputHashes, outputPropertyNames);
        if (classLoaderHash == null || actionsClassLoaderHash == null) {
            return new InvalidTaskOutputCachingBuildCacheKey(inputs);
        }
        return new DefaultTaskOutputCachingBuildCacheKey(hasher.hash(), inputs);
    }

    private class DefaultTaskOutputCachingBuildCacheKey implements TaskOutputCachingBuildCacheKey {
        private final HashCode hashCode;
        private final BuildCacheKeyInputs inputs;

        private DefaultTaskOutputCachingBuildCacheKey(HashCode hashCode, BuildCacheKeyInputs inputs) {
            this.hashCode = hashCode;
            this.inputs = inputs;
        }

        @Override
        public String getHashCode() {
            return hashCode.toString();
        }

        @Override
        public BuildCacheKeyInputs getInputs() {
            return inputs;
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }
}
