/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cy6erGn0m.bf.compiller;

import cy6erGn0m.bf.iset.Instruction;
import cy6erGn0m.bf.iset.InstructionSet;
import java.util.ArrayList;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author cy6ergn0m
 */
public class OptimizedCompillerTest {

    public OptimizedCompillerTest () {
    }

    @BeforeClass
    public static void setUpClass () throws Exception {
    }

    @AfterClass
    public static void tearDownClass () throws Exception {
    }

    @Test
    public void testIndexOf () throws Exception {
        ArrayList<Instruction> arr = new ArrayList<Instruction>();
        arr.add( new Instruction( InstructionSet.JUMP_FORWARD, 0, 0 ) );
        arr.add( new Instruction( InstructionSet.DEC, 0, 0 ) );
        arr.add( new Instruction( InstructionSet.JUMP_BACKWARD, 0, 0 ) );

        Instruction[] toBeFound = {new Instruction( InstructionSet.JUMP_FORWARD, 0, 0 ),
            new Instruction( InstructionSet.DEC, 0, 0 ),
            new Instruction( InstructionSet.JUMP_BACKWARD, 0, 0 )
        };

        int index = new OptimizedCompiller().indexOf( arr, toBeFound, 0 );

        if ( index != 0 )
            fail( "bad indexOf() result, should be 0, but " + index + " was returned" );
    }

    @Test
    public void testIndexOf2 () throws Exception {
        ArrayList<Instruction> arr = new ArrayList<Instruction>();
        arr.add( new Instruction( InstructionSet.INC, 0, 0 ) );
        arr.add( new Instruction( InstructionSet.INC, 0, 0 ) );
        arr.add( new Instruction( InstructionSet.INC, 0, 0 ) );
        arr.add( new Instruction( InstructionSet.JUMP_FORWARD, 0, 0 ) );
        arr.add( new Instruction( InstructionSet.DEC, 0, 0 ) );
        arr.add( new Instruction( InstructionSet.JUMP_BACKWARD, 0, 0 ) );
        arr.add( new Instruction( InstructionSet.INC, 0, 0 ) );
        arr.add( new Instruction( InstructionSet.INC, 0, 0 ) );


        Instruction[] toBeFound = {
            new Instruction( InstructionSet.JUMP_FORWARD, 0, 0 ),
            new Instruction( InstructionSet.DEC, 0, 0 ),
            new Instruction( InstructionSet.JUMP_BACKWARD, 0, 0 )
        };

        int index = new OptimizedCompiller().indexOf( arr, toBeFound, 0 );

        if ( index != 3 )
            fail( "bad indexOf() result, should be 0, but " + index + " was returned" );
    }
}