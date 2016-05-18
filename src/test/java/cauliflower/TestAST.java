package cauliflower;

import cauliflower.cflr.Label;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TestAST {

    @Test
    public void testNewLabelHasDomains(){
        Label l = new Label(0, 1);
        assertTrue(l.fromDomain == 0);
        assertTrue(l.toDomain == 1);
    }

    @Test
    public void testNewLabelNoFields(){
        Label l = new Label(0, 1);
        assertTrue(l.fDomains.size() == 0);
    }

    @Test
    public void testNewLabelSomeFields(){
        Label l = new Label(0, 1, 1, 2, 3, 4);
        assertTrue(l.fDomains.size() == 4);
        assertTrue(l.fDomains.get(0) == 1);
        assertTrue(l.fDomains.get(1) == 2);
        assertTrue(l.fDomains.get(2) == 3);
        assertTrue(l.fDomains.get(3) == 4);
    }

}
