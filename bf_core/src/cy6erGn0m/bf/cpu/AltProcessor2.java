/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cy6erGn0m.bf.cpu;

import cy6erGn0m.bf.exception.DebugException;
import cy6erGn0m.bf.iset.Instruction;
import java.io.IOException;

/**
 *
 * @author cy6ergn0m
 */
public class AltProcessor2 implements BfCpu {

    private BfMemory memory;
    private IOBus bus;
    private int[] code;
    private int CP = 0;

    public AltProcessor2 ( BfMemory memory, IOBus bus, int[] code ) {
        this.memory = memory;
        this.bus = bus;
        this.code = code;
    }

    public void interrupt () {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public void step () throws DebugException {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public void reset () {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public Instruction getCurrentInstruction () {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    private boolean end = false;

    private final void performZeroed () throws IOException {
        int cp = CP;
        main_zeroed:
        do {
            int icode = code[cp++];
            switch (icode) {
                case Instruction.MOVE_PTR_CODE: {
                    int value = code[cp++];
                    if ( value > 0 )
                        memory.forward( value );
                    else
                        memory.backward( -value );
                    if ( memory.isNonZero() ) {
                        break main_zeroed;
                    }
                    break;
                }
                case Instruction.DATA_MOVE:
                    int op = code[cp++];
                    if ( op == 0 ) {
                        op = code[cp++]; // size
                        cp += op << 1;
                    }
                    break;
                case Instruction.ZERO_CODE:
                    break;
                case Instruction.JUMP_ON_ZERO_CODE: {
                    cp = code[cp];
                    break;
                }
                case Instruction.JUMP_ON_NONZERO_CODE:
                    while ( code[++cp] == Instruction.JUMP_ON_NONZERO_CODE )
                        cp++;
                    break;
                case Instruction.FORWARD_CODE:
                    memory.forward1();
                    if ( memory.isNonZero() ) {
                        break main_zeroed;
                    }
                    break;
                case Instruction.BACKWARD_CODE:
                    memory.backward1();
                    if ( memory.isNonZero() ) {
                        break main_zeroed;
                    }
                    break;
                case Instruction.DEC_CODE:
                    memory.decrease();
                    break main_zeroed;
                case Instruction.INC_CODE:
                    memory.increase();
                    break main_zeroed;
                case Instruction.MODIFY_CODE:
                    memory.delta( code[cp++] );
                    break main_zeroed;
                case Instruction.OUT_CODE:
                    bus.out( 0 );
                    break;
                case Instruction.IN_CODE: {
                    int val = bus.in();
                    if ( val == 0 )
                        memory.zero();
                    else {
                        memory.set( val );
                        break main_zeroed;
                    }
                    break;
                }
                case -1:
                    end = true;
                    break main_zeroed;
                default:
                    throw new IllegalArgumentException();
            }
        } while ( true );
        CP = cp;
    }

    private final void performGeneric () throws IOException {
        int cp = CP;
        main_nz:
        do {
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
                    break main_nz;
                case Instruction.ZERO_CODE: {
                    memory.zero();
                    if( code[cp] != Instruction.INC_CODE )
                        break main_nz;
                    break;
                }
                case Instruction.JUMP_ON_ZERO_CODE: {
                    if ( memory.isZero() ) {
                        cp = code[cp];
                        break main_nz;
                    } else {
                        while ( code[++cp] == Instruction.JUMP_ON_ZERO_CODE )
                            cp++;
                    }
                    break;
                }
                case Instruction.JUMP_ON_NONZERO_CODE:
                    if ( memory.isNonZero() )
                        cp = code[cp];
                    else {
                        while ( code[++cp] == Instruction.JUMP_ON_NONZERO_CODE )
                            cp++;
                        break main_nz;
                    }
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
                case Instruction.OUT_CODE:
                    bus.out( memory.export() );
                    break;
                case Instruction.IN_CODE:
                    int val = bus.in();
                    if ( val == 0 ) {
                        memory.zero();
                        break main_nz;
                    }
                    memory.set( val );
                    break;
                case -1:
                    end = true;
                    break main_nz;
                default:
                    throw new IllegalArgumentException();
            }
        } while ( true );
        CP = cp;
    }

    private final boolean doIt() throws IOException {
        int c = 0;
        do {
            performZeroed();
            if( end )
                return false;
            performGeneric();
        } while( !end && ++c < 1024 );
        return !end;
    }

    public void perform () throws DebugException {
        try {
            do {
            } while( doIt() ); // ~ 8M switches...
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
