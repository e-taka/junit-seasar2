package org.example;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.seasar.framework.unit.Seasar24;
import org.seasar.framework.unit.TestContext;

/**
 * サンプルコード.
 */
@RunWith(Seasar24.class)
public class FooTest {
    /** テストコンテキスト */
    private TestContext _context = null;
    /** DI 対象のオブジェクト */
    private Date _now = null;

    /** ルールを実行するか確認する */
    @Rule
    public TestName _name = new TestName();

    /**
     * テストメソッド実行前に実行する処理.
     */
    @BeforeClass
    public static void beforeClass() {
        System.out.println("Before Class.");
    }
    /**
     * テストメソッド実行後に実行する処理.
     */
    @AfterClass
    public static void afterClass() {
        System.out.println("After Class.");
    }

    /**
     * 各テストメソッド実行前に実行する処理.
     */
    @Before
    public void before() {
        System.out.println("Before Method.");
        System.out.printf("Test Name is %s%n", _context.getTestMethodName());
        _context.register(new Date());
    }
    /**
     * 各テストメソッド実行後に実行する処理.
     */
    @After
    public void after() {
        System.out.println("After Method.");

        assertThat(_now, is(nullValue()));
    }

    /**
     * サンプルのテスト.
     */
    @Test
    public void test1() {
        System.out.println("Test1.");
        System.out.printf("Test Name is %s%n", _name.getMethodName());
        System.out.println(_now);

        assertThat(_now, is(notNullValue()));
        assertThat(_name.getMethodName(), is("test1"));
    }

    /**
     * サンプルのテスト.
     */
    @Test
    public void test2() {
        System.out.println("Test2.");
        System.out.printf("Test Name is %s%n", _name.getMethodName());
        System.out.println(_now);

        assertThat(_now, is(notNullValue()));
        assertThat(_name.getMethodName(), is("test2"));
    }
}
