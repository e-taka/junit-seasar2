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
import org.seasar.framework.util.StringUtil;
import org.seasar.framework.util.tiger.ReflectionUtil;

public class TestContextRule extends Statement {
    /** S2JUnit4のデフォルトの設定ファイルのパス */
    protected static final String DEFAULT_S2JUNIT4_PATH = "s2junit4.dicon";
    /** S2JUnit4の設定ファイルのパス */
    protected static String s2junit4Path = DEFAULT_S2JUNIT4_PATH;

    private final Statement _statement;

    /** テストオブジェクト */
    private final Object _test;
    /** テストクラス */
    private final Class<?> _testClass;
    /** テストメソッド */
    private final Method _method;

    /** テストクラスのイントロスペクター */
    private final S2TestIntrospector _introspector;
    /** {@link #_unitClassLoader テストで使用するクラスローダー}で置き換えられる前のオリジナルのクラスローダー */
    private ClassLoader _originalClassLoader;
    /** テストで使用するクラスローダー */
    private UnitClassLoader _unitClassLoader;
    /** S2JUnit4の内部的なテストコンテキスト */
    private InternalTestContext _testContext;

    public TestContextRule(
    		final Statement statement,
    		final Object target, final TestClass clazz, final FrameworkMethod method) {
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
     * テストコンテキストをセットアップします。
     *
     * @throws Throwable
     *             何らかの例外またはエラーが起きた場合
     */
    protected void setUpTestContext() throws Throwable {
        _originalClassLoader = getOriginalClassLoader();
        _unitClassLoader = new UnitClassLoader(_originalClassLoader);
        Thread.currentThread().setContextClassLoader(_unitClassLoader);
//        if (needsWarmDeploy()) {
//            S2ContainerFactory.configure("warmdeploy.dicon");
//        }
        final S2Container container = createRootContainer();
        SingletonS2ContainerFactory.setContainer(container);
        _testContext = InternalTestContext.class.cast(container
                .getComponent(InternalTestContext.class));
        _testContext.setTestClass(_testClass);
        _testContext.setTestMethod(_method);
        if (!_testContext.hasComponentDef(NamingConvention.class)
                && _introspector.isRegisterNamingConvention(_testClass, _method)) {
            final NamingConvention namingConvention = new NamingConventionImpl();
            _testContext.register(namingConvention);
            _testContext.setNamingConvention(namingConvention);
        }
        TestContextRepository.put(_testContext);

        for (Class<?> clazz = _testClass; clazz != Object.class; clazz = clazz
                .getSuperclass()) {

        	final Field[] fields = clazz.getDeclaredFields();
            for (int i = 0; i < fields.length; ++i) {
                final Field field = fields[i];
                final Class<?> fieldClass = field.getType();
                if (isAutoBindable(field)
                        && fieldClass.isAssignableFrom(_testContext.getClass())
                        && fieldClass
                                .isAnnotationPresent(PublishedTestContext.class)) {
                    field.setAccessible(true);
                    if (ReflectionUtil.getValue(field, _test) != null) {
                        continue;
                    }
                    bindField(field, _testContext);
                }
            }
        }
    }

    /**
     * オリジナルのクラスローダーを返します。
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
     * ルートのコンテナを返します。
     *
     * @return ルートのコンテナ
     */
    protected S2Container createRootContainer() {
        final String rootDicon = _introspector.getRootDicon(_testClass, _method);
        if (StringUtil.isEmpty(rootDicon)) {
            return S2ContainerFactory.create(s2junit4Path);
        }
        S2Container container = S2ContainerFactory.create(rootDicon);
        S2ContainerFactory.include(container, s2junit4Path);
        return container;

    }

    /**
     * テストコンテキストを解放します。
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
     * 自動フィールドバインディングが可能な場合<code>true</code>を返します。
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
     * 指定されたフィールドに指定された値をバインディングします。
     *
     * @param field
     *            フィールド
     * @param object
     *            値
     */
    protected void bindField(final Field field, final Object object) {
        ReflectionUtil.setValue(field, _test, object);
    }
}
