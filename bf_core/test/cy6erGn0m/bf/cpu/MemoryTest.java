/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cy6erGn0m.bf.cpu;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author cy6ergn0m
 */
public class MemoryTest {

    Memory32 memory = new Memory32( 20 );
    
    public MemoryTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void bigTest() {
        for( int i = 0; i < 10000000; ++i ) {
            assertTrue( memory.isZero() );
            memory.increase(); // +0
            assertTrue( memory.isNonZero() );
            memory.decrease();
            memory.forward( 1 ); // +1
            assertTrue( memory.isZero() );
            memory.decrease();
            assertTrue( memory.isNonZero() );
            memory.forward( 4 ); // +5
            memory.increase();
            memory.forward( 17 ); // +22
            memory.increase();
            memory.backward( 21 ); // +1
            assertTrue( memory.isNonZero() );
            memory.increase();
            memory.backward( 1 ); // +0
            assertTrue( memory.isZero() );
        }
    }


}