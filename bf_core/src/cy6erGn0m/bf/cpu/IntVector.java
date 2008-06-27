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

/**
 *
 * @author cy6ergn0m
 */
public class IntVector {

    private int[] data;
    private int ptr = 0;

    public IntVector () {
        this(10);
    }

    public IntVector ( int size ) {
        data = new int[ size ];
    }

    private void ensureCapacity ( int capacity ) {
        if ( data.length < capacity ) {
            int[] aData = new int[ capacity + 10 ];
            System.arraycopy( data, 0, aData, 0, data.length );
            data = aData;
        }
    }

    public synchronized void add ( int number ) {
        final int sz = data.length;
        if ( ptr == sz - 1 )
            ensureCapacity( sz + ( sz >>> 1 ) );
        data[ptr++] = number;
    }

    public synchronized int size () {
        return ptr;
    }

    public int[] toArray () {
        final int sz = ptr;
        int[] result = new int[ sz ];
        System.arraycopy( data, 0, result, 0, sz );
        return result;
    }
    
    public synchronized void clear() {
        ptr = 0;
    }

    public int at( int index ) {
        if( index >= ptr )
            throw new IndexOutOfBoundsException();
        return data[ index ];
    }

    public void write( int index, int value ) {
        if( index >= ptr )
            throw new IndexOutOfBoundsException();
        data[ index ] = value;
    }
}
