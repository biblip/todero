package com.social100.todero.console.base;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * A URLClassLoader that loads classes child-first except for java.*
 * and specified parent-first package prefixes.
 */
public class ChildFirstURLClassLoader extends URLClassLoader {
  private final String[] parentFirstPackages;

  /**
   * Constructs a ChildFirstURLClassLoader with no custom parent-first packages.
   */
  public ChildFirstURLClassLoader(URL[] urls, ClassLoader parent) {
    this(urls, parent, new String[0]);
  }

  /**
   * Constructs a ChildFirstURLClassLoader with custom parent-first package prefixes.
   *
   * @param urls                   URLs to load classes and resources from
   * @param parent                 the parent class loader for delegation
   * @param parentFirstPackages    package name prefixes to always delegate to parent
   */
  public ChildFirstURLClassLoader(URL[] urls, ClassLoader parent, String[] parentFirstPackages) {
    super(urls, parent);
    this.parentFirstPackages = parentFirstPackages != null ? parentFirstPackages : new String[0];
  }

  @Override
  protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    // Always delegate system classes
    if (name.startsWith("java.")) {
      return super.loadClass(name, resolve);
    }
    // Delegate parent-first for configured prefixes
    for (String prefix : parentFirstPackages) {
      if (name.startsWith(prefix)) {
        return super.loadClass(name, resolve);
      }
    }
    // Try to load locally (child-first)
    Class<?> c = findLoadedClass(name);
    if (c == null) {
      try {
        c = findClass(name);
      } catch (ClassNotFoundException e) {
        // Fallback to parent if not found
        return super.loadClass(name, resolve);
      }
    }
    if (resolve) {
      resolveClass(c);
    }
    return c;
  }
}
