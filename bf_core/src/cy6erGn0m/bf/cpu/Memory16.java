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

import java.util.Vector;


/**
 * This is not a random access memory! Only stream access. Memory32 has not fixed
 * size and auto expands to high addresses, but not expands to negative addresses.
 * @author cy6ergn0m
 */
public class Memory16 implements BfMemory {
    
    class MemoryUnit {

        public short data[] = null;
        private final int size;
        public MemoryUnit( int sizeToBeAllocated ) {
            if( sizeToBeAllocated <= 0 )
                throw new IllegalArgumentException( "size shoud be positive" );
            data = new short[ size = sizeToBeAllocated ];
        }

        public void teardown() {
            data = null;
        }

        public int getSize() {
            return size;
        }    
    }    
    
    protected static final int baseUnitSize = 16 * 1024;
    Vector<MemoryUnit> units = new Vector<MemoryUnit>( 16 );
    
    MemoryUnit currentUnit = null;
    int currentVectorIndex = 0;
    int currentBase = 0;
    int currentAddress = 0;
    int currentOffset = 0;
    int nextUnitSize;
    short currentValue = 0;
    boolean currentNonZero = false;
    
    protected class State {
        final MemoryUnit currentUnit;
        public final int currentVectorIndex;
        public final int currentBase;
        public final int currentAddress;
        public final int currentOffset;
        public final int nextUnitSize;
        State ( MemoryUnit currentUnit, int currentVectorIndex,
                       int currentBase, int currentAddress, int currentOffset,
                       int nextUnitSize ) {
            this.currentUnit = currentUnit;
            this.currentVectorIndex = currentVectorIndex;
            this.currentBase = currentBase;
            this.currentAddress = currentAddress;
            this.currentOffset = currentOffset;
            this.nextUnitSize = nextUnitSize;
        }
    }
    
    protected State saveState() {
        return new State(currentUnit, currentVectorIndex, currentBase, currentAddress, currentOffset, nextUnitSize );
    }
    
    protected void restoreState( State state ) {
        currentUnit.data[ currentOffset ] = currentValue;
        
        currentUnit = state.currentUnit;
        currentVectorIndex = state.currentVectorIndex;
        currentAddress = state.currentAddress;
        currentBase = state.currentBase;
        currentOffset = state.currentOffset;
        currentNonZero = ( currentValue = currentUnit.data[ currentOffset ] ) != 0;
    }
    
    public Memory16() {
        this( baseUnitSize );
    }
    
    public Memory16( int baseSize ) {
        nextUnitSize = baseSize;
        units.add( currentUnit = allocNext() );
    }
    
    public void teardown() {
        for( MemoryUnit u : units )
            u.teardown();
        units.clear();
        currentAddress = currentBase = currentVectorIndex = 0;
        currentOffset = 0;
        units.add( currentUnit = allocNext() );
        currentValue = 0;
        currentNonZero = false;
    }
    
    private MemoryUnit allocNext() {
        MemoryUnit unit = new MemoryUnit( nextUnitSize );
        nextUnitSize += nextUnitSize >> 1;
        return unit;
    }
    
    public void increase() {
        if( !currentNonZero ) {
            currentNonZero = true;
            ++currentValue;
        } else
            currentNonZero = ( ++currentValue != 0 );
    }
    
    public void decrease() {
        if( !currentNonZero ) {
            currentNonZero = true;
            --currentValue;
        } else
            currentNonZero = ( --currentValue != 0 );
    }
    
    public void delta( int delta ) {
        short d = (short)delta;
        if( !currentNonZero ) {
            currentValue += d;
            currentNonZero = true;
        } else
            currentNonZero = ( ( currentValue += d ) != 0 );
    }
    
