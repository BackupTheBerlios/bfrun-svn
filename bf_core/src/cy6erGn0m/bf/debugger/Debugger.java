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
package cy6erGn0m.bf.debugger;

import cy6erGn0m.bf.compiller.CompillingException;
import cy6erGn0m.bf.compiller.SimpleCompiller;
import cy6erGn0m.bf.cpu.BfMemory;
import cy6erGn0m.bf.exception.BreakpointException;
import cy6erGn0m.bf.exception.DebugException;
import cy6erGn0m.bf.cpu.DebugProcessor;
import cy6erGn0m.bf.exception.FatalException;
import cy6erGn0m.bf.cpu.IOBus;
import cy6erGn0m.bf.cpu.Memory16;
import cy6erGn0m.bf.cpu.Memory32;
import cy6erGn0m.bf.cpu.Memory8;
import cy6erGn0m.bf.exception.AbortedException;
import cy6erGn0m.bf.exception.EndOfCodeException;
import cy6erGn0m.bf.iset.Instruction;
import cy6erGn0m.bf.vm.SocketBus;
import java.io.IOException;
import java.net.Socket;
import java.util.Vector;

/**
 *
 * @author cy6ergn0m
 */
public class Debugger extends Thread {

    protected char[] sourceCode;
    protected Instruction[] compiled;
    protected BfMemory memory;
    protected DebugProcessor cpu;
    protected IOBus bus;
    protected Vector<DebugClient> clients = new Vector<DebugClient>( 5 );

    public Debugger ( String sourceCode, int bits, IOBus bus ) throws CompillingException,
            IOException {
        this.sourceCode = sourceCode.toCharArray();
        SimpleCompiller c = new SimpleCompiller();
        compiled = c.compile( this.sourceCode );
        if ( bits == 16 )
            memory = new Memory16();
        else if ( bits == 32 )
            memory = new Memory32();
        else
            memory = new Memory8();
        this.bus = bus;
        cpu = new DebugProcessor( compiled, bus, memory );
        start();
    }

    public Debugger ( String sourceCode, int bits, Socket socket ) throws CompillingException,
            IOException {
        this( sourceCode, bits, new SocketBus( socket ) );
    }

    public boolean setBreakpoint ( int index, boolean state ) {
        boolean oldstate = cpu.isBreakpointAt( index );
        if ( state )
            cpu.setBreakpointAtSource( index );
        else
            cpu.clearBreakpointAt( index );
        return oldstate;
    }
    
    public boolean getBreakpoint( int index ) {
        return cpu.isBreakpointAt( index );
    }
    
    protected boolean running = false;   // запущено
    protected boolean performing = false;  // выполняется в данный момент
    protected boolean exitRequested = false;
    protected Integer notRunning = new Integer( 0 );
    protected Integer whileRunning = new Integer( 0 );

    private void notifyDie( DebugException e ) {
        for( DebugClient client : clients ) {
            try {
                client.notifyDie( null );
            } catch( Throwable t ) {
                t.printStackTrace();
            }
        }
    }
    
    private void notifyBreak( BreakpointException e ) {
        for( DebugClient client : clients ) {
            try {
                client.notifyBreakpoint( e );
            } catch( Throwable t ) {
                t.printStackTrace();
            }
        }
    }
    
    private void notifyException( final DebugException e ) {
        (new Thread() {
            @Override
            public void run() {
                for( DebugClient client : clients ) {
                    try {
                        client.notifyException( e );
                    } catch( Throwable t ) {
                        t.printStackTrace();
                    }
                }
            }
        }).start();
    }
    
    public void step () {
        BreakpointException bp = null;
        synchronized ( this ) {
            if ( !running )
                throw new IllegalStateException( "application is not runnning" );
            if ( performing )
                throw new IllegalStateException( "application currently running" );
            try {
                performing = true;
                cpu.step();
            } catch ( BreakpointException e ) {
                bp = e;
            } catch ( DebugException e ) {
                notifyException( e );
                if ( e.isFatal() )
                    terminate( e );
            }
            performing = false;
        }

        synchronized ( whileRunning ) {
            whileRunning.notifyAll();
        }
        
        if( bp != null )
            notifyBreak( bp );
    }
    
    public void cont () {
        synchronized ( this ) {
            if ( !running )
                throw new IllegalStateException( "application is not running" );
            if ( performing )
                throw new IllegalStateException( "application currently performing" );
        }
        if ( !performing ) {
            performing = true;
            synchronized ( notRunning ) {
                notRunning.notifyAll();
            }
        }
    }

    public void pause () {
        boolean f;
        synchronized ( this ) {
            if ( ( f = running && performing ) )
                cpu.interrupt();
        }
        if ( f ) {
            waitWhileRunning();
        }
    }

    protected void waitWhileRunning () {
        while ( performing ) {
            synchronized ( whileRunning ) {
                try {
                    whileRunning.wait( 150 );
                } catch ( InterruptedException e ) {
                }
            }
        }
    }

    public void runTo ( int sourceIndex ) {
        boolean oldstate = setBreakpoint( sourceIndex, true );
        cont();
        waitWhileRunning();
        setBreakpoint( sourceIndex, oldstate );
    }

    public void terminate ( DebugException problem ) {
        cpu.interrupt();
        waitWhileRunning();
        notifyDie( problem );
    }

    public void begin () {
        if ( !exitRequested ) {
            synchronized ( this ) {
                if ( running )
                    terminate( new AbortedException( 0, cpu.getCurrentInstruction() ) );
            }
            running = true;
            synchronized ( notRunning ) {
                notRunning.notifyAll();
            }
        }
    }

    public boolean isRunning () {
        return running;
    }

    public boolean isPerofrming () {
        return performing;
    }

    @Override
    public void run () {
        main_loop:
        while ( !exitRequested ) {
            while ( !running || !performing ) {
                synchronized ( notRunning ) {
                    try {
                        notRunning.wait();
                    } catch ( InterruptedException e ) {
                        continue main_loop;
                    }
                }
            }
            try {
                cpu.perform();
                running = false;
                notifyDie( new EndOfCodeException( 0, cpu.getCurrentInstruction() ) );
            } catch ( BreakpointException bp ) {
                notifyBreak( bp );
            } catch ( FatalException e ) {
                notifyException( e );
                running = false;
                notifyDie( e );
            } catch ( DebugException e ) {
                notifyException( e );
                if ( e.isFatal() )
                    terminate( e );
            }
            performing = false;
            synchronized ( whileRunning ) {
                whileRunning.notifyAll();
            }
        }
    }

    public String getSourceCode () {
        return String.valueOf( sourceCode );
    }
    
    public void registerDebugClient( DebugClient client ) {
        if( ( client != null ) && ( !clients.contains( client ) ) )
            clients.add( client );
    }
}
