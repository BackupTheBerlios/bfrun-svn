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

package cy6erGn0m.bf.vm;

import cy6erGn0m.bf.cpu.IOBus;
import java.io.IOException;

/**
 *
 * @author cy6ergn0m
 */
public class NullBus implements IOBus {

    public void out ( int value ) throws IOException {
    }

    public int in () throws IOException {
        return 0x13;
    }

    public void interrupt () {
    }

    public void teardown () {
    }

}