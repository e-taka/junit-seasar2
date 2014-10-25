package org.seasar.framework.unit;

import org.junit.runners.model.Statement;

/**
 * テスト実行前後で、DI コンテナを初期化、廃棄する.
 */
class ContainerRule extends Statement {
    /** 元の statement */
    private final Statement _statement;
    /** S2JUnit4の内部的なテストコンテキスト */
    private InternalTestContext _testContext = null;

    /**
     * DI コンテナを初期化、廃棄する statement を作成する.
     *
     * @param statement 元の statement
     */
    public ContainerRule(final Statement statement) {
        _statement = statement;
    }

    @Override
    public void evaluate() throws Throwable {
        initContainer();
        try {
            _statement.evaluate();
        } finally {
            if (_testContext != null) {
                _testContext.destroyContainer();
            }
        }
    }

    /**
     * コンテナを初期化します.
     */
    protected void initContainer() {
        _testContext = TestContextRepository.get();
        if (_testContext == null) {
            return;
        }

        _testContext.include();
//        introspector.createMock(method, test, testContext);
        _testContext.initContainer();
    }
}
