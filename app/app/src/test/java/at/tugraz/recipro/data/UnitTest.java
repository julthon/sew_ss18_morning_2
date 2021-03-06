package at.tugraz.recipro.data;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UnitTest {

    private final String SHORT_NAME = "ml";
    private Unit unit;

    @Before
    public void setUpUnit() {

        this.unit = Unit.MILLILITER;
    }

    @Test
    public void testUnitToStringStuff() {

        assertEquals(SHORT_NAME, this.unit.toString());
    }

}