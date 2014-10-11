package org.seasar.framework.unit;

import org.seasar.framework.unit.impl.ConventionTestIntrospector;

public class ConventionIntrospectorRepository {
    private static ThreadLocal<ConventionTestIntrospector> _introspector = new ThreadLocal<>();

    static {
        ConventionTestIntrospector introspector =
                new ConventionTestIntrospector();
        introspector.init();
        _introspector.set(introspector);
    }

    public static ConventionTestIntrospector get() {
        return _introspector.get();
    }
}
