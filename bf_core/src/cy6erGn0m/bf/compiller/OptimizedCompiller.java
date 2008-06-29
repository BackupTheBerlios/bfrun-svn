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
package cy6erGn0m.bf.compiller;

import cy6erGn0m.bf.cpu.IntVector;
import cy6erGn0m.bf.iset.Instruction;
import cy6erGn0m.bf.iset.InstructionSet;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.Stack;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author cy6ergn0m
 */
public class OptimizedCompiller implements Compiller {

    private int optimizationLevel = 2;

    public void setOptimizationLevel ( int optimizationLevel ) {
        this.optimizationLevel = optimizationLevel;
    }

    protected final int indexOf ( List<Instruction> array, Instruction[] toBeFound, int fromIndex ) {
        if ( toBeFound == null || toBeFound.length == 0 )
            return -1;
        int start_i = toBeFound[0].ival;
        int l = toBeFound.length;
        big_find:
        for ( int i = fromIndex, m = array.size(); i < m; ++i ) {
            if ( array.get( i ).ival == start_i ) {
                for ( int j = i + 1, k = 1; j < m && k < l; ++j, ++k )
                    if ( array.get( j ).ival != toBeFound[k].ival )
                        continue big_find;
                return i;
            }
        }
        return -1;
    }

    private ArrayList<Instruction> pass1 ( ArrayList<Instruction> input ) {
        ArrayList<Instruction> result = new ArrayList<Instruction>( input.size() );
        if ( input.size() > 0 ) {
            int value = 0;
            int source = input.get( 0 ).sourceIndex;
            for ( Instruction i : input ) {
                switch (i.ival) {
                    case Instruction.INC_CODE:
                        value++;
                        break;
                    case Instruction.DEC_CODE:
                        value--;
                        break;
                    default:
                        if ( value == 1 )
                            result.add( new Instruction( InstructionSet.INC, 1, source ) );
                        else if ( value == -1 )
                            result.add( new Instruction( InstructionSet.DEC, -1, source ) );
                        else if ( value != 0 )
                            result.add( new Instruction( InstructionSet.MODIFY, value, source ) );
                        value = 0;
                        result.add( i );
                        source = i.sourceIndex;
                }
            }
        }
        return result;
    }

    private ArrayList<Instruction> pass2 ( ArrayList<Instruction> input ) {
        ArrayList<Instruction> result = new ArrayList<Instruction>( input.size() );
        if ( input.size() > 0 ) {
            int value = 0;
            int source = input.get( 0 ).sourceIndex;
            for ( Instruction i : input ) {
                switch (i.ival) {
                    case Instruction.FORWARD_CODE:
                        value++;
                        break;
                    case Instruction.BACKWARD_CODE:
                        value--;
                        break;
                    default:
                        if ( value == 1 )
                            result.add( new Instruction( InstructionSet.FORWARD, 1, source ) );
                        else if ( value == -1 )
                            result.add( new Instruction( InstructionSet.BACKWARD, -1, source ) );
                        else if ( value != 0 )
                            result.add( new Instruction( InstructionSet.MOVE_PTR, value, source ) );
                        value = 0;
                        result.add( i );
                        source = i.sourceIndex;
                }
            }
        }
        return result;
    }

    @SuppressWarnings("fallthrough")
    private ArrayList<Instruction> pass3 ( ArrayList<Instruction> input ) {
        ArrayList<Instruction> result = new ArrayList<Instruction>( input.size() );
        if ( input.size() > 0 ) {
            ArrayList<Instruction> buffer = new ArrayList<Instruction>( 4 );
            int source = input.get( 0 ).sourceIndex;
            boolean buffering = false;
            zero_find:
            for ( Instruction i : input ) {
                switch (i.ival) {
                    case Instruction.JUMP_FORWARD_CODE:
                        if ( buffering )
                            flush( result, buffer );
                        buffering = true;
                        break;
                    case Instruction.JUMP_BACKWARD_CODE:
                        if ( buffer.size() == 2 && buffering ) {
                            if ( buffer.get( 0 ).ival == Instruction.JUMP_FORWARD_CODE &&
                                    (buffer.get( 1 ).ival == Instruction.INC_CODE ||
                                    buffer.get( 1 ).ival == Instruction.DEC_CODE) ) {

                                result.add( new Instruction( InstructionSet.ZERO, 0, source ) );
                                buffer.clear();
                                buffering = false;
                                continue zero_find;
                            }
                        }
                    default:
                        if ( buffering && buffer.size() > 3 ) {
                            flush( result, buffer );
                            buffering = false;
                        }
                }

                (buffering ? buffer : result).add( i );
            }
        }
        return result;
    }

