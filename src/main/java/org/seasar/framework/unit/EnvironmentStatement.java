package org.seasar.framework.unit;

import org.junit.runners.model.Statement;
import org.seasar.framework.container.S2Container;
import org.seasar.framework.container.factory.S2ContainerFactory;
import org.seasar.framework.env.Env;
import org.seasar.framework.unit.Seasar2.Configurator;
import org.seasar.framework.util.ResourceUtil;

/**
 * S2JUnit4 の振る舞いを設定する.
 */
class EnvironmentStatement extends Statement {
    /** 元の statement */
    private final Statement _statement;

    /** コンフィグレーションファイルから構築されたコンフィグレーションS2コンテナ */
    private S2Container _configurationContainer = null;

    /**
     * コンストラクタ.
     *
     * @param statement 元の statement
     */
    public EnvironmentStatement(final Statement statement) {
        _statement = statement;
    }

    @Override
    public void evaluate() throws Throwable {
        configure();
        try {
            _statement.evaluate();
        } finally {
            dispose();
        }
    }

    /**
     * このクラスを設定します.
     */
    private void configure() {
        String configFile =
                System.getProperty(
                        Seasar2.S2JUNIT4_CONFIG_KEY,
                        Seasar2.S2JUNIT4_CONFIG_PATH);
        configure(configFile);
    }

    /**
     * このクラスを設定します.
     *
     * @param configFile
     *            設定ファイルのパス
     */
    private void configure(final String configFile) {
        Env.setFilePath(Seasar2.ENV_PATH);
        Env.setValueIfAbsent(Seasar2.ENV_VALUE);

        if (!ResourceUtil.isExist(configFile)) {
            return;
        }

        _configurationContainer = S2ContainerFactory.create(configFile);
        Configurator configurator;
        if (_configurationContainer.hasComponentDef(Configurator.class)) {
            configurator =
                    (Configurator)
                    _configurationContainer.getComponent(Configurator.class);
        } else {
            configurator = new Seasar2.DefaultConfigurator();
        }
        configurator.configure(_configurationContainer);
    }

    /**
     * このクラスを破棄します.
     */
    private void dispose() {
        if (_configurationContainer != null) {
            _configurationContainer.destroy();
            _configurationContainer = null;
        }
        Env.initialize();
    }
}
