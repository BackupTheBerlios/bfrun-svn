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

package cy6erGn0m.bf.exception;

import cy6erGn0m.bf.iset.Instruction;
import cy6erGn0m.bf.iset.InstructionSet;

/**
 *
 * @author cy6ergn0m
 */
public abstract class DebugException extends Exception {
    protected int address;
    protected Instruction instruction;

    public DebugException(int address, Instruction instruction) {
        this.address = address;
        if( ( this.instruction = instruction ) == null )
            this.instruction = new Instruction( InstructionSet.NONE, 0, 0 );
    }

    public void setAddress(int address) {
        this.address = address;
    }
    
    public abstract boolean isFatal();

    @Override
    public String getMessage() {
        StringBuffer sb = new StringBuffer();
        sb.append( "at address " );
        sb.append( address );
        sb.append( " and at source " );
        sb.append( instruction.sourceIndex );
        return sb.toString();
    }

    public Instruction getInstruction() {
        return instruction;
    }
}
