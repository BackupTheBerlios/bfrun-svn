/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cy6erGn0m.bf.cpu;

import cy6erGn0m.bf.compiller.CompillingException;
import cy6erGn0m.bf.compiller.OptimizedCompiller;
import cy6erGn0m.bf.exception.DebugException;
import cy6erGn0m.bf.iset.Instruction;
import cy6erGn0m.bf.vm.NullBus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author cy6ergn0m
 */
public class ProcessorTest {

    public ProcessorTest() {
    }

    @BeforeClass
    public static void setUpClass () throws Exception {
    }

    @AfterClass
    public static void tearDownClass () throws Exception {
    }

    public static boolean dumpEquals ( int[] one, int[] two ) {
        if ( one == null && two == null )
            return true;
        if ( one == null || two == null )
            return false;
        if ( one.length != two.length )
            return false;
        for ( int i = 0,  m = one.length; i < m; ++i ) {
            if ( one[i] != two[i] )
                return false;
        }
        return true;
    }
    
    public static int[] perform( String code ) throws CompillingException, DebugException {
        OptimizedCompiller c = new OptimizedCompiller();
        Instruction is[] = c.compile( code );
        BfMemory m = new Memory8();
        IOBus b = new NullBus();
        Processor p = new Processor( is, b, m );
        p.perform();
        return m.dump();
    }
    
    @Test
    public void testSimple() throws CompillingException, DebugException {
        String code = "++++[->++<]";
        int[] result = perform(code);
        int[] expected = new int[ result.length ];
        expected[ 0 ] = 0;
        expected[ 1 ] = 8;
        assertTrue( dumpEquals(result, expected) );
    }

}