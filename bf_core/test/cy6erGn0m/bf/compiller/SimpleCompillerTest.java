/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cy6erGn0m.bf.compiller;

import cy6erGn0m.bf.iset.Instruction;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author cy6ergn0m
 */
public class SimpleCompillerTest {

    protected Compiller c;

    public SimpleCompillerTest () {
        c = new SimpleCompiller();
    }

    @BeforeClass
    public static void setUpClass () throws Exception {
    }

    @AfterClass
    public static void tearDownClass () throws Exception {
    }

    @Test
    public void compileOnes () throws Exception {
        String code = "+";
        Instruction[] result;
        synchronized( c ) {
            result = c.compile( code );
        }
        assertEquals( 1, result.length );
        assertEquals( Instruction.INC_CODE, result[ 0 ].ival );
        
        code = "-";
        synchronized( c ) {
            result = c.compile( code );
        }
        assertEquals( 1, result.length );
        assertEquals( Instruction.DEC_CODE, result[ 0 ].ival );
        
    }
}
