/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cy6erGn0m.bf.cpu;

import cy6erGn0m.bf.exception.DebugException;
import cy6erGn0m.bf.iset.Instruction;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author cy6ergn0m
 */
public class AltProcessor implements BfCpu {

    private BfMemory memory;
    private IOBus bus;
    private int[] code;
    private int cp = 0;

    public AltProcessor ( BfMemory memory, IOBus bus, int[] code ) {
        this.memory = memory;
        this.bus = bus;
        this.code = code;
    }

    private final boolean doOne () {
        int cp = this.cp;
        for ( int i = 0; i < 100; ++i ) {
            int icode = code[cp++];
            switch (icode) {
                case Instruction.MOVE_PTR_CODE: {
                    int value = code[cp++];
                    if ( value > 0 )
                        memory.forward( value );
                    else
                        memory.backward( -value );
                    break;
                }
                case Instruction.DATA_MOVE:
                    int op = code[cp++];
                    if ( op != 0 )
                        memory.increaseAt( op );
                    else {
                        op = code[cp]; // size
                        memory.increaseAt( code, cp );
                        cp += (op << 1) + 1;
                    }
                    break;
                case Instruction.ZERO_CODE: {
                    memory.zero();
                    break;
                }
                case Instruction.JUMP_ON_ZERO_CODE: {
                    if ( memory.isZero() )
                        cp = code[cp];
                    else {
                        while ( code[++cp] == Instruction.JUMP_ON_ZERO_CODE )
                            cp++;
                    }
                    break;
                }
                case Instruction.JUMP_ON_NONZERO_CODE:
                    if ( memory.isNonZero() )
                        cp = code[cp];
                    else
                        while ( code[++cp] == Instruction.JUMP_ON_NONZERO_CODE )
                            cp++;
                    break;
                case Instruction.FORWARD_CODE:
                    memory.forward1();
                    break;
                case Instruction.BACKWARD_CODE:
                    memory.backward1();
                    break;
                case Instruction.DEC_CODE:
                    memory.decrease();
                    break;
                case Instruction.INC_CODE:
                    memory.increase();
                    break;
                case Instruction.MODIFY_CODE:
                    memory.delta( code[cp++] );
                    break;
                case Instruction.OUT_CODE: {
                    try {
                        bus.out( memory.export() );
                    } catch (IOException ex) {
                        Logger.getLogger( AltProcessor.class.getName() ).log( Level.SEVERE, null, ex );
                    }
                    break;
                }
                case Instruction.IN_CODE: {
                    try {
                        memory.set( bus.in() );
                    } catch (IOException ex) {
                        Logger.getLogger( AltProcessor.class.getName() ).log( Level.SEVERE, null, ex );
                    }
                    break;
                }
                case -1:
                    return false;
                default:
                    throw new IllegalArgumentException();
            }
        }
        this.cp = cp;
        return true;
    }

    private final boolean perform100 () {
        int i = 0;
        do {
        } while ( ++i < 1000 && doOne() );
        return i == 1000;
    }

    public void perform () throws DebugException {
        do {
        } while ( perform100() );
    }

    public void interrupt () {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public void step () throws DebugException {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public void reset () {
        cp = 0;
    }

    public Instruction getCurrentInstruction () {
        throw new UnsupportedOperationException( "Not supported yet." );
    }
}
