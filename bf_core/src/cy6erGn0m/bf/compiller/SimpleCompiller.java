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

import cy6erGn0m.bf.iset.Instruction;
import cy6erGn0m.bf.iset.InstructionSet;
import java.util.Collection;
import java.util.Vector;

/**
 *
 * @author cy6ergn0m
 */
public class SimpleCompiller implements Compiller {

    public Instruction[] compile( char[] code ) throws CompillingException {
        Vector<Instruction> compiled = new Vector<Instruction>( code.length );
        for( int j = 0; j < code.length; ++j ) {
            InstructionSet is = InstructionSet.forCode( code[ j ] );
            if( is != null )
                compiled.add( new Instruction( is, 0, j ) );
        }
        return toArray(compiled);
    }
    
    public Instruction[] compile(String code) throws CompillingException {
        return compile( code.toCharArray() );
    }
    
    public static Instruction[] toArray( Collection<Instruction> instructions ) {
        Instruction[] result = new Instruction[ instructions.size() ];
        instructions.toArray( result );
        return result;
    }
}
