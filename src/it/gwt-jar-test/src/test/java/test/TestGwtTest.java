package test;

import com.google.gwt.junit.client.GWTTestCase;

import walkingkooka.j2cl.locale.LocaleAware;
import walkingkooka.spreadsheet.webworker.Main;

@LocaleAware
public class TestGwtTest extends GWTTestCase {

    @Override
    public String getModuleName() {
        return "test.Test";
    }

    public void testAssertEquals() {
        assertEquals(
                1,
                1
        );
    }

    public void testStart() {
        try {
            new Main().onModuleLoad();
            fail("IllegalArgumentException should have been thrown");
        } catch (final IllegalArgumentException expected) {
        }
    }
}
