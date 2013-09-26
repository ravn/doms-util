/*
 * $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The DOMS project.
 * Copyright (C) 2007-2010  The State and University Library
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package dk.statsbiblioteket.doms.webservices.exceptionhandling;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract superclass of mappers that map from one type of exception to
 * another.
 *
 * @param <E1> Exception type to map to.
 * @param <E2> Exception type to map from.
 */
/**
 * @deprecated This is nasty. Do not use it
 */
@Deprecated
public abstract class ExceptionMapper<E1 extends Exception, E2 extends Exception> {
    /**
     * Map an exception of type E2 to an exception of type E1. Implementations
     * should implement this method for each specific type of exception subtype
     * of E2 they wish to map. Implementations are expected to be trivial
     * one-liners, that simply wrap the given instance of E2 in an instance of a
     * subtype of E1.
     *
     * @param ce Exception to map from.
     * @return The exception to map to.
     */
    public abstract E1 convert(E2 ce);

    /**
     * Given an exception of type E2, map it to an exception of type E1.
     * This method will pick an implementation of <code>convert</code> that
     * takes the specific type of E2 and use it to map the exception. If none
     * are found, it will return the result of <code>convert(E2)</code>.
     *
     * @param ce The exception to map from.
     * @return The result of mapping the exception.
     * @throws RuntimeException If ce is not an instance of E2(!)
     */
    public final E1 convertMostApplicable(E2 ce) {
        Class<? extends Exception> ceClass = ce.getClass();
        List<Class<?>> typearguments = getTypeArguments(ExceptionMapper.class,
                                                        getClass());
        Class<?> e1Class = typearguments.get(0);
        Class<?> e2Class = typearguments.get(1);

        //TODO: Can this check ever fail?
        if (e2Class != null && !e2Class.isAssignableFrom(
                ceClass)) {
            //We are trying to convert an kind exception this mapper was not made for
            //FIXME: Use proper exception
            throw new RuntimeException("Tried to convert " + ceClass.toString()
                    + " with a mapper for " + e2Class.toString());
        }

        try {
            Method rightmethod = this.getClass().getMethod("convert", ceClass);
            Class<?> rightmethodreturnclass = rightmethod.getReturnType();
            if (e1Class.isAssignableFrom(
                    rightmethodreturnclass)) {
                //This should check the viability of the cast below
                Object result = rightmethod.invoke(this, ce);
                return (E1) result;
            } else {
                //Note: This exception is caught by the control flow below.
                throw new NoSuchMethodException(
                        "Convert('" + ceClass + "') returns '"
                                + rightmethodreturnclass + "' not '" + e2Class
                                + "'. Falling back to default conversion.");
            }
        } catch (Exception e) {
            //TODO log e
            return convert(ce);
        }
    }

    /**
     * Get the actual type arguments a child class has used to extend a generic
     * base class.
     *
     * @param baseClass  the base class
     * @param <T>        the base class
     * @param childClass the child class
     * @return a list of the raw classes for the actual type arguments.
     */
    private static <T> List<Class<?>> getTypeArguments(
            Class<T> baseClass, Class<? extends T> childClass) {
        Map<Type, Type> resolvedTypes = new HashMap<Type, Type>();
        Type type = childClass;

        // start walking up the inheritance hierarchy until we hit baseClass.
        // We know that there will be a way, due to the generic relations from
        // the parameters
        while (!baseClass.equals(getClass(type))) {
            if (type instanceof Class) {
                // there is no useful information for us in raw types, so just
                // keep going.
                type = ((Class) type).getGenericSuperclass();
            } else {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Class<?> rawType = (Class) parameterizedType.getRawType();
                Type[] actualTypeArguments = parameterizedType
                        .getActualTypeArguments();
                TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
                for (int i = 0; i < actualTypeArguments.length; i++) {
                    resolvedTypes
                            .put(typeParameters[i], actualTypeArguments[i]);
                }

                if (!rawType.equals(baseClass)) {
                    type = rawType.getGenericSuperclass();
                }
            }
        }

        // finally, for each actual type argument provided to baseClass,
        // determine (if possible) the raw class for that type argument.
        Type[] actualTypeArguments;
        if (type instanceof Class) {
            actualTypeArguments = ((Class) type).getTypeParameters();
        } else {
            actualTypeArguments = ((ParameterizedType) type)
                    .getActualTypeArguments();
        }
        List<Class<?>> typeArgumentsAsClasses = new ArrayList<Class<?>>();
        // resolve types by chasing down type variables.
        for (Type baseType : actualTypeArguments) {
            while (resolvedTypes.containsKey(baseType)) {
                baseType = resolvedTypes.get(baseType);
            }
            typeArgumentsAsClasses.add(getClass(baseType));
        }
        return typeArgumentsAsClasses;
    }

    /**
     * Get the underlying class for a type, or null if the type is a variable
     * type.
     *
     * @param type the type
     * @return the underlying class
     */
    private static Class<?> getClass(Type type) {
        if (type instanceof Class) {
            return (Class) type;
        } else if (type instanceof ParameterizedType) {
            return getClass(((ParameterizedType) type).getRawType());
        } else if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type)
                    .getGenericComponentType();
            Class<?> componentClass = getClass(componentType);
            if (componentClass != null) {
                return Array.newInstance(componentClass, 0).getClass();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
