/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cy6erGn0m.bf.cpu;

/**
 *
 * @author cy6ergn0m
 */
public class Memory16_2nd implements BfMemory {

     protected static final int BASE_MEMORY_SIZE = 0x4000;

    private class MemoryUnit {
        public final short[] data;

        public final int length;

        public MemoryUnit left = null;

        public MemoryUnit right = null;

        public MemoryUnit ( int size ) {
            data = new short[ length = size ];
        }
    }

    private MemoryUnit zeroUnit;

    private MemoryUnit current;

    private int currentLength;

    private short[] currentData;

    private int currentOffset;

    public Memory16_2nd () {
        this(BASE_MEMORY_SIZE);
    }

    public Memory16_2nd ( int size ) {
        zeroUnit = current = new MemoryUnit( currentLength = size );
        currentData = current.data;
    }

    public void teardown () {
        currentLength = currentOffset = 0;
        zeroUnit = current = null;
    }

    public void increase () {
        currentData[currentOffset] ++;
    }

    public void decrease () {
        currentData[currentOffset] --;
    }

    public void delta ( int delta ) {
        currentData[currentOffset] += delta;
    }

    private MemoryUnit alloc() {
        return new MemoryUnit( BASE_MEMORY_SIZE );
    }

    public void forward ( int delta ) {
        if( (currentOffset += delta) >= currentLength ) {
            do {
                currentOffset -= currentLength;
                if( current.right == null ) {
                    MemoryUnit u = alloc();
                    u.left = current;
                    current = current.right = u;
                } else
                    current = current.right;
                currentLength = current.length;
            } while( currentOffset >= currentLength );
            currentData = current.data;
        }
    }

    public void forward1 () {
        if( ++currentOffset == currentLength ) {
            currentOffset -= currentLength;
            if( current.right == null )
                current = current.right = alloc();
            else
                current = current.right;
            currentLength = current.length;
            currentData = current.data;
        }
    }

    public void backward ( int delta ) {
        if(( currentOffset -= delta ) < 0 ) {
            do {
                if( current.left == null ) {
                    MemoryUnit u = alloc();
                    u.right = current;
                    current = current.left = u;
                } else
                    current = current.left;
                currentLength = current.length;
                currentOffset += currentLength;
            } while( currentOffset < 0 );
            currentData = current.data;
        }
    }

    public void backward1 () {
        if( --currentOffset < 0 ) {
            if( current.left == null )
                current = current.left = alloc();
            else
                current = current.left;
            currentLength = current.length;
            currentOffset += currentLength;
            currentData = current.data;
        }
    }

    public int export () {
        return currentData[currentOffset];
    }

    public int[] dump () {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public int getAddress () {
        // find at right
        int offset = 0;
        MemoryUnit u = zeroUnit;
        for( ; u != current && u.right != null; u = u.right )
            offset += u.length;
        if( u == current )
            return offset + currentOffset;
        u = zeroUnit;
        offset = 0;
        for( ; u != current && u.left != null; u = u.right )
            offset -= u.length;
        return offset + currentOffset; // TODO: what about absolute address?
    }

    public void set ( int value ) {
        currentData[currentOffset] = (short) value;
    }

    public void zero () {
        currentData[currentOffset] = 0;
    }

    public boolean isZero () {
        return currentData[currentOffset] == 0;
    }

    public boolean isNonZero () {
        return currentData[currentOffset] != 0;
    }

    public void increaseAt ( int delta ) {
        int newOffset = currentOffset + delta;
        if( newOffset >= 0 && currentOffset < currentLength ) {
            currentData[newOffset] += currentData[currentOffset];
        } else {
            final MemoryUnit currentUnit = current;
            final int oldOffset = this.currentOffset;
            final short v = currentData[oldOffset];

            if( delta > 0 )
                forward( delta );
            else
                backward( -delta );

            //delta( v );
            currentData[currentOffset] += v;

            current = currentUnit;
            currentOffset = oldOffset;
            currentData = current.data;
            currentLength = current.length;
        }
        currentData[currentOffset] = 0;
    }

    public void increaseAt ( int[] deltas, int[] values ) {
        final int v = currentData[currentOffset];
        currentData[currentOffset] = 0;
        final MemoryUnit currentUnit = current;
        final int offset = currentOffset;

        int currentDelta = deltas[0];
        if( currentDelta > 0 )
            forward( currentDelta );
        else
            backward( -currentDelta );
        currentData[currentOffset] += values[0] * v;
        for ( int i = 1, m = deltas.length; i < m; ++i ) {
            final int cdelta = deltas[i];
            final int jmp;
            if( (jmp = cdelta - currentDelta) > 0 )
                forward( jmp );
            else
                backward( -jmp );
            currentDelta = cdelta;
            currentData[currentOffset] += values[i] * v;
        }

        current = currentUnit;
        currentOffset = offset;
        currentData = current.data;
        currentLength = current.length;
    }

    public void increaseAt ( int[] code, int base ) {
        final int v = currentData[currentOffset];
        currentData[currentOffset] = 0;
        final MemoryUnit currentUnit = current;
        final int offset = currentOffset;

        int m = code[base++];
        int currentDelta = code[base++];
        if( currentDelta > 0 )
            forward( currentDelta );
        else
            backward( -currentDelta );
        currentData[currentOffset] += code[base++] * v;
        for ( int i = 1; i < m; ++i ) {
            final int cdelta = code[base++];
            final int jmp;
            if( (jmp = cdelta - currentDelta) > 0 )
                forward( jmp );
            else
                backward( -jmp );
            currentDelta = cdelta;
            currentData[currentOffset] += code[base++] * v;
        }

        current = currentUnit;
        currentOffset = offset;
        currentData = current.data;
        currentLength = current.length;
    }
}