    /**
     * 
     * @param chars
     * @param optimizationLevel 0 - do not optimize, 1 - fast optimize, 2 - full, 3 - experimental optimizations
     * @return
     * @throws cy6erGn0m.bf.compiller.CompillingException 
     */
    public synchronized Instruction[] compile ( char[] chars,
            int optimizationLevel ) throws CompillingException {
        if ( optimizationLevel == 0 )
            return (new SimpleCompiller()).compile( chars );

        ArrayList<Instruction> compiled = new ArrayList<Instruction>( chars.length );
        for ( int j = 0; j < chars.length; ++j ) {
            InstructionSet is = InstructionSet.forCode( chars[j] );
            if ( is != null )
                compiled.add( new Instruction( is, 0, j ) );
        }

        ArrayList<Instruction> optimized = compiled;

        if ( optimizationLevel > 0 ) {
            optimized = pass1( optimized );

            optimized = pass2( optimized );

            optimized = pass3( optimized );

            if ( optimizationLevel > 1 ) {
                optimized = data_move_optimization( optimized );
                if ( optimizationLevel > 2 )
                    optimized = noSideEffectOptimization( optimized );
            }
            optimized = calcJumps( optimized );
        }

        return SimpleCompiller.toArray( optimized );
    }

    public synchronized Instruction[] compile ( String code ) throws CompillingException {
        return compile( code.toCharArray() );
    }

    /**
     * compiles bf text to instructions. Do not compile many codes in same time!
     * @param chars
     * @return 
     * @throws cy6erGn0m.bf.compiller.CompillingException
     */
    public synchronized Instruction[] compile ( char[] chars ) throws CompillingException {
        return compile( chars, optimizationLevel );
    }

    @SuppressWarnings("fallthrough")
    private static ArrayList<Instruction> data_move_optimization ( ArrayList<Instruction> code ) {
        ArrayList<Instruction> result = new ArrayList<Instruction>( code.size() );
        ArrayList<Instruction> buffer = new ArrayList<Instruction>( 10 );
        boolean isInLoop = false;

        // find stupid move
        for ( Instruction i : code ) {
            if ( isInLoop ) {
                if ( i.ival == Instruction.JUMP_ON_ZERO_CODE || i.ival == Instruction.JUMP_FORWARD_CODE ) {
                    // [
                    result.addAll( buffer );
                    buffer.clear();
                } else if ( i.ival == Instruction.JUMP_ON_NONZERO_CODE || i.ival == Instruction.JUMP_BACKWARD_CODE ) {
                    // ]
                    // here we get simplest cycle
                    // expected parse
                    if ( buffer.size() >= 2 && (buffer.get( 0 ).ival == Instruction.JUMP_ON_ZERO_CODE || buffer.get( 0 ).ival == Instruction.JUMP_FORWARD_CODE) ) {
                        // [- or [
                        int currentIndex = 0;
                        IntVector indexes = new IntVector( 4 );
                        IntVector values = new IntVector( 4 );
                        int value = 0;
                        final int m = buffer.size();
                        boolean decAtFirst = buffer.get( 1 ).ival == Instruction.DEC_CODE; // [-
                        boolean decFound = decAtFirst;
                        pairs:
                        for ( int j = decAtFirst ? 2 : 1; j < m; ++j ) {
                            final int iv = buffer.get( j ).ival;
                            switch (iv) {
                                case Instruction.MOVE_PTR_CODE:
                                case Instruction.FORWARD_CODE:
                                case Instruction.BACKWARD_CODE:
                                    if ( value != 0 ) {
                                        indexes.add( currentIndex );
                                        values.add( value );
                                        value = 0;
                                    }
                                    if ( iv == Instruction.MOVE_PTR_CODE )
                                        currentIndex += buffer.get( j ).op;
                                    else if ( iv == Instruction.FORWARD_CODE )
                                        currentIndex++;
                                    else if ( iv == Instruction.BACKWARD_CODE )
                                        currentIndex--;
                                    break;
                                case Instruction.INC_CODE:
                                    value++;
                                    break;
                                case Instruction.MODIFY_CODE:
                                    value += buffer.get( j ).op;
                                    break;
                                case Instruction.DEC_CODE:
                                    if ( currentIndex != 0 )
                                        value--;
                                    else {
                                        if ( decAtFirst ) {
                                            indexes.clear();
                                            values.clear();
                                            value = 0;
                                            currentIndex = 0;
                                            break pairs;
                                        } else
                                            decFound = true;
                                    }
                                    break;
                                default:
                                    value = 0;
                                    indexes.clear();
                                    break pairs;
                            }
                        }
                        if ( currentIndex != 0 || value != 0 || !decFound ) {
                            indexes.clear();
                            values.clear();
                            currentIndex = 0;
                        }

                        if ( indexes.size() > 0 && currentIndex == 0 ) {
                            Instruction i2;
                            if ( indexes.size() == 1 && values.at( 0 ) == 1 )
                                i2 = new Instruction( InstructionSet.DATA_MOVE, indexes.at( 0 ), buffer.get( 0 ).sourceIndex );
                            else {
                                i2 = new Instruction( InstructionSet.DATA_MOVE, 0, buffer.get( 0 ).sourceIndex );
                                i2.extOps = indexes.toArray();
                                i2.extOps2 = values.toArray();
                            }
                            // debug
//                            System.out.println( " optimize: change" );
//                            buffer.add( i );
//                            Instruction tmp[] = new Instruction[ buffer.size() ];
//                            buffer.toArray( tmp );
//                            System.out.println( InstructionSet.dump( tmp, false ) );
//                            System.out.println( "to new code" );
//                            System.out.println( InstructionSet.dump( i2 ) );
//                            System.out.println("===============================");
                            indexes.clear();
                            values.clear();
                            buffer.clear();
                            result.add( i2 );
                            isInLoop = false;
                            continue;
                        }
                    }
                    isInLoop = false;
                    flush( result, buffer );
                    result.add( i );
                    continue;
                }
                buffer.add( i );
            } else {
                if ( i.ival == Instruction.JUMP_ON_ZERO_CODE || i.ival == Instruction.JUMP_FORWARD_CODE ) {
                    flush( result, buffer );
                    buffer.add( i );
                    isInLoop = true;
                } else
                    result.add( i );
            }
        }
        result.addAll( buffer );
        buffer.clear();
        return result;
    }

