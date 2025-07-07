package com.social100.processor.beans;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeanFactory {

  private static final Map<Class<?>, Object> singletonCache = new HashMap<>();
  private static final List<Class<?>> registeredBeans = new ArrayList<>();

  public static void registerBeanClass(Class<?> clazz) {
    registeredBeans.add(clazz);
  }

  public static void registerBeanClasses(Collection<Class<?>> classes) {
    registeredBeans.addAll(classes);
  }

  public static <T> T getBean(Class<T> type) {
    for (Class<?> clazz : registeredBeans) {
      if (type.isAssignableFrom(clazz)) {
        return createOrGet(clazz, type);
      }
    }
    throw new RuntimeException("No registered bean for: " + type);
  }

  private static <T> T createOrGet(Class<?> clazz, Class<T> type) {
    Service annotation = clazz.getAnnotation(Service.class);
    if (annotation == null) {
      throw new RuntimeException("Class not annotated with @Service: " + clazz);
    }

    if (annotation.scope() == Scope.SINGLETON) {
      return type.cast(singletonCache.computeIfAbsent(clazz, BeanFactory::createBean));
    } else {
      return type.cast(createBean(clazz));
    }
  }

  private static Object createBean(Class<?> clazz) {
    try {
      Object instance = clazz.getDeclaredConstructor().newInstance();

      // Field injection
      for (Field field : clazz.getDeclaredFields()) {
        if (field.isAnnotationPresent(Inject.class)) {
          Class<?> fieldType = field.getType();
          Object dependency = getBean(fieldType);
          field.setAccessible(true);
          field.set(instance, dependency);
        }
      }

      return instance;
    } catch (Exception e) {
      throw new RuntimeException("Failed to create bean for " + clazz, e);
    }
  }
}
