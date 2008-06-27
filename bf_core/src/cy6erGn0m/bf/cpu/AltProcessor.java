/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cy6erGn0m.bf.cpu;

import cy6erGn0m.bf.cpu.BfCpu;
import cy6erGn0m.bf.cpu.BfMemory;
import cy6erGn0m.bf.cpu.IOBus;
import cy6erGn0m.bf.exception.DebugException;
import cy6erGn0m.bf.iset.Instruction;
import java.io.IOException;

/**
 *
 * @author cy6ergn0m
 */
public class AltProcessor implements BfCpu {

    private BfMemory memory;
    private IOBus bus;
    private int[] code;
    private int CP = 0;

    public AltProcessor ( BfMemory memory, IOBus bus, int[] code ) {
        this.memory = memory;
        this.bus = bus;
        this.code = code;
    }

    public void perform () throws DebugException {
        try {

            int cp = CP;
            main:
            do {
                int icode = code[cp++];
                switch (icode) {
                    case Instruction.ZERO_CODE:
                        memory.zero();
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
                    case Instruction.FORWARD_CODE:
                        memory.forward1();
                        break;
                    case Instruction.MOVE_PTR_CODE:
                        int value = code[cp++];
                        if ( value > 0 )
                            memory.forward( value );
                        else
                            memory.backward( -value );
                        break;
                    case Instruction.MODIFY_CODE:
                        memory.delta( code[cp++] );
                        break;
                    case Instruction.DATA_MOVE:
                        int op = code[cp++];
                        if ( op != 0 )
                            memory.increaseAt( op );
                        else {
                            op = code[cp++]; // size
                            int[] ops1 = new int[ op ];
                            int[] ops2 = new int[ op ];
                            for ( int i = 0; i < op; ++i ) {
                                ops1[i] = code[cp++];
                                ops2[i] = code[cp++];
                            }
                            memory.increaseAt( ops1, ops2 );
                        }
                        break;
                    case Instruction.JUMP_ON_ZERO_CODE:
                        if ( memory.isZero() )
                            cp = code[cp];
                        else
                            cp++;
                        break;
                    case Instruction.JUMP_ON_NONZERO_CODE:
                        if ( memory.isNonZero() )
                            cp = code[cp];
                        else
                            cp++;
                        break;
                    case Instruction.OUT_CODE:
                        bus.out( memory.export() );
                        break;
                    case Instruction.IN_CODE:
                        memory.set( bus.in() );
                        break;
                    case -1:
                        break main;
                    default:
                        throw new IllegalArgumentException();
                }
            } while ( true );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void interrupt () {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public void step () throws DebugException {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public void reset () {
        CP = 0;
    }

    public Instruction getCurrentInstruction () {
        throw new UnsupportedOperationException( "Not supported yet." );
    }
}
