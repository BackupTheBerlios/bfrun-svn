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
import java.io.IOException;

/**
 *
 * @author cy6ergn0m
 */
public class DebugProcessor extends Processor implements DebugPoint {
    public DebugProcessor( Instruction[] instructions, IOBus bus, BfMemory memory ) {
        super( instructions, bus, memory );
    }

    @Override
    public synchronized void perform () throws DebugException {
        lastDump = null;
        try {
            if( CP < instructions.length ) {
                _currentInstruction = instructions[ CP ];
                for( ; CP < instructions.length; ) {
                    if( interrupted ) {
                        interrupted = false;
                        throw new BreakpointException( CP, _currentInstruction );
                    }
                    performOne();
                    if( ++CP < instructions.length ) {
                        if( ( _currentInstruction = instructions[ CP ] ).bp )
                            throw new BreakpointException( CP, _currentInstruction );
                    }
                }
            }
        } catch ( DebugException e ) {
            e.setAddress( CP );
            throw e;
        } catch( IOException e ) {
            e.printStackTrace();
            throw new FatalException(CP, _currentInstruction, "I/O exception at " + e.getMessage() );
        }
    }

    @Override
    public synchronized void step () throws DebugException {
        lastDump = null;
        super.step();
    }

    @Override
    public synchronized void reset () {
        super.reset();
        lastDump = null;
    }
    
    // some debug features
    private Instruction findInstructionAt( int sourceIndex ) {
        for( Instruction i : instructions ) {
            if( i.sourceIndex >= sourceIndex )
                return i;
        }
        return ( instructions.length > 0 )? instructions[ instructions.length - 1 ] : null;
    }
    
    public Instruction setBreakpointAtSource( int sourceIndex ) {
        Instruction i = findInstructionAt( sourceIndex );
        if( i != null )
            i.bp = true;
        return i;
    }
    
    public Instruction clearBreakpointAt( int sourceIndex ) {
        Instruction i = findInstructionAt(sourceIndex);
        if( i != null )
            i.bp = false;
        return i;
    }
    
    public Instruction toggleBreakpointAt( int sourceIndex ) {
        Instruction i = findInstructionAt(sourceIndex);
        if( i != null )
            i.bp = !i.bp;
        return i;
    }

    public boolean isBreakpointAt ( int sourceIndex ) {
        Instruction i = findInstructionAt(sourceIndex);
        return ( i == null )? false : i.bp;
    }
    
    int[] lastDump = null;
    public synchronized int[] getFullMemDump() {
        if( lastDump == null )
            lastDump = memory.dump();
        return lastDump;
    }
    
    public int getDataAt( int rel ) {
        if( memory.getAddress() + rel < 0 )
            return 0;
        final int[] dump = getFullMemDump();
        final int addr = memory.getAddress() + rel;
        return ( addr > 0 && addr < dump.length )? dump[ addr ] : 0;
    }
    
    public int getCurrentData() {
        return memory.export();
    }
    
    public int getMemoryOnLeft() {
        return memory.getAddress();
    }
    
    public int getMemoryOnRight() {
        int[] dump = getFullMemDump();
        final int rs = dump.length - memory.getAddress() - 1;
        return ( rs > 0 )? rs : 0;
    }
}