    private static ArrayList<Instruction> noSideEffectOptimization ( ArrayList<Instruction> code ) {
        int optimizations;
        ArrayList<Instruction> src;
        ArrayList<Instruction> result = code;
        Instruction instr;
        int deep = 0;
        do {
            optimizations = 0;
            src = result;
            // TODO: if( src.size() == 1...
            if ( src.size() > 0 ) {
                int m = src.size() - 1;
                result = new ArrayList<Instruction>( m + 1 );
                main:
                for ( int i = 0; i < m; ++i ) {
                    switch ((instr = src.get( i )).ival) {
                        case Instruction.ZERO_CODE:
                        case Instruction.DEC_CODE:
                        case Instruction.INC_CODE:
                        case Instruction.IN_CODE:
                        case Instruction.MODIFY_CODE:
                            if ( src.get( i + 1 ).ival == Instruction.ZERO_CODE ) {
                                optimizations++;
                                continue main;
                            }
                            break;
                        case Instruction.JUMP_FORWARD_CODE:
                            if ( src.get( i + 1 ).ival == Instruction.JUMP_BACKWARD_CODE ) {
                                ++i;
                                optimizations++;
                                continue main;
                            }
                            break;
                        case Instruction.JUMP_BACKWARD_CODE:
                            if ( src.get( i + 1 ).ival == Instruction.ZERO_CODE ) {
                                ++i;
                                optimizations++;
                            // note: do not continue here
                            }
                        case Instruction.DATA_MOVE:
                            if ( src.get( i + 1 ).ival == Instruction.ZERO_CODE ) {
                                ++i;
                                optimizations++;
                            // note: do not continue here
                            }
                            break;
                        default:
                            break;
                    }
                    result.add( instr );
                }
                instr = src.get( m );
                if ( instr.ival == Instruction.OUT_CODE || instr.ival == Instruction.JUMP_BACKWARD_CODE )
                    result.add( src.get( m ) );
                if ( ++deep == 1000 )
                    throw new IllegalStateException();
            }
        } while ( optimizations > 0 );
        return src;
    }

    private static void flush ( Collection<Instruction> big, Collection<Instruction> toBeFlushed ) {
        big.addAll( toBeFlushed );
        toBeFlushed.clear();
    }

    private ArrayList<Instruction> calcJumps ( ArrayList<Instruction> compiled )
            throws CompillingException {
        try {
            final int m = compiled.size();
            ArrayList<Instruction> result = new ArrayList<Instruction>( m + 1 );
            Stack<Integer> stack = new Stack<Integer>();
            for ( int i = 0; i < m; i++ ) {
                Instruction ins = compiled.get( i );
                int approx_src = ins.sourceIndex;
                final Instruction i2;
                switch (ins.instr) {
                    case JUMP_FORWARD:
                        //i2 = new Instruction( InstructionSet.JUMP_ON_ZERO, findForward( compiled, i ), approx_src );
                        i2 = new Instruction( InstructionSet.NONE, 0, approx_src );
                        stack.push( i );
                        break;
                    case JUMP_BACKWARD:
                        int left_b = stack.pop();
                        Instruction old = result.get( left_b );
                        result.set( left_b, new Instruction( InstructionSet.JUMP_ON_ZERO, i, old.sourceIndex ) );
                        i2 = new Instruction( InstructionSet.JUMP_ON_NONZERO, left_b, approx_src );
                        break;
                    default:
                        i2 = ins;
                }
                result.add( i2 );
            }
            if ( stack.size() > 0 )
                throw new CompillingException( "brackets disballance" );
            return result;
        } catch (EmptyStackException e) {
            throw new CompillingException( "brackets disballance" );
        }
    }
}
