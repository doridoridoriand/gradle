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

package org.gradle.api.reflect;

import com.google.common.base.Objects;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import org.gradle.api.Incubating;
import org.gradle.internal.reflect.Types;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * A {@link Type} with generics.
 *
 * @param <T> Parameterized type
 * @since 4.0
 */
@Incubating
public abstract class TypeOf<T> {

    public static <T> TypeOf<T> of(Class<T> type) {
        return new TypeOf<T>(TypeToken.of(type)) {
        };
    }

    public static <T> TypeOf<T> of(Type type) {
        return new TypeOf<T>((TypeToken<T>) TypeToken.of(type)) {
        };
    }

    public static <T> TypeOf<List<T>> listOf(Class<T> elementType) {
        return new TypeOf<List<T>>(
            new TypeToken<List<T>>() {
            }.where(new TypeParameter<T>() {
            }, elementType)) {
        };
    }

    public static <T> TypeOf<Set<T>> setOf(Class<T> elementType) {
        return new TypeOf<Set<T>>(
            new TypeToken<Set<T>>() {
            }.where(new TypeParameter<T>() {
            }, elementType)) {
        };
    }

    public static <K, V> TypeOf<Map<K, V>> mapOf(Class<K> keyType, Class<V> valueType) {
        return new TypeOf<Map<K, V>>(
            new TypeToken<Map<K, V>>() {
            }.where(new TypeParameter<K>() {
            }, keyType).where(new TypeParameter<V>() {
            }, valueType)) {
        };
    }

    private final TypeToken<T> token;

    private TypeOf(TypeToken<T> token) {
        this.token = token;
    }

    @SuppressWarnings("unchecked")
    protected TypeOf() {
        Type genericSuperclass = getClass().getGenericSuperclass();
        Type type;
        if (genericSuperclass instanceof ParameterizedType) {
            type = ((ParameterizedType) genericSuperclass).getActualTypeArguments()[0];
        } else {
            type = Object.class;
        }
        token = (TypeToken<T>) TypeToken.of(type);
    }

    public final Type getType() {
        return token.getType();
    }

    public final Class<? super T> getRawType() {
        return token.getRawType();
    }

    public final List<Type> getTypeArguments() {
        if (token.isArray()) {
            return singletonList(token.getComponentType().getType());
        }
        if (token.getType() instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) token.getType();
            return asList(pt.getActualTypeArguments());
        }
        return emptyList();
    }

    public final List<TypeOf<?>> getTypeOfArguments() {
        List<TypeOf<?>> typeOfArguments = new ArrayList<TypeOf<?>>();
        List<Type> typeArguments = getTypeArguments();
        for (Type typeArgument : typeArguments) {
            typeOfArguments.add(new TypeOf(TypeToken.of(typeArgument)) {
            });
        }
        return typeOfArguments;
    }

    public final List<Class<?>> getRawTypeArguments() {
        List<TypeOf<?>> typeOfArguments = getTypeOfArguments();
        List<Class<?>> rawTypeArguments = new ArrayList<Class<?>>();
        for (TypeOf<?> typeOfArgument : typeOfArguments) {
            rawTypeArguments.add(typeOfArgument.getRawType());
        }
        return rawTypeArguments;
    }

    public final boolean isAssignableFrom(Type type) {
        return token.isAssignableFrom(type);
    }

    public String getSimpleName() {
        return Types.getGenericSimpleName(token.getType());
    }

    @Override
    public final String toString() {
        return token.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof TypeOf)) {
            return false;
        }
        TypeOf<?> typeOf = (TypeOf<?>) o;
        return Objects.equal(token, typeOf.token);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(token);
    }
}
