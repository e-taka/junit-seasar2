package org.seasar.framework.unit;

/**
 * S2JUnit4の内部的なテストコンテキストのレポジトリ.
 *
 */
class TestContextRepository {
    /** {@link InternalTestContext} の値 */
    private static ThreadLocal<InternalTestContext> _context =
            new ThreadLocal<>();

    /**
     * {@link InternalTestContext} を取得する.
     *
     * @return {@link InternalTestContext}
     */
    public static InternalTestContext get() {
        return _context.get();
    }

    /**
     * {@link InternalTestContext} を設定する.
     *
     * @param context {@link InternalTestContext}
     */
    public static void put(final InternalTestContext context) {
        _context.set(context);
    }

    /**
     * {@link InternalTestContext} を削除する.
     */
    public static void remove() {
        _context.remove();
    }

    /** ユーティリティクラスであるため、インスタンスを生成しない. */
    protected TestContextRepository() {
        throw new UnsupportedOperationException();
    }
}
