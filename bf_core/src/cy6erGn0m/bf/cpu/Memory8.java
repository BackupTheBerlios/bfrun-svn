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

import java.util.ArrayList;

/**
 * This is not a random access memory! Only stream access. Memory32 has not fixed
 * size and auto expands to high addresses, but not expands to negative addresses.
 * @author cy6ergn0m
 */
public class Memory8 implements BfMemory {

    class MemoryUnit {

        public byte data[] = null;
        private final int size;

        public MemoryUnit ( int sizeToBeAllocated ) {
            if ( sizeToBeAllocated <= 0 )
                throw new IllegalArgumentException( "size shoud be positive" );
            data = new byte[ size = sizeToBeAllocated ];
        }

        public void teardown () {
            data = null;
        }

        public int getSize () {
            return size;
        }
    }
    private static final int baseUnitSize = 16 * 1024;
    private ArrayList<MemoryUnit> units = new ArrayList<MemoryUnit>( 16 );
    private MemoryUnit currentUnit = null;
    private int currentVectorIndex = 0;
    private int currentBase = 0;
    //int currentAddress = 0;
    private int currentOffset = 0;
    private int nextUnitSize;
    private byte currentValue = 0;
    private boolean currentNonZero = false;
    private  MemoryUnit _currentUnit;
    private int _currentVectorIndex;
    private int _currentBase;
    private int _currentOffset;
    private int _nextUnitSize;

    private void set ( Memory8.MemoryUnit currentUnit, int currentVectorIndex,
               int currentBase, int currentOffset,
               int nextUnitSize ) {
        _currentUnit = currentUnit;
        _currentVectorIndex = currentVectorIndex;
        _currentBase = currentBase;
        _currentOffset = currentOffset;
        _nextUnitSize = nextUnitSize;
    }

    private void save () {
        _currentUnit = currentUnit;
        _currentVectorIndex = currentVectorIndex;
        _currentBase = currentBase;
        _currentOffset = currentOffset;
        _nextUnitSize = nextUnitSize;
    }

    private void restoreZero() {
        currentUnit.data[currentOffset] = currentValue;

        currentUnit = _currentUnit;
        currentVectorIndex = _currentVectorIndex;
        currentBase = _currentBase;
        currentOffset = _currentOffset;
        currentNonZero = false;
        currentValue = /*currentUnit.data[currentOffset] =*/  0;
    }

    public Memory8 () {
        this( baseUnitSize );
    }

    public Memory8 ( int baseSize ) {
        nextUnitSize = baseSize;
        units.add( currentUnit = allocNext() );
//        currentOffset = baseSize >> 1;
    }

    public void teardown () {
        for ( MemoryUnit u : units )
            u.teardown();
        units.clear();
        currentBase = currentVectorIndex = 0;
        currentOffset = 0;
        units.add( currentUnit = allocNext() );
        currentValue = 0;
        currentNonZero = false;
    }

    private MemoryUnit allocNext () {
        MemoryUnit unit = new MemoryUnit( nextUnitSize );
        nextUnitSize += nextUnitSize >> 1;
        return unit;
    }

    public void increase () {
        currentNonZero = ( ++currentValue != 0 );
    }

    public void decrease () {
        currentNonZero = ( --currentValue != 0 );
    }

    public void delta ( int delta ) {
        currentNonZero = ( ( currentValue += delta ) != 0 );
    }

    public void forward ( int delta ) {
        assert delta > 0;
        currentUnit.data[currentOffset] = currentValue;
        int newOffset = currentOffset + delta;
        int sz;
        while ( newOffset >= ( sz = currentUnit.getSize() ) ) {
            newOffset -= sz;
            currentBase += sz;
            if ( ++currentVectorIndex >= units.size() )
                units.add( currentUnit = allocNext() );
            else
                currentUnit = units.get( currentVectorIndex );
        }
        currentNonZero = ( ( currentValue = currentUnit.data[currentOffset = newOffset] ) != 0 );
    }

