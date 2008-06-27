/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cy6erGn0m.bf.compiller;

import cy6erGn0m.bf.cpu.IntVector;
import cy6erGn0m.bf.iset.Instruction;
import java.util.Stack;

/**
 *
 * @author cy6ergn0m
 */
public class AltcpuCompiller {
    public int[] compile( Instruction[] code ) {
        Stack<Integer> jumps = new Stack<Integer>();

        IntVector generated = new IntVector( code.length );
        int ival;
        for( Instruction i : code ) {
            generated.add( ival = i.ival );
            switch( ival ) {
                case Instruction.DATA_MOVE:
                    if( i.op != 0 )
                        generated.add( i.op );
                    else {
                        generated.add( 0 );
                        generated.add( i.extOps.length );
                        for( int j = 0, m = i.extOps.length; j < m; ++j ) {
                            generated.add( i.extOps[j] );
                            generated.add( i.extOps2[j] );
                        }
                    }
                    break;
                case Instruction.MOVE_PTR_CODE:
                case Instruction.MODIFY_CODE:
                    generated.add( i.op );
                    break;
                case Instruction.JUMP_ON_ZERO_CODE:
                    generated.add( 0 );
                    jumps.push( generated.size() );
                    break;
                case Instruction.JUMP_ON_NONZERO_CODE:
                    int p = jumps.pop();
                    generated.add( p );
                    generated.write( p - 1, generated.size() );
                    break;
            }
        }
        generated.add( -1 );
        return generated.toArray();
    }
}
