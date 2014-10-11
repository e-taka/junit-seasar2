package org.seasar.framework.unit;

import org.junit.runners.model.Statement;

public class ContainerRule extends Statement {
    private final Statement _statement;
    private InternalTestContext testContext = null;

    public ContainerRule(final Statement statement) {
        _statement = statement;
    }

    @Override
    public void evaluate() throws Throwable {
        initContainer();
        try {
            _statement.evaluate();
        } finally {
            if (testContext != null) {
                testContext.destroyContainer();
            }
        }
    }

    /**
     * コンテナを初期化します。
     */
    protected void initContainer() {
        testContext = TestContextRepository.get();
        if (testContext == null) {
            return;
        }

        testContext.include();
//        introspector.createMock(method, test, testContext);
        testContext.initContainer();
    }
}