    public void forward1 () {
        currentUnit.data[currentOffset] = currentValue;
        int newOffset = currentOffset + 1;
        int sz;
        while ( newOffset >= ( sz = currentUnit.getSize() ) ) {
            newOffset -= sz;
            currentBase += sz;
            if ( ++currentVectorIndex >= units.size() )
                units.add( currentUnit = allocNext() );
            else
                currentUnit = units.get( currentVectorIndex );
        }
        currentNonZero = ( ( currentValue = currentUnit.data[currentOffset = newOffset] ) != 0 );
    }

    public void backward ( int delta ) {
        assert delta > 0;
        currentUnit.data[currentOffset] = currentValue;
        int newOffset = currentOffset - delta;
        int sz;
        while ( newOffset < 0 ) {
            if ( currentVectorIndex < 0 )
                throw new IndexOutOfBoundsException( "current address is negative. aborting" );
            newOffset += ( sz = ( currentUnit = units.get( --currentVectorIndex ) ).getSize() );
            currentBase -= sz;
        }
        currentNonZero = ( ( currentValue = currentUnit.data[currentOffset = newOffset] ) != 0 );
    }

    public void backward1 () {
        int newOffset = currentOffset ;
        currentUnit.data[newOffset--] = currentValue;
        if ( newOffset < 0 ) {
            if ( currentBase == 0 )
                throw new IndexOutOfBoundsException( "current address is negative. aborting" );
            int sz;
            do {
                newOffset += ( sz = ( currentUnit = units.get( --currentVectorIndex ) ).getSize() );
                currentBase -= sz;
            } while( newOffset < 0 );
        }
        currentNonZero = ( ( currentValue = currentUnit.data[currentOffset = newOffset] ) != 0 );
    }

    public int export () {
        return currentValue;
    }

    public int[] dump () {
        // flush cache
        currentUnit.data[currentOffset] = currentValue;

        int size = 0;
        for ( MemoryUnit u : units )
            size += u.size;
        final int[] result = new int[ size ];
        int ptr = 0;
        for ( MemoryUnit u : units ) {
            //System.arraycopy( u.data, 0, result, ptr, u.size ); // type mismatch
            final byte[] data = u.data;
            final int m = data.length;
            for ( int i = 0; i < m; ++i )
                result[ptr++] = data[i];
        }
        return result;
    }

    public int getAddress () {
        return currentBase + currentOffset;
    }

    public void set ( int value ) {
        currentNonZero = ( ( currentValue = (byte) value ) != 0 );
    }

    public void zero () {
        currentValue = 0;
        currentNonZero = false;
    }

    public boolean isZero () {
        return !currentNonZero;
    }

    public boolean isNonZero () {
        return currentNonZero;
    }

    @Override
    public String toString () {
        return "address is " + getAddress() + " and value is " + export();
    }

    public void increaseAt ( int delta ) {
        int expOffs = currentOffset + delta;
        if( expOffs >= 0 && expOffs < currentUnit.size ) {
            currentUnit.data[ expOffs ] += currentValue;
            currentValue = 0;
            currentNonZero = false;
        } else {
            final int v = currentValue;
            save();
            if( delta > 0 )
                forward(delta);
            else
                backward(-delta);
            delta( v );
            restoreZero();
        }
    }

    public void increaseAt ( int[] deltas, int[] values ) {
        final int v = currentValue;
        save();
        int current = deltas[0];
        if( current > 0 )
            forward( current );
        else
            backward( -current );
        delta( values[0] * v );
        for ( int i = 1, m = deltas.length; i < m; ++i ) {
            final int cdelta = deltas[i];
            final int jmp;
            if( (jmp = cdelta - current) > 0 )
                forward( jmp );
            else
                backward( -jmp );
            current = cdelta;
            delta( values[i] * v );
        }
        restoreZero();
    }

    public void increaseAt ( int[] code, int base ) {
        int op = code[base++]; // size
        int[] ops1 = new int[ op ];
        int[] ops2 = new int[ op ];
        for ( int i = 0; i < op; ++i ) {
            ops1[i] = code[base++];
            ops2[i] = code[base++];
        }
        increaseAt( ops1, ops2 );
    }
}
