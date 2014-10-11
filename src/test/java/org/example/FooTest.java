package org.example;

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

@RunWith(Seasar24.class)
public class FooTest {
    private TestContext _context = null;
    private Date _now = null;

    @Rule
    public TestName _name = new TestName();
    @BeforeClass
    public static void beforeClass() {
        System.out.println("Before Class.");
    }
    @AfterClass
    public static void afterClass() {
        System.out.println("After Class.");
    }
    @Before
    public void before() {
        System.out.println("Before Method.");
        System.out.printf("Test Name is %s%n", _context.getTestMethodName());
        _context.register(new Date());
    }
    @After
    public void after() {
        System.out.println("After Method.");
    }

    @Test
    public void test1() {
        System.out.println("Test1.");
        System.out.printf("Test Name is %s%n", _name.getMethodName());
        System.out.println(_now);
    }
    @Test
    public void test2() {
        System.out.println("Test2.");
        System.out.printf("Test Name is %s%n", _name.getMethodName());
        System.out.println(_now);
    }
}
