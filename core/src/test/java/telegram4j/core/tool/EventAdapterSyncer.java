/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.core.tool;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.Event;

import java.util.TreeSet;

public class EventAdapterSyncer {
    public static final String INDENT = " ".repeat(4);

    public static void main(String[] args) {
        System.out.println("package telegram4j.core.event;");
        System.out.println();

        var imports = new TreeSet<String>();
        imports.add(Publisher.class.getCanonicalName());
        imports.add(Mono.class.getCanonicalName());

        collectSubtypes(Event.class, imports);

        for (String imp : imports) {
            System.out.println("import " + imp + ";");
        }

        System.out.println();
        System.out.println("import java.util.ArrayList;");

        System.out.println();
        System.out.println("public abstract class EventAdapter {");

        for (Class<?> subtype : Event.class.getPermittedSubclasses()) {
            printMethods(subtype);
        }

        System.out.println();
        System.out.println(INDENT + "public Publisher<?> hookOnEvent(Event event) {");
        System.out.println(INDENT.repeat(2) + "var compatible = new ArrayList<Publisher<?>>();");

        for (Class<?> subtype : Event.class.getPermittedSubclasses()) {
            printHooks(subtype);
        }

        System.out.println(INDENT.repeat(2) + "return Mono.whenDelayError(compatible);");
        System.out.println(INDENT + "}");

        System.out.println("}");
    }

    private static void printHooks(Class<?> type) {

        var subtypes = type.getPermittedSubclasses();
        if (subtypes != null) {
            String simpleName = type.getSimpleName();
            String methodName = "on" + simpleName;

            printHook(methodName, simpleName);

            for (Class<?> subtype : subtypes) {
                printHooks(subtype);
            }
        } else {
            String simpleName = type.getSimpleName();
            String methodName = "on" + simpleName.substring(0, simpleName.length() - "Event".length());

            printHook(methodName, simpleName);
        }
    }

    private static void printHook(String methodName, String simpleName) {
        System.out.println(INDENT.repeat(2) + "if (event instanceof " + simpleName + " e) compatible.add(" + methodName + "(e));");
    }

    private static void printMethods(Class<?> type) {

        var subtypes = type.getPermittedSubclasses();
        if (subtypes != null) {
            String simpleName = type.getSimpleName();
            String methodName = "on" + simpleName;

            boolean isGeneralSubtype = type.getSuperclass() == Event.class;
            if (isGeneralSubtype) {
                System.out.println(INDENT + "// region " + simpleName);
            }

            printMethod(methodName, simpleName);

            for (Class<?> subtype : subtypes) {
                printMethods(subtype);
            }

            if (isGeneralSubtype) {
                System.out.println();
                System.out.println(INDENT + "// endregion");
            }
        } else {
            String simpleName = type.getSimpleName();
            String methodName = "on" + simpleName.substring(0, simpleName.length() - "Event".length());

            printMethod(methodName, simpleName);
        }
    }

    private static void printMethod(String methodName, String simpleName) {
        System.out.println();
        System.out.println(INDENT + "public Publisher<?> " + methodName + "(" + simpleName + " event) {");
        System.out.println(INDENT.repeat(2) + "return Mono.empty();");
        System.out.println(INDENT + "}");
    }

    private static void collectSubtypes(Class<?> generalType, TreeSet<String> imports) {
        imports.add(generalType.getCanonicalName());

        var permittedSubclasses = generalType.getPermittedSubclasses();
        if (permittedSubclasses != null) {
            for (Class<?> subclass : permittedSubclasses) {
                collectSubtypes(subclass, imports);
            }
        }
    }
}
