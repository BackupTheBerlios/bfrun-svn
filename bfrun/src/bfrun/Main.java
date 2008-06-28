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
package bfrun;

import cy6erGn0m.bf.compiller.AltcpuCompiller;
import cy6erGn0m.bf.compiller.Compiller;
import cy6erGn0m.bf.compiller.CompillingException;
import cy6erGn0m.bf.compiller.OptimizedCompiller;
import cy6erGn0m.bf.compiller.SimpleCompiller;
import cy6erGn0m.bf.cpu.AltProcessor;
import cy6erGn0m.bf.cpu.BfCpu;
import cy6erGn0m.bf.cpu.BfMemory;
import cy6erGn0m.bf.exception.DebugException;
import cy6erGn0m.bf.cpu.IOBus;
import cy6erGn0m.bf.cpu.Memory16;
import cy6erGn0m.bf.cpu.Memory32;
import cy6erGn0m.bf.cpu.Memory8;
import cy6erGn0m.bf.cpu.Memory8_2nd;
import cy6erGn0m.bf.cpu.Processor;
import cy6erGn0m.bf.iset.Instruction;
import cy6erGn0m.bf.iset.InstructionSet;
import cy6erGn0m.bf.vm.StreamedBus;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 *
 * @author cy6ergn0m
 */
public class Main {

    public static final String bf_version = "1.1.3m3";

    protected static void help () {
        System.out.println( "Brainf*ck interpreter. v." + bf_version + "\n" +
                "This is Free software: no any waranties.\n" +
                "\nusage:\n" +
                "java -jar bfrun.jar [-no-optimize] [-16|-32] [-O0|-O1|-O2|-O3] [-dump] [-time] [--] file\n" );
        System.out.println( "-no-optimize\t\trun bf Pcode without optimizations" );
        System.out.println( "-8\t\t\t(default) use 8bit bf memory" );
        System.out.println( "-16\t\t\tuse 16bit bf memory" );
        System.out.println( "-32\t\t\tuse 32bit bf memory" );
        System.out.println( "-O0\t\t\tdisable optimizations, see -no-optimize" );
        System.out.println( "-O1\t\t\tdisable most optimizations" );
        System.out.println( "-O2\t\t\t(default) set normal optimization level" );
        System.out.println( "-O3\t\t\tenable experimental optimizations" );
        System.out.println( "-dump\t\t\tprint Pcode compiled dump" );
        System.out.println( "-time\t\t\tprint performing time" );
        System.out.println( "-stat\t\t\tprint compiled code statistics" );
        System.out.println();
    }

    protected static void printUsage () {
        System.out.println( "Brainf*ck interpreter. v." + bf_version + "\n" +
                "This is Free software: no any waranties.\n" +
                "\nusage:\n" +
                "java -jar bfrun.jar [-no-optimize] [-8|-16|-32] [-O0|-O1|-O2|-O3] [-dump] [-time] [-stat] [--] file\n" +
                "or java -jar bfrun.jar -help\n" );
    }
    String filename = null;
    boolean noOptimize = false;
    int bits = 8;
    boolean dp = false;
    boolean altcpu = false;
    boolean checkTime = false;
    int optimizationLevel = 2;
    boolean dumpCode = false;
    boolean debugPass = false;
    boolean printHelp = false;
    boolean bo = false;
    boolean notRun = false;
    boolean printStat = false;
    boolean comparer = false;
    boolean noAddr = false;
    boolean doNothing = false;
    BfMemory memory;
    IOBus bus;
    Instruction[] code;

    public Main () {
    }

    protected boolean parseParameters ( String[] args ) {
        for ( String param : args ) {
            if ( !dp && param.startsWith( "-" ) ) {
                if ( param.equals( "-no-optimize" ) )
                    noOptimize = true;
                else if ( param.equals( "-alt-cpu" ) ) {
//                    System.err.println( "-alt-cpu option deleted." );
//                    return false;
                    altcpu = true;
                } else if ( param.equals( "-8" ) )
                    bits = 8;
                else if ( param.equals( "-16" ) )
                    bits = 16;
                else if ( param.equals( "-32" ) )
                    bits = 32;
                else if ( param.equals( "-time" ) )
                    checkTime = true;
                else if ( param.equals( "--" ) )
                    dp = true;
                else if ( param.equals( "-O0" ) )
                    optimizationLevel = 0;
                else if ( param.equals( "-O1" ) )
                    optimizationLevel = 1;
                else if ( param.equals( "-O2" ) )
                    optimizationLevel = 2;
                else if ( param.equals( "-O3" ) )
                    optimizationLevel = 3;
                else if ( param.equals( "-dump" ) )
                    dumpCode = true;
                else if ( param.equals( "-no-addr" ) )
                    noAddr = true;
                else if ( param.equals( "-do-nothing" ) )
                    doNothing = true;
                else if ( param.equals( "-debugOptimization" ) )
                    comparer = true;
                else if ( param.equals( "-bo" ) )
                    bo = true;
                else if ( param.equals( "-debugPass" ) ) {
                    System.err.println( "-debugPass is not supported" );
                    return false;
                } else if ( param.equals( "-stat" ) )
                    printStat = true;
                else if ( param.equals( "-help" ) )
                    printHelp = true;
                else {
                    System.err.println( "invalid option " + param );
                    return false;
                }
            } else {
                filename = param;
                break;
            }
        }
        return true;
    }

