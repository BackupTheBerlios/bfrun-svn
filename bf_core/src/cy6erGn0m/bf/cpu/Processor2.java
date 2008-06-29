/***************************************************************************
 *   Copyright (C) 2008 by cy6ergn0m                                       *
 *                                                                         *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 ***************************************************************************/
package cy6erGn0m.bf.cpu;

import cy6erGn0m.bf.exception.BreakpointException;
import cy6erGn0m.bf.exception.FatalException;
import cy6erGn0m.bf.exception.DebugException;
import cy6erGn0m.bf.iset.Instruction;
import cy6erGn0m.bf.vm.NullBus;
import java.io.IOException;

/**
 *
 * @author cy6ergn0m
 */
public class Processor2 implements BfCpu {

    protected final IOBus bus;
    protected BfMemory memory;
    protected final Instruction[] instructions;
    protected int CP = 0;

    public Processor2 ( Instruction[] instructions, IOBus bus, BfMemory memory ) {
        if ( (this.instructions = instructions) == null )
            throw new NullPointerException( "instructions array should not be null" );
        this.bus = (bus != null) ? bus : new NullBus();
        this.memory = (memory != null) ? memory : new Memory8();
        initJumpsTable();
    }
    protected boolean interrupted = false;

    public void interrupt () {
        interrupted = true;
    }
    Instruction _currentInstruction = null;

    protected void performOne () throws DebugException, IOException {
        final Instruction currentInstruction = _currentInstruction;
        switch (currentInstruction.ival) {
            case Instruction.JUMP_BACKWARD_CODE:
                jumpBackward();
                break;
            case Instruction.DEC_CODE:
                memory.decrease();
                break;
            case Instruction.INC_CODE:
                memory.increase();
                break;
            case Instruction.MODIFY_CODE:
                memory.delta( currentInstruction.op );
                break;
            case Instruction.MOVE_PTR_CODE:
                final int op = currentInstruction.op;
                if ( op > 0 )
                    memory.forward( op );
                else {
                    try {
                        memory.backward( -op );
                    } catch (IndexOutOfBoundsException e) {
                        throw new FatalException( CP, currentInstruction, e.getMessage() );
                    }
                }
                break;
            case Instruction.FORWARD_CODE:
                memory.forward1();
                break;
            case Instruction.BACKWARD_CODE:
                try {
                    memory.backward1();
                } catch (IndexOutOfBoundsException e) {
                    throw new FatalException( CP, currentInstruction, e.getMessage() );
                }
                break;
            case Instruction.JUMP_FORWARD_CODE:
                jumpForward();
                break;
            case Instruction.ZERO_CODE:
                memory.zero();
                break;
            case Instruction.JUMP_ON_ZERO_CODE:
                if ( memory.isZero() )
                    CP = currentInstruction.op;
                break;
            case Instruction.JUMP_ON_NONZERO_CODE:
                if ( memory.isNonZero() )
                    CP = currentInstruction.op;
                break;
            case Instruction.DATA_MOVE:
                if ( currentInstruction.op != 0 )
                    memory.increaseAt( currentInstruction.op );
                else
                    memory.increaseAt( currentInstruction.extOps, currentInstruction.extOps2 );
                //memory.zero();
                break;
            case Instruction.OUT_CODE:
                bus.out( memory.export() );
                break;
            case Instruction.IN_CODE:
                memory.set( bus.in() );
                break;
            default:
                throw new FatalException( CP, currentInstruction, "unknown instruction" );
        }
    }

