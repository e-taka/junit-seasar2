package org.seasar.framework.unit;

/**
 * {@link InternalTestContext} のレポジトリ.
 *
 */
class TestContextRepository {
    private static ThreadLocal<InternalTestContext> _context = new ThreadLocal<>();

    public static InternalTestContext get() {
        return _context.get();
    }
    public static void put(final InternalTestContext context) {
        _context.set(context);
    }
    public static void remove() {
        _context.remove();
    }
}
