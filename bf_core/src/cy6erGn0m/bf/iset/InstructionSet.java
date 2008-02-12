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
package cy6erGn0m.bf.iset;

/**
 *
 * @author cy6ergn0m
 */
public enum InstructionSet {

    NONE( -1, "*NONE*" ),
    INC( Instruction.INC_CODE, '+' ),
    DEC( Instruction.DEC_CODE, '-' ),
    MODIFY( Instruction.MODIFY_CODE, "*MODIFY*" ),
    ZERO( Instruction.ZERO_CODE, "*ZERO*" ),
    FORWARD( Instruction.FORWARD_CODE, '>' ),
    BACKWARD( Instruction.BACKWARD_CODE, '<' ),
    MOVE_PTR( Instruction.MOVE_PTR_CODE, "*MOVEPTR*" ),
    JUMP_FORWARD( Instruction.JUMP_FORWARD_CODE, '[' ),
    JUMP_BACKWARD( Instruction.JUMP_BACKWARD_CODE, ']' ),
    JUMP_ON_ZERO( Instruction.JUMP_ON_ZERO_CODE, "*JZ*" ),
    JUMP_ON_NONZERO( Instruction.JUMP_ON_NONZERO_CODE, "*JNZ" ),
    IN( Instruction.IN_CODE, ',' ),
    OUT( Instruction.OUT_CODE, '.' ),
    // additional
    DATA_MOVE( Instruction.DATA_MOVE, "*DATAMOVE*" );
    private char code;
    final public int val;
    final public String dumpName;

    private InstructionSet ( int v, String dump ) {
        val = v;
        dumpName = dump;
    }

    private InstructionSet ( int v, char code ) {
        this.code = code;
        val = v;
        dumpName = Character.toString( code );
    }

    public static InstructionSet forCode ( char ch ) {
        if ( ch != 0 ) {
            for ( InstructionSet i : values() )
                if ( i.code == ch )
                    return i;
        }
        return null;
    }

    public static String dump ( Instruction i ) {
        StringBuffer sb = new StringBuffer();
        sb.append( i.instr.dumpName );
        sb.append( ':' );
        sb.append( i.op );
        if ( i.extOps != null && i.extOps2 != null ) {
            for ( int j = 0,  m = i.extOps.length; j < m; ++j ) {
                sb.append( ':' );
                sb.append( i.extOps[j] );
                sb.append( '/' );
                sb.append( i.extOps2[j] );
            }
        }
        return sb.toString();
    }

    public static String dump ( Instruction[] code, boolean dumpAddresses ) {
        StringBuffer sb = new StringBuffer();
        int index = 0;
        for ( Instruction i : code ) {
            if( dumpAddresses ) {
                if ( index < 10000 ) {
                    sb.append( ' ' );
                    if ( index < 1000 ) {
                        sb.append( ' ' );
                        if ( index < 100 ) {
                            sb.append( ' ' );
                            if ( index < 10 )
                                sb.append( ' ' );
                        }
                    }
                }
                sb.append( index++ );
                sb.append( ": " );
            }
            if( i != null )
                sb.append( dump( i ) );
            else 
                sb.append( "IFC" );
            sb.append( '\n' );
        }
        return sb.toString();
    }
    
    public static String dumpPartial ( Instruction[] code, int start, int end ) {
        if( start < 0 )
            start = 0;
        if( end >= code.length )
            end = code.length - 1;
        
        StringBuffer sb = new StringBuffer();
        for ( int index = start; index <= end; ++index ) {
            Instruction i = code[ index ];
            if ( index < 10000 ) {
                sb.append( ' ' );
                if ( index < 1000 ) {
                    sb.append( ' ' );
                    if ( index < 100 ) {
                        sb.append( ' ' );
                        if ( index < 10 )
                            sb.append( ' ' );
                    }
                }
            }
            sb.append( index );
            sb.append( ": " );
            sb.append( dump( i ) );
            sb.append( '\n' );
        }
        return sb.toString();
    }
}