    protected void correctOptions () {
        if ( noOptimize )
            optimizationLevel = 0;
        else
            noOptimize = ( optimizationLevel == 0 );

        if ( dumpCode || printStat || doNothing )
            notRun = true;

        if( altcpu ) {
            noOptimize = false;
            if( optimizationLevel == 0 )
                optimizationLevel = 1;
        }
    }

    protected Instruction[] compile ( String programText ) throws CompillingException {
        Compiller compiller;
        if ( noOptimize ) {
            compiller = new SimpleCompiller();
        } else {
            compiller = new OptimizedCompiller();
            ( (OptimizedCompiller) compiller ).setOptimizationLevel( optimizationLevel );
        }
        return compiller.compile( programText );
    }

    protected void setupMemory () {
        if ( bits == 16 )
            memory = new Memory16();
        else if ( bits == 32 )
            memory = new Memory32();
        else
            memory = new Memory8_2nd();
    }

    protected void setupBus () {
        if ( bo )
            bus = new StreamedBus( System.in, new BufferedOutputStream( System.out, 8 ) );
        else
            bus = new StreamedBus( System.in, System.out );
    }

    protected BfCpu produceCPU () {

        BfCpu cpu;
        if( altcpu ) {
            cpu = new AltProcessor( memory, bus, new AltcpuCompiller().compile( code ) );
        } else {
            cpu = new Processor( code, bus, memory );
            if ( !altcpu )
                ( (Processor) cpu ).setExpectOptimizedCode( !noOptimize );
        }
        return cpu;
    }

    protected long perform () throws DebugException {
        setupMemory();
        setupBus();
        BfCpu cpu = produceCPU();
        long begin = System.currentTimeMillis();
        cpu.perform();
        return System.currentTimeMillis() - begin;
    }

    protected void printStat () {
        if ( code != null ) {
            TreeMap<String, Integer> stat = new TreeMap<String, Integer>();
            for ( Instruction i : code ) {
                final String ins = i.instr.toString();
                final Integer v = stat.get( ins );
                stat.put( ins, ( v != null ) ? v + 1 : 1 );
            }

            for ( Entry<String, Integer> e : stat.entrySet() ) {
                System.out.print( e.getKey() );
                System.out.print( ":" );
                System.out.println( e.getValue() );
            }
            stat.clear();
            System.out.println( "Total: " + code.length );
        } else {
            System.out.println( "There are no code to print stat" );
        }
    }

    protected void printTimes ( long ctime, long rtime ) {
        StringBuffer sb = new StringBuffer();
        sb.append( "times: \n" );
        if ( ctime > 0 ) {
            sb.append( "compilling time: " );
            sb.append( ctime );
            sb.append( " ms\n" );
        }
        if ( rtime > 0 ) {
            sb.append( "performing time: " );
            sb.append( rtime );
            sb.append( " ms\n" );
        }
        if ( ctime + rtime > 0 ) {
            sb.append( "total: " );
            sb.append( ctime + rtime );
            sb.append( " ms\n" );
        }
        System.out.println( sb.toString() );
    }

    private String warnUpCode = "++[-][]-><+-[-].";

    private void warmUp() throws CompillingException, DebugException {
        if( ( code = compile( warnUpCode ) ) != null )
            perform();
        code = null;
    }
    
    public void runIt ( String[] args ) throws DebugException,
                                               CompillingException {
        if ( !parseParameters( args ) )
            System.exit( -1 );
        if ( printHelp )
            help();
        else if ( filename == null )
            printUsage();
        else {
            correctOptions();

            warmUp();
            warmUp();

            String programText = fromFile( filename );
            if ( programText == null )
                System.exit( -1 );
            long begin = System.currentTimeMillis();
            code = compile( programText );
            long ctime = System.currentTimeMillis() - begin;
            long rtime = 0;
            
            if ( !notRun )
                rtime = perform();
            
            if ( dumpCode )
                System.out.println( InstructionSet.dump( code, !noAddr ) );
            
            if ( checkTime )
                printTimes( ctime, rtime );
            
            if ( printStat )
                printStat();
            
            bus.teardown();
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main ( String[] args ) {
        try {
            ( new Main() ).runIt( args );
        } catch ( DebugException ex ) {
            System.err.println( "runtime error: " + ex.getMessage() );
        } catch ( CompillingException ex ) {
            System.err.println( " compillation failed: " + ex.getMessage() );
        }
    }

    public static String fromFile ( String path ) {
        if ( path == null )
            return null;
        FileInputStream fis = null;
        String s = null;
        try {
            File f = new File( path );
            if ( !f.exists() )
                return null;
            int sz = (int) f.length();
            byte[] bytes = new byte[ sz ];
            fis = new FileInputStream( f );
            fis.read( bytes );
            s = new String( bytes, Charset.defaultCharset().name() );
        } catch ( IOException ex ) {
            System.err.println( ex.getMessage() );
        } finally {
            try {
                if ( fis != null )
                    fis.close();
            } catch ( IOException ex ) {
                System.err.println( ex.getMessage() );
            }
        }
        return s;
    }
}