    private final void performSomething () throws DebugException {
        int cp = CP;
        Instruction currentInstruction = null;
        int iters = 0;
        try {
            final int m = instructions.length;
            if ( cp < m ) {
                int op;
                main:
                do {
                    switch ((currentInstruction = instructions[cp]).ival) {
                        case Instruction.MODIFY_CODE:
                            memory.delta( currentInstruction.op );
                            break;
                        case Instruction.MOVE_PTR_CODE:
                            if ( (op = currentInstruction.op) > 0 )
                                memory.forward( op );
                            else
                                memory.backward( -op );
                            if ( interrupted )
                                break main;
                            break;
                        case Instruction.ZERO_CODE:
                            memory.zero();
                            break;
                        case Instruction.JUMP_ON_ZERO_CODE:
                            if ( memory.isZero() )
                                cp = currentInstruction.op;
                            break;
                        case Instruction.JUMP_ON_NONZERO_CODE:
                            if ( memory.isNonZero() )
                                cp = currentInstruction.op;
                            break;
                        case Instruction.OUT_CODE:
                            if ( interrupted )
                                break main;
                            bus.out( memory.export() );
                            break;
                        case Instruction.DEC_CODE:
                            memory.decrease();
                            break;
                        case Instruction.INC_CODE:
                            memory.increase();
                            break;
                        case Instruction.JUMP_BACKWARD_CODE:
                            jumpBackward();
                            break;
                        case Instruction.DATA_MOVE:
                            if ( currentInstruction.op != 0 )
                                memory.increaseAt( currentInstruction.op );
                            else
                                memory.increaseAt( currentInstruction.extOps, currentInstruction.extOps2 );
//                                memory.zero();
                            break;
                        case Instruction.FORWARD_CODE:
                            memory.forward1();
                            break;
                        case Instruction.BACKWARD_CODE:
                            memory.backward1();
                            break;
                        case Instruction.JUMP_FORWARD_CODE:
                            jumpForward();
                            break;
                        case Instruction.IN_CODE:
                            if ( interrupted )
                                break main;
                            memory.set( bus.in() );
                            break;
                        default:
                            throw new FatalException( cp, currentInstruction, "unknown instruction" );
                    }
                } while ( ++cp < m && --iters < 1000 );
            }
        } catch (IOException e) {
            throw new FatalException( cp, currentInstruction, "I/O problem: " + e.getMessage() );
        } catch (IndexOutOfBoundsException e) {
            throw new FatalException( cp, (currentInstruction = instructions[cp]), e.getMessage() );
        } finally {
            CP = cp;
            _currentInstruction = currentInstruction;
        }
    }

    public synchronized void perform () throws DebugException {
        try {
            while ( !isEnd() )
                performSomething();
        } catch (IndexOutOfBoundsException e) {
            throw new FatalException( CP, (_currentInstruction = instructions[CP]), e.getMessage() );
        }
    }

    public boolean isEnd () {
        return (CP >= instructions.length);
    }

    public synchronized void step () throws DebugException {
        try {
            interrupted = false;
            if ( !isEnd() ) {
                _currentInstruction = instructions[CP];
                performOne();
                if ( ++CP < instructions.length )
                    throw new BreakpointException( CP, instructions[CP] );
            }
        } catch (DebugException ex) {
            ex.setAddress( CP );
            throw ex;
        } catch (IOException e) {
            throw new FatalException( CP, instructions[CP], "I/O problem: " + e.getMessage() );
        }
    }

    public synchronized void reset () {
        interrupt();
        memory.teardown();
        CP = 0;
    }
    private int[] jumpsTable = null;

    private void initJumpsTable () {
        if ( jumpsTable == null ) {
            final int m = instructions.length;
            jumpsTable = new int[ m ];
            for ( int i = 0; i < m; ++i )
                jumpsTable[i] = -1;
        }
    }

    protected void jumpForward () throws DebugException {
        int cp = CP;
        if ( memory.isZero() ) {
            int c = jumpsTable[cp];
            if ( c != -1 ) {
                cp = c;
            } else {
                int level = 0;
                int oldcp = cp++;
                final int m = instructions.length;
                loop:
                for (; cp < m; cp++ ) {
                    switch (instructions[cp].ival) {
                        case Instruction.JUMP_FORWARD_CODE:
                            level++;
                            break;
                        case Instruction.JUMP_BACKWARD_CODE:
                            if ( level == 0 )
                                break loop;
                            level--;
                            break;
                        case Instruction.JUMP_ON_ZERO_CODE:
                        case Instruction.JUMP_ON_NONZERO_CODE:
                            throw new FatalException( cp, instructions[cp], "mixed jumps forbidden" );
                    }
                }

                if ( cp >= m ) {
                    cp = oldcp;
                    throw new FatalException( oldcp, instructions[oldcp], "illegal forward jump" );
                }
                jumpsTable[oldcp] = cp;
                jumpsTable[cp] = oldcp;
            }
            CP = cp;
        }
    }

    protected void jumpBackward () throws DebugException {
        int cp = CP;
        int level = 0;
        final int oldcp = cp--;
        if ( oldcp > 0 ) {
            loop:
            do {
                switch (instructions[cp].ival) {
                    case Instruction.JUMP_BACKWARD_CODE:
                        level++;
                        break;
                    case Instruction.JUMP_FORWARD_CODE:
                        if ( level == 0 )
                            break loop;
                        level--;
                        break;
                }
            } while ( --cp >= 0 );
        }
        if ( cp < 0 ) {
            cp = oldcp;
            throw new FatalException( oldcp, instructions[oldcp], "illegal forward jump" );
        }
        jumpsTable[oldcp] = cp;
        jumpsTable[cp] = oldcp;
        CP = cp;
    }

    public Instruction getCurrentInstruction () {
        return (_currentInstruction != null) ? _currentInstruction : instructions[CP];
    }

    public int getCurrentAddress () {
        return CP;
    }
}
