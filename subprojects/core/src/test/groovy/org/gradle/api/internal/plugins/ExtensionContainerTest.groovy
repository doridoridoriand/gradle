/*
 * Copyright 2011 the original author or authors.
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

package org.gradle.api.internal.plugins

import org.gradle.api.reflect.TypeOf;
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.internal.ThreadGlobalInstantiator
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension
import spock.lang.Specification

class ExtensionContainerTest extends Specification {

    def container = new DefaultConvention(ThreadGlobalInstantiator.getOrCreate())
    def extension = new FooExtension()
    def barExtension = new BarExtension()

    class FooExtension {
        String message = "smile"
    }

    class BarExtension {}

    class SomeExtension {}

    def "has dynamic extension"() {
        expect:
        container.getByName(ExtraPropertiesExtension.EXTENSION_NAME) == container.extraProperties
    }

    def "extension can be accessed and configured"() {
        when:
        container.add("foo", extension)
        container.extensionsAsDynamicObject.foo.message = "Hey!"

        then:
        extension.message == "Hey!"
    }

    def "extension can be configured via script block"() {
        when:
        container.add("foo", extension)
        container.extensionsAsDynamicObject.foo {
            message = "You cool?"
        }

        then:
        extension.message == "You cool?"
    }

    def "extension cannot be set as property because we want users to use explicit method to add extensions"() {
        when:
        container.add("foo", extension)
        container.extensionsAsDynamicObject.foo = new FooExtension()

        then:
        IllegalArgumentException e = thrown()
        e.message == "There's an extension registered with name 'foo'. You should not reassign it via a property setter."
    }

    def "can register extensions using dynamic property setter"() {
        when:
        container.foo = extension

        then:
        container.findByName('foo') == extension
    }

    def "can access extensions using dynamic property getter"() {
        when:
        container.add('foo', extension)

        then:
        container.foo == extension
    }

    def "cannot replace an extension"() {
        given:
        container.add('foo', extension)

        when:
        container.add('foo', 'other')

        then:
        IllegalArgumentException e = thrown()
        e.message == "Cannot add extension with name 'foo', as there is an extension already registered with that name."

        when:
        container.foo = 'other'

        then:
        IllegalArgumentException e2 = thrown()
        e2.message == "There's an extension registered with name 'foo'. You should not reassign it via a property setter."

        when:
        container.create('foo', Thing, 'bar')

        then:
        IllegalArgumentException e3 = thrown()
        e3.message == "Cannot add extension with name 'foo', as there is an extension already registered with that name."
    }

    def "knows registered extensions"() {
        when:
        container.add("foo", extension)
        container.add("bar", barExtension)

        then:
        container.getByName("foo") == extension
        container.findByName("bar") == barExtension

        container.getByType(BarExtension) == barExtension
        container.findByType(FooExtension) == extension

        container.findByType(SomeExtension) == null
        container.findByName("i don't exist") == null
    }

    def "throws when unknown exception wanted by name"() {
        container.add("foo", extension)

        when:
        container.getByName("i don't exist")

        then:
        def ex = thrown(UnknownDomainObjectException)
        ex.message == "Extension with name 'i don't exist' does not exist. Currently registered extension names: [${ExtraPropertiesExtension.EXTENSION_NAME}, foo]"
    }

    def "throws when unknown extension wanted by type"() {
        container.add("foo", extension)

        when:
        container.getByType(SomeExtension)

        then:
        def ex = thrown(UnknownDomainObjectException)
        ex.message == "Extension of type 'SomeExtension' does not exist. Currently registered extension types: [${ExtraPropertiesExtension.simpleName}, FooExtension]"
    }

    def "types can be retrieved by interface and super types"() {
        given:
        def impl = new Impl()
        def child = new Child()

        when:
        container.add('i', impl)
        container.add('c', child)

        then:
        container.findByType(Capability) == impl
        container.getByType(Impl) == impl
        container.findByType(Parent) == child
        container.getByType(Parent) == child
    }

    def "can create ExtensionAware extensions"() {
        given:
        container.add("foo", Parent)
        def extension = container.getByName("foo")

        expect:
        extension instanceof ExtensionAware

        when:
        extension.extensions.create("thing", Thing, "bar")

        then:
        extension.thing.name == "bar"
    }

    def "can hide implementation type of extensions"() {
        given:
        container.create Parent, 'foo', Child
        container.create Capability, 'bar', Impl

        expect:
        container.findByType(Parent) != null
        container.findByType(Child) == null

        and:
        container.findByType(Capability) != null
        container.findByType(Impl) == null
    }

    def "can register extension with generic public type"() {
        given:
        container.add TypeOf.listOf(String), 'foo', []

        expect:
        container.findByType(List) != null
        container.findByType(TypeOf.listOf(String)) != null
    }

    def "can get extensions schema"() {
        given:
        container.create Parent, 'foo', Child
        container.create Capability, 'bar', Impl
        container.add TypeOf.listOf(String), 'baz', []

        expect:
        container.schema == [ext: TypeOf.of(ExtraPropertiesExtension), foo: TypeOf.of(Parent), bar: TypeOf.of(Capability), baz: TypeOf.listOf(String)]
    }
}

interface Capability {}
class Impl implements Capability {}

class Parent {}
class Child extends Parent {}
class Thing {
    String name

    Thing(String name) {
        this.name = name
    }
}
