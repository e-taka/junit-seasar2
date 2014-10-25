package org.seasar.framework.unit;

import org.seasar.framework.unit.impl.ConventionTestIntrospector;

/**
 * テストクラスのイントロスペクターのレポジトリ.
 */
class ConventionIntrospectorRepository {
    /** テストクラスのイントロスペクター */
    private static ThreadLocal<ConventionTestIntrospector> _introspector =
            new ThreadLocal<>();

    static {
        ConventionTestIntrospector introspector =
                new ConventionTestIntrospector();
        introspector.init();
        _introspector.set(introspector);
    }

    /**
     * テストクラスのイントロスペクターを取得する.
     *
     * @return テストクラスのイントロスペクター
     */
    public static ConventionTestIntrospector get() {
        return _introspector.get();
    }

    /** ユーティリティクラスであるため、インスタンスを生成しない. */
    protected ConventionIntrospectorRepository() {
        throw new UnsupportedOperationException();
    }
}
