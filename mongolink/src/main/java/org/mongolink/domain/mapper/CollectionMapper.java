/*
 * MongoLink, Object Document Mapper for Java and MongoDB
 *
 * Copyright (c) 2012, Arpinum or third-party contributors as
 * indicated by the @author tags
 *
 * MongoLink is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MongoLink is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the Lesser GNU General Public License
 * along with MongoLink.  If not, see <http://www.gnu.org/licenses/>. 
 *
 */

package org.mongolink.domain.mapper;


import org.bson.Document;
import org.mongolink.domain.converter.Converter;
import org.mongolink.utils.*;
import org.slf4j.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

class CollectionMapper implements Mapper {

    public CollectionMapper(FieldContainer fieldContainer) {
        this.fieldContainer = fieldContainer;
    }

    @Override
    public void save(Object instance, Document into) {
        try {
            Collection collection = value(instance);
            Object children = collection.stream().map(v -> context().converterFor(v.getClass()).toDbValue(v))
                    .collect(Collectors.toList());
            into.put(name(), children);
        } catch (Exception e) {
            LOGGER.error("Can't saveInto collection {}", name(), e);
        }
    }

    private String name() {
        return fieldContainer.name();
    }

    private Collection value(Object instance) {
        return (Collection) fieldContainer.value(instance);
    }

    @Override
    public void populate(Object instance, Document from) {
        try {
            Field field = Fields.find(instance.getClass(), name());
            field.setAccessible(true);
            ParameterizedType elementType = (ParameterizedType) field.getGenericType();
            Converter childMapper = context().converterFor((Class<?>) elementType.getActualTypeArguments()[0]);
            List list = (List) from.get(name());
            if (list != null) {
                Collection collection = (Collection) field.get(instance);
                for (Object o : list) {
                    //noinspection unchecked
                    collection.add(childMapper.fromDbValue(o));
                }
            }
            field.setAccessible(false);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private MapperContext context() {
        return mapper.getContext();
    }

    public void setMapper(ClassMapper<?> mapper) {
        this.mapper = mapper;
    }

    private final FieldContainer fieldContainer;
    private ClassMapper<?> mapper;
    private static final Logger LOGGER = LoggerFactory.getLogger(CollectionMapper.class);
}