    public void forward( int delta ) {
        if( delta != 0 ) {
            if( delta < 0 )
                backward( -delta );
            else {
                currentUnit.data[ currentOffset ] = currentValue;
                int newOffset = currentOffset + delta;
                int sz;
                while( newOffset >= ( sz = currentUnit.getSize() ) ) {
                    newOffset -= sz;
                    currentBase += sz;
                    if( ++currentVectorIndex >= units.size() )
                        units.add( currentUnit = allocNext() );
                    else
                        currentUnit = units.get( currentVectorIndex );
                }
                currentOffset = newOffset;
                currentAddress += delta;
                currentNonZero = ( ( currentValue = currentUnit.data[ currentOffset ] ) != 0 );
            }
        }
    }
    
    public void forward1() {
        currentUnit.data[ currentOffset ] = currentValue;
        int newOffset = currentOffset + 1;
        int sz;
        while( newOffset >= ( sz = currentUnit.getSize() ) ) {
            newOffset -= sz;
            currentBase += sz;
            if( ++currentVectorIndex >= units.size() )
                units.add( currentUnit = allocNext() );
            else
                currentUnit = units.get( currentVectorIndex );
        }
        currentOffset = newOffset;
        currentAddress ++;
        currentNonZero = ( ( currentValue = currentUnit.data[ currentOffset ] ) != 0 );
    }
    
    public void backward( int delta ) {
        if( delta != 0 ) {
            if( delta < 0 )
                forward( -delta );
            else {
                if( delta > currentAddress )
                    throw new IndexOutOfBoundsException( "current address is negative. aborting" );
                currentUnit.data[ currentOffset ] = currentValue;
                int newOffset = currentOffset - delta;
                int sz;
                while( newOffset < 0 ) {
                    newOffset += ( sz = ( currentUnit = units.get( --currentVectorIndex ) ).getSize() );
                    currentBase -= sz;
                }
                currentOffset = newOffset;
                currentAddress -= delta;
                currentNonZero = ( ( currentValue = currentUnit.data[ currentOffset ] ) != 0 );
            }
        }
    }
    
    public void backward1() {
        if( currentAddress == 0 )
            throw new IndexOutOfBoundsException( "current address is negative. aborting" );
        currentUnit.data[ currentOffset ] = currentValue;
        int newOffset = currentOffset - 1;
        int sz;
        while( newOffset < 0 ) {
            newOffset += ( sz = ( currentUnit = units.get( --currentVectorIndex ) ).getSize() );
            currentBase -= sz;
        }
        currentOffset = newOffset;
        currentAddress --;
        currentNonZero = ( ( currentValue = currentUnit.data[ currentOffset ] ) != 0 );
    }
    
    public int export() {
        return currentValue;
    }
    
    public int[] dump() {
        int size = 0;
        for( MemoryUnit u : units )
            size += u.size;
        int[] result = new int[ size ];
        int ptr = 0;
        for( MemoryUnit u : units ) {
            System.arraycopy( u.data, 0, result, ptr, u.size );
            ptr += u.size;
        }
        return result;
    }
    
    public int getAddress() {
        return currentAddress;
    }
    
    public void set( int value ) {
        currentNonZero = ( ( currentValue = (short)value ) != 0 );
    }
    
    public void zero() {
        currentValue = 0;
        currentNonZero = false;
    }
    
    public boolean isZero() {
        return !currentNonZero;
    }
    
    public boolean isNonZero() {
        return currentNonZero;
    }
    
    @Override
    public String toString() {
        return "address is " + currentAddress + " and value is " + export();
    }

    public void increaseAt ( int delta ) {
        final int v = currentValue;
        if( delta == 0 )
            increase();
        else {
            State st = saveState();
            forward(delta);
            delta( v );
            restoreState(st);
        }
    }
    
    public void increaseAt ( int[] deltas, int[] values ) {
        final int v = currentValue;
        State st = saveState();
        int current = 0;
        for( int i = 0, m = deltas.length; i < m; ++i ) {
            forward( deltas[ i ] - current );
            current = deltas[ i ];
            delta( values[i] * v );
        }
        restoreState(st);
    }
}
