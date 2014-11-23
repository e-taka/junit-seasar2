package org.seasar.framework.unit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.seasar.framework.container.S2Container;
import org.seasar.framework.container.factory.S2ContainerFactory;
import org.seasar.framework.container.factory.SingletonS2ContainerFactory;
import org.seasar.framework.container.impl.S2ContainerBehavior;
import org.seasar.framework.convention.NamingConvention;
import org.seasar.framework.convention.impl.NamingConventionImpl;
import org.seasar.framework.unit.annotation.PublishedTestContext;
import org.seasar.framework.util.DisposableUtil;
import org.seasar.framework.util.ResourceUtil;
import org.seasar.framework.util.StringUtil;
import org.seasar.framework.util.tiger.ReflectionUtil;

/**
 * テスト実行前後で、{@link InternalTestContext} を作成する、削除する.
 */
class TestContextRule extends Statement {
    /** S2JUnit4のデフォルトの設定ファイルのパス */
    protected static final String DEFAULT_S2JUNIT4_PATH = "s2junit4.dicon";
    /** S2JUnit4の設定ファイルのパス */
    protected static String _s2junit4Path = DEFAULT_S2JUNIT4_PATH;

    /** 元の statement */
    private final Statement _statement;

    /** テストオブジェクト */
    private final Object _test;
    /** テストクラス */
    private final Class<?> _testClass;
    /** テストメソッド */
    private final Method _method;

    /** テストクラスのイントロスペクター */
    private final S2TestIntrospector _introspector;
    /** オリジナルのクラスローダー */
    private ClassLoader _originalClassLoader;
    /** テストで使用するクラスローダー */
    private UnitClassLoader _unitClassLoader;
    /** S2JUnit4の内部的なテストコンテキスト */
    private InternalTestContext _testContext;

    /**
     * {@link InternalTestContext} を作成、削除する statement を作成する.
     *
     * @param statement 元の statement
     * @param target テストクラスのインスタンス
     * @param clazz テストクラス
     * @param method テストメソッド
     */
    public TestContextRule(
            final Statement statement,
            final Object target,
            final TestClass clazz,
            final FrameworkMethod method) {
        _introspector = ConventionIntrospectorRepository.get();
        _statement = statement;
        _test = target;
        _testClass = clazz.getJavaClass();
        _method = method.getMethod();
    }

    @Override
    public void evaluate() throws Throwable {
        setUpTestContext();
        try {
            _statement.evaluate();
        } finally {
            tearDownTestContext();
        }
    }

    /**
     * テストコンテキストをセットアップします.
     *
     * @throws Throwable
     *             何らかの例外またはエラーが起きた場合
     */
    protected void setUpTestContext() throws Throwable {
        _originalClassLoader = getOriginalClassLoader();
        _unitClassLoader = new UnitClassLoader(_originalClassLoader);
        Thread.currentThread().setContextClassLoader(_unitClassLoader);
        if (needsWarmDeploy()) {
            S2ContainerFactory.configure("warmdeploy.dicon");
        }

        S2Container container = createRootContainer();
        SingletonS2ContainerFactory.setContainer(container);

        _testContext =
                InternalTestContext.class.cast(
                        container.getComponent(InternalTestContext.class));
        _testContext.setTestClass(_testClass);
        _testContext.setTestMethod(_method);

        if (_testContext.hasComponentDef(NamingConvention.class)) {
            ;
        } else if (isRegisterNamingConvention(_testClass, _method)) {
            NamingConvention namingConvention = new NamingConventionImpl();
            _testContext.register(namingConvention);
            _testContext.setNamingConvention(namingConvention);
        }

        TestContextRepository.put(_testContext);
        bindTestContext(_testClass);
    }

    /**
     * {@link NamingConvention} を登録しているか否かを返す.
     *
     * @param clazz  テストクラス
     * @param method テストメソッド
     * @return 登録している場合、{@code true}
     */
    private boolean isRegisterNamingConvention(
            final Class<?> clazz, final Method method) {
        return _introspector.isRegisterNamingConvention(clazz, method);
    }

    /**
     * テストクラスのインスタンスにj {@link InternalTestContext} をバインディングする.
     *
     * @param clazz テストクラス
     */
    private void bindTestContext(final Class<?> clazz) {
        if (Object.class.equals(clazz)) {
            return;
        }

        for (final Field field : clazz.getDeclaredFields()) {
            final Class<?> fieldClass = field.getType();
            if (!isAutoBindable(field)) {
                continue;
            }
            if (!fieldClass.isAssignableFrom(_testContext.getClass())) {
                continue;
            }
            if (!fieldClass.isAnnotationPresent(PublishedTestContext.class)) {
                continue;
            }

            field.setAccessible(true);
            if (ReflectionUtil.getValue(field, _test) != null) {
                continue;
            }
            bindField(field, _testContext);
        }

        bindTestContext(clazz.getSuperclass());
    }

    /**
     * オリジナルのクラスローダーを返します.
     *
     * @return オリジナルのクラスローダー
     */
    protected ClassLoader getOriginalClassLoader() {
        S2Container configurationContainer =
                S2ContainerFactory.getConfigurationContainer();
        if (configurationContainer != null
                && configurationContainer.hasComponentDef(ClassLoader.class)) {
            return ClassLoader.class.cast(configurationContainer
                    .getComponent(ClassLoader.class));
        }
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * ルートのコンテナを返します.
     *
     * @return ルートのコンテナ
     */
    protected S2Container createRootContainer() {
        String rootDicon = _introspector.getRootDicon(_testClass, _method);
        if (StringUtil.isEmpty(rootDicon)) {
            return S2ContainerFactory.create(_s2junit4Path);
        }
        S2Container container = S2ContainerFactory.create(rootDicon);
        S2ContainerFactory.include(container, _s2junit4Path);
        return container;

    }

    /**
     * テストコンテキストを解放します.
     *
     * @throws Throwable
     *             何らかの例外またはエラーが起きた場合
     */
    protected void tearDownTestContext() throws Throwable {
        _testContext = null;
        TestContextRepository.remove();
        DisposableUtil.dispose();
        S2ContainerBehavior.setProvider(
                new S2ContainerBehavior.DefaultProvider());
        Thread.currentThread().setContextClassLoader(_originalClassLoader);
        _unitClassLoader = null;
        _originalClassLoader = null;
    }

    /**
     * 自動フィールドバインディングが可能な場合<code>true</code>を返します.
     *
     * @param field
     *            フィールド
     * @return 自動フィールドバインディングが可能な場合<code>true</code>、そうでない場合<code>false</code>
     */
    protected boolean isAutoBindable(final Field field) {
        final int modifiers = field.getModifiers();
        return !Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)
                && !field.getType().isPrimitive();
    }

    /**
     * 指定されたフィールドに指定された値をバインディングします.
     *
     * @param field
     *            フィールド
     * @param object
     *            値
     */
    protected void bindField(final Field field, final Object object) {
        ReflectionUtil.setValue(field, _test, object);
    }

    /**
     * WARM deployが必要とされる場合{@code true}を返します.
     *
     * @return WARM deployが必要とされる場合{@code true}、そうでない場合{@code false}
     */
    protected boolean needsWarmDeploy() {
        return _introspector.needsWarmDeploy(_testClass, _method)
                && !ResourceUtil.isExist("s2container.dicon")
                && ResourceUtil.isExist("convention.dicon")
                && ResourceUtil.isExist("creator.dicon")
                && ResourceUtil.isExist("customizer.dicon");
    }
}
