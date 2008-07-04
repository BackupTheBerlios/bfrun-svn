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
    
    private static int[] perform( String code, int level ) throws CompillingException, DebugException {
        OptimizedCompiller c = new OptimizedCompiller();
        c.setOptimizationLevel( level );
        Instruction is[] = c.compile( code );
        BfMemory m = new Memory8();
        IOBus b = new NullBus();
        Processor p = new Processor( is, b, m );
        p.perform();
        return m.dump();
    }

    private static int[] perform2ndmem( String code, int level ) throws CompillingException, DebugException {
        OptimizedCompiller c = new OptimizedCompiller();
        c.setOptimizationLevel( level );
        Instruction is[] = c.compile( code );
        BfMemory m = new Memory8_2nd();
        IOBus b = new NullBus();
        Processor p = new Processor( is, b, m );
        p.perform();
        return m.dump();
    }
    
    @Test
    public void testSimple() throws CompillingException, DebugException {
        String code = "++++[->++<].++.";
        int[] result = perform(code, 0);
        int[] expected = new int[ result.length ];
        expected[ 0 ] = 2;
        expected[ 1 ] = 8;
        assertTrue( dumpEquals(result, expected) );
    }

    @Test
    public void testSimple1_1() throws CompillingException, DebugException {
        String code = "++++[->++<[-][-]].>++";
        int[] result = perform(code, 0);
        int[] expected = new int[ result.length ];
        expected[ 0 ] = 0;
        expected[ 1 ] = 4;
        assertTrue( dumpEquals(result, expected) );
    }

    @Test
    public void testSimple2() throws CompillingException, DebugException {
        String code = "++++[->++<].";
        int[] result = perform(code, 1);
        int[] expected = new int[ result.length ];
        expected[ 0 ] = 0;
        expected[ 1 ] = 8;
        assertTrue( dumpEquals(result, expected) );
    }

    @Test
    public void testSimple3() throws CompillingException, DebugException {
        String code = "++++[->++<].";
        int[] result = perform(code, 2);
        int[] expected = new int[ result.length ];
        expected[ 0 ] = 0;
        expected[ 1 ] = 8;
        assertTrue( dumpEquals(result, expected) );
    }

    @Test
    public void testSimple4() throws CompillingException, DebugException {
        String code = "++++[->++<].";
        int[] result = perform(code, 3);
        int[] expected = new int[ result.length ];
        expected[ 0 ] = 0;
        expected[ 1 ] = 8;
        assertTrue( dumpEquals(result, expected) );
    }

    @Test
    public void testSimple5() throws CompillingException, DebugException {
        String code = "++++[->++<].";
        int[] result = perform2ndmem(code, 0);
        int[] expected = new int[ result.length ];
        expected[ 0 ] = 0;
        expected[ 1 ] = 8;
        assertTrue( dumpEquals(result, expected) );
    }

    @Test
    public void testSimple6() throws CompillingException, DebugException {
        String code = "++++[->++<].";
        int[] result = perform2ndmem(code, 1);
        int[] expected = new int[ result.length ];
        expected[ 0 ] = 0;
        expected[ 1 ] = 8;
        assertTrue( dumpEquals(result, expected) );
    }

    @Test
    public void testSimple7() throws CompillingException, DebugException {
        String code = "++++[->++<].";
        int[] result = perform2ndmem(code, 2);
        int[] expected = new int[ result.length ];
        expected[ 0 ] = 0;
        expected[ 1 ] = 8;
        assertTrue( dumpEquals(result, expected) );
    }

    @Test
    public void testSimple8() throws CompillingException, DebugException {
        String code = "++++[->++<].";
        int[] result = perform2ndmem(code, 3);
        int[] expected = new int[ result.length ];
        expected[ 0 ] = 0;
        expected[ 1 ] = 8;
        assertTrue( dumpEquals(result, expected) );
    }

}
