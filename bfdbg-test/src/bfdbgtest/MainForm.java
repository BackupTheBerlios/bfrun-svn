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
package bfdbgtest;

import cy6erGn0m.bf.compiller.CompillingException;
import cy6erGn0m.bf.cpu.BfMemory;
import cy6erGn0m.bf.exception.BreakpointException;
import cy6erGn0m.bf.exception.DebugException;
import cy6erGn0m.bf.debugger.DebugClient;
import cy6erGn0m.bf.debugger.Debugger;
import cy6erGn0m.bf.exception.AbortedException;
import cy6erGn0m.bf.vm.NullBus;
import java.awt.Color;
import java.awt.EventQueue;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.basic.BasicTextUI.BasicCaret;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 *
 * @author  cy6ergn0m
 */
public class MainForm extends javax.swing.JFrame implements DebugClient {

    StyledDocument doc;
    Style runPointStyle;
    Style bpStyle;
    Style bpCurrStyle;
    Style normalStyle;
    StyledDocument consoleDoc;
    Style redText;
    final Object lock = new Object();
    protected ArrayList<Integer> breakpoints = new ArrayList<Integer>( 10 );

    /** Creates new form MainForm */
    public MainForm () {
        initComponents();
        setButtonsState();

        doc = sourceText.getStyledDocument();
        runPointStyle = doc.addStyle( "run point", null );
        runPointStyle.addAttribute( StyleConstants.Background, Color.GREEN );

        bpStyle = doc.addStyle( "break point", null );
        bpStyle.addAttribute( StyleConstants.Background, Color.RED );

        bpCurrStyle = doc.addStyle( "breakpoint under cursor", null );
        bpCurrStyle.addAttribute( StyleConstants.Background, Color.ORANGE );

        normalStyle = doc.addStyle( "normal", null );

        consoleDoc = debugOutputWindow.getStyledDocument();
        redText = consoleDoc.addStyle( "fatal message", null );
        redText.addAttribute( StyleConstants.Foreground, Color.RED );

        String home = System.getProperty( "user.home" );
        jTextField1.setText( home != null ? home : "" );
    }
    protected Debugger dbg_vm = null;
    protected File sourceFile = null;
    DebugState currentState = DebugState.NOT_LOADED;

    protected boolean setSourceFile ( String path ) {
        File f = new File( path );
        if ( (sourceFile == null) || (!f.equals( sourceFile )) ) {
            if ( dbg_vm != null ) {
                dbg_vm.terminate( new AbortedException( 0, null ) );
                dbg_vm = null;
            }
            sourceFile = f;
        }
        breakpoints.clear();
        boolean rs = sourceFile.exists() && sourceFile.isFile();
        currentState = (rs) ? DebugState.NOT_RUNNING : DebugState.NOT_LOADED;
        if ( !rs )
            sourceText.setText( "" );
        else {
            sourceText.setText( readFromFile( sourceFile ) );
            doc.setParagraphAttributes( 0, doc.getEndPosition().getOffset(), normalStyle, false );
            sourceText.setCaretPosition( 0 );
        }
        return rs;
    }

    private String readFromFile ( File f ) {
        if ( f == null || !f.exists() )
            return null;
        if( f.length() > 0x50000L )
            return null;
        int len = (int) f.length();
        byte[] bytes = new byte[ len ];
        try {
            FileInputStream in = new FileInputStream( f );
            in.read( bytes );
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return new String( bytes, Charset.defaultCharset() );
    }

    protected String tryRun () {
        try {
            currentState = DebugState.NOT_RUNNING;
            if ( sourceFile == null )
                return java.util.ResourceBundle.getBundle( "bfdbgtest/Bundle" ).getString( "source_file_not_set" );
            if ( !sourceFile.exists() )
                return java.util.ResourceBundle.getBundle( "bfdbgtest/Bundle" ).getString( "source_file_not_found" );
            dbg_vm = new Debugger( readFromFile( sourceFile ), 8, new NullBus() );
            dbg_vm.registerDebugClient( this );
            dbg_vm.begin();
            currentState = DebugState.PAUSED;
            for ( Integer i : breakpoints )
                dbg_vm.setBreakpoint( i, true );
            return java.util.ResourceBundle.getBundle( "bfdbgtest/Bundle" ).getString( "OK" );
        } catch (CompillingException ex) {
//            Logger.getLogger( MainForm.class.getName() ).log( Level.SEVERE, null, ex );
            return java.util.ResourceBundle.getBundle( "bfdbgtest/Bundle" ).getString( "compillation_failed" );
        } catch (IOException ex) {
//            Logger.getLogger( MainForm.class.getName() ).log( Level.SEVERE, null, ex );;
            return java.util.ResourceBundle.getBundle( "bfdbgtest/Bundle" ).getString( "I/O_exception" );
        }
    }

    private void correctState () {
        if ( dbg_vm != null ) {
            if ( dbg_vm.isRunning() ) {
                if ( dbg_vm.isPerofrming() )
                    currentState = DebugState.RUNNING_AND_PERFORMING;
                else
                    currentState = DebugState.PAUSED;
            } else
                currentState = DebugState.NOT_RUNNING;
        } else
            currentState = DebugState.NOT_LOADED;
    }

    protected void cont () {
        if ( currentState != DebugState.PAUSED )
            throw new IllegalStateException();
        try {
            dbg_vm.cont();
            currentState = DebugState.RUNNING_AND_PERFORMING;
        } catch (IllegalStateException e) {
            correctState();
        }
        markBreakpoint( null, null, false );
    }

    protected void pause () {
        if ( currentState != DebugState.RUNNING_AND_PERFORMING )
            throw new IllegalStateException();
        dbg_vm.pause();
        currentState = DebugState.PAUSED;
    // TODO: how to mark current?
    }

    protected void kill () {
        if ( dbg_vm != null ) {
            dbg_vm.terminate( new AbortedException( 0, null ) );
            dbg_vm = null;
        }
        currentState = DebugState.NOT_RUNNING;
        markBreakpoint( null, null, false );
    }

    protected void toggle ( int pos ) {
        if ( currentState != DebugState.PAUSED && currentState != DebugState.RUNNING_AND_PERFORMING )
            throw new IllegalStateException();
        boolean v = !dbg_vm.getBreakpoint( pos );
        dbg_vm.setBreakpoint( pos, v );
        markBreakpoint( pos, v, false );
        if ( v )
            breakpoints.add( pos );
        else
            breakpoints.remove( breakpoints.indexOf( pos ) );
        markBreakpoint( pos, v, false );
    }

    private void setButtonsState () {
        EventQueue.invokeLater( new Runnable() {

            public void run () {
                synchronized ( lock ) {
                switch (currentState) {
                    case NOT_LOADED:
                        jTextField1.setEnabled( true );
                        jButton1.setEnabled( true );
                        buttonRun.setEnabled( false );
                        buttonContinue.setEnabled( false );
                        buttonPause.setEnabled( false );
                        buttonToggle.setEnabled( false );
                        buttonKill.setEnabled( false );
                        buttonStep.setEnabled( false );
                        setStatusText( java.util.ResourceBundle.getBundle( "bfdbgtest/Bundle" ).getString( "Ready" ) );
                        break;
                    case NOT_RUNNING:
                        jTextField1.setEnabled( true );
                        jButton1.setEnabled( true );
                        buttonRun.setEnabled( true );
                        buttonContinue.setEnabled( false );
                        buttonPause.setEnabled( false );
                        buttonToggle.setEnabled( false );
                        buttonKill.setEnabled( false );
                        buttonStep.setEnabled( false );
                        setStatusText( java.util.ResourceBundle.getBundle( "bfdbgtest/Bundle" ).getString( "Ready" ) );
                        break;
                    case PAUSED:
                        jTextField1.setEnabled( false );
                        jButton1.setEnabled( false );
                        buttonRun.setEnabled( false );
                        buttonContinue.setEnabled( true );
                        buttonPause.setEnabled( false );
                        buttonToggle.setEnabled( true );
                        buttonKill.setEnabled( true );
                        buttonStep.setEnabled( true );
                        setStatusText( java.util.ResourceBundle.getBundle( "bfdbgtest/Bundle" ).getString( "At_breakpoint" ) );
                        break;
                    case RUNNING_AND_PERFORMING:
                        jTextField1.setEnabled( false );
                        jButton1.setEnabled( false );
                        buttonRun.setEnabled( false );
                        buttonContinue.setEnabled( false );
                        buttonPause.setEnabled( true );
                        buttonToggle.setEnabled( true );
                        buttonKill.setEnabled( true );
                        buttonStep.setEnabled( false );
                        setStatusText( java.util.ResourceBundle.getBundle( "bfdbgtest/Bundle" ).getString( "Running" ) );
                        break;
                    default:
                        setStatusText( java.util.ResourceBundle.getBundle( "bfdbgtest/Bundle" ).getString( "Internal_Error" ) );
                        kill();
                        setButtonsState();
                        throw new IllegalStateException();
                }
            }
            }
        });
        
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFileChooser1 = new javax.swing.JFileChooser();
        jScrollPane1 = new javax.swing.JScrollPane();
        sourceText = new javax.swing.JTextPane();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        statusLabel = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        debugOutputWindow = new javax.swing.JTextPane();
        buttonRun = new javax.swing.JButton();
        buttonPause = new javax.swing.JButton();
        buttonContinue = new javax.swing.JButton();
        buttonToggle = new javax.swing.JButton();
        buttonKill = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        buttonStep = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        memoryDumpTable = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Brainbugger - v.1.0M2");
        setLocationByPlatform(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        sourceText.setFont(new java.awt.Font("Monospaced", 0, 12));
        sourceText.setCaret(new BasicCaret()
        );
        sourceText.addInputMethodListener(new java.awt.event.InputMethodListener() {
            public void caretPositionChanged(java.awt.event.InputMethodEvent evt) {
            }
            public void inputMethodTextChanged(java.awt.event.InputMethodEvent evt) {
                sourceTextInputMethodTextChanged(evt);
            }
        });
        jScrollPane1.setViewportView(sourceText);

        jTextField1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextField1KeyReleased(evt);
            }
        });

        jButton1.setFont(new java.awt.Font("Dialog", 0, 8));
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("bfdbgtest/Bundle"); // NOI18N
        jButton1.setText(bundle.getString("...")); // NOI18N
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        statusLabel.setText(bundle.getString("Status")); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusLabel)
                .addContainerGap(488, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(statusLabel)
                .addContainerGap(3, Short.MAX_VALUE))
        );

        debugOutputWindow.setEditable(false);
        jScrollPane2.setViewportView(debugOutputWindow);

        buttonRun.setText(bundle.getString("Run")); // NOI18N
        buttonRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRunActionPerformed(evt);
            }
        });

        buttonPause.setText(bundle.getString("Pause")); // NOI18N
        buttonPause.setEnabled(false);
        buttonPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPauseActionPerformed(evt);
            }
        });

        buttonContinue.setText(bundle.getString("Continue")); // NOI18N
        buttonContinue.setEnabled(false);
        buttonContinue.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonContinueActionPerformed(evt);
            }
        });

        buttonToggle.setText(bundle.getString("Toggle")); // NOI18N
        buttonToggle.setEnabled(false);
        buttonToggle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonToggleActionPerformed(evt);
            }
        });

        buttonKill.setText(bundle.getString("Kill")); // NOI18N
        buttonKill.setEnabled(false);
        buttonKill.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonKillActionPerformed(evt);
            }
        });

        jButton2.setFont(new java.awt.Font("Dialog", 0, 8));
        jButton2.setText(bundle.getString("About")); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        buttonStep.setText(bundle.getString("Step")); // NOI18N
        buttonStep.setEnabled(false);
        buttonStep.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStepActionPerformed(evt);
            }
        });

        jScrollPane3.setWheelScrollingEnabled(false);

        memoryDumpTable.setModel(new MemoryDumpModel());
        memoryDumpTable.setColumnSelectionAllowed(true);
        memoryDumpTable.setFillsViewportHeight(true);
        memoryDumpTable.setShowHorizontalLines(false);
        memoryDumpTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane3.setViewportView(memoryDumpTable);
        memoryDumpTable.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 594, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 57, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(73, 73, 73))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jTextField1, javax.swing.GroupLayout.DEFAULT_SIZE, 481, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 594, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(buttonRun)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonPause)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonContinue)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonStep)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonToggle)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonKill))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 594, Short.MAX_VALUE)
                            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonRun)
                    .addComponent(buttonPause)
                    .addComponent(buttonContinue)
                    .addComponent(buttonToggle)
                    .addComponent(buttonKill)
                    .addComponent(buttonStep))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 192, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        if ( jFileChooser1.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION ) {
            setSourceFile( jFileChooser1.getSelectedFile().getAbsolutePath() );
            jTextField1.setText( sourceFile.getAbsolutePath() );
            setButtonsState();
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void buttonRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRunActionPerformed
        String rs = tryRun();
        if ( !rs.equals( java.util.ResourceBundle.getBundle( "bfdbgtest/Bundle" ).getString( "OK" ) ) )
            statusLabel.setText( rs );
        setButtonsState();
    }//GEN-LAST:event_buttonRunActionPerformed

    private void buttonPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPauseActionPerformed
        pause();
        setButtonsState();
    }//GEN-LAST:event_buttonPauseActionPerformed

    private void buttonContinueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonContinueActionPerformed
        cont();
        setButtonsState();
    }//GEN-LAST:event_buttonContinueActionPerformed

    private void buttonToggleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonToggleActionPerformed
        toggle( sourceText.getCaretPosition() );
        setButtonsState();
    }//GEN-LAST:event_buttonToggleActionPerformed

    private void buttonKillActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonKillActionPerformed
        kill();
        setButtonsState();
    }//GEN-LAST:event_buttonKillActionPerformed

    private void jTextField1KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextField1KeyReleased
        setSourceFile( jTextField1.getText() );
        setButtonsState();
    }//GEN-LAST:event_jTextField1KeyReleased

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        kill();
    }//GEN-LAST:event_formWindowClosing

    private void jButton2ActionPerformed (java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        new AboutDialog( this, true ).setVisible( true );
    }//GEN-LAST:event_jButton2ActionPerformed

    private void buttonStepActionPerformed (java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonStepActionPerformed
        if ( dbg_vm != null ) {
            dbg_vm.step();
        }
    }//GEN-LAST:event_buttonStepActionPerformed

    private void sourceTextInputMethodTextChanged (java.awt.event.InputMethodEvent evt) {//GEN-FIRST:event_sourceTextInputMethodTextChanged
        
    }//GEN-LAST:event_sourceTextInputMethodTextChanged


    public static void main ( final String args[] ) {

        try {
            String toBeFound = args.length == 0? "Nimbus" : args[0];
            String found = null;
            LookAndFeelInfo[] lfs = UIManager.getInstalledLookAndFeels();
            if( lfs != null ) {
                for( LookAndFeelInfo lf : lfs ) {
                    if( lf != null && lf.getName().equals( toBeFound ) ) {
                        found = lf.getClassName();
                        break;
                    }
                }
            }
            if( found != null ) {
                LookAndFeel lf = (LookAndFeel) Class.forName( found ).newInstance();
                if( lf.isSupportedLookAndFeel() )
                    UIManager.setLookAndFeel( lf );
            }
        } catch( Exception e ) {
            e.printStackTrace();
        }

        java.awt.EventQueue.invokeLater( new Runnable() {

            public void run () {
                new MainForm().setVisible( true );
            }
        });
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonContinue;
    private javax.swing.JButton buttonKill;
    private javax.swing.JButton buttonPause;
    private javax.swing.JButton buttonRun;
    private javax.swing.JButton buttonStep;
    private javax.swing.JButton buttonToggle;
    private javax.swing.JTextPane debugOutputWindow;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JFileChooser jFileChooser1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTable memoryDumpTable;
    private javax.swing.JTextPane sourceText;
    private javax.swing.JLabel statusLabel;
    // End of variables declaration//GEN-END:variables

    public void notifyDie ( DebugException dieException ) {
        synchronized ( lock ) {
            currentState = DebugState.NOT_RUNNING;
            setButtonsState();
            markBreakpoint( null, null, false );
            notifyException( dieException );
            model.validateDump();
        }
    }

    public void notifyBreakpoint ( BreakpointException e ) {
        synchronized ( lock ) {
            currentState = DebugState.PAUSED;
            setButtonsState();
            model.validateDump();
            if ( e != null ) {
                int pos = e.getInstruction().sourceIndex;
                markBreakpoint( pos, null, true );
                //writeln( java.util.ResourceBundle.getBundle("bfdbgtest/Bundle").getString("paused_at_") + dbg_vm.getCpu().getCurrentAddress() + java.util.ResourceBundle.getBundle("bfdbgtest/Bundle").getString(",_memory_state:_value_at_address_") + dbg_vm.getMemory().getAddress() + java.util.ResourceBundle.getBundle("bfdbgtest/Bundle").getString("_is_") + dbg_vm.getMemory().export() );
            }
        }
    }
    private int lastCurrent = -1;

    private void markBreakpoint ( final Integer _pos, final Boolean _isBreakpoint,
            final boolean isCurrent ) {
        EventQueue.invokeLater( new Runnable() {

            public void run () {
                Integer pos = _pos;
                Boolean isBreakpoint = _isBreakpoint;
                synchronized ( lock ) {
                    if ( pos == null )
                        pos = lastCurrent;
                    if ( pos >= 0 && pos < doc.getLength() ) {
                        Style newStyle;
                        if ( isBreakpoint == null )
                            isBreakpoint = breakpoints.contains( pos );
                        if ( isBreakpoint ) {
                            if ( isCurrent )
                                newStyle = bpCurrStyle;
                            else
                                newStyle = bpStyle;
                        } else {
                            if ( isCurrent )
                                newStyle = runPointStyle;
                            else
                                newStyle = normalStyle;
                        }
                        doc.setCharacterAttributes( pos, 1, newStyle, true );
                        sourceText.setCaretPosition( pos );
                        if ( isCurrent ) {
                            if ( pos != lastCurrent && lastCurrent != -1 ) {
                                newStyle = breakpoints.contains( lastCurrent ) ? bpStyle : normalStyle;
                                doc.setCharacterAttributes( lastCurrent, 1, newStyle, true );
                            }
                            lastCurrent = pos;
                        }
                    }
                }
            }
        });
        
    }

    public void notifyException ( final DebugException e ) {
        if ( e != null ) {
            EventQueue.invokeLater( new Runnable() {

                public void run () {
                    try {
                        String m = e.getMessage() + "\n";
                        consoleDoc.insertString( consoleDoc.getLength(), m, e.isFatal() ? redText : null );
                    } catch (BadLocationException ex) {
                        Logger.getLogger( MainForm.class.getName() ).log( Level.SEVERE, null, ex );
                    }
                    setButtonsState();
                    model.validateDump();
                }
            });
            
        }
    }

    private void notifyUnexpectedException ( Throwable t ) {
        if ( t != null ) {
            try {
                String m = t.getMessage() + "\n";
                consoleDoc.insertString( consoleDoc.getLength(), m, redText );
            } catch (BadLocationException ex) {
                Logger.getLogger( MainForm.class.getName() ).log( Level.SEVERE, null, ex );
            }
        }
    }

    private void writeln( final String str ) {
        if( str != null ) {
            EventQueue.invokeLater( new Runnable() {

                public void run () {
                    try {
                        consoleDoc.insertString( consoleDoc.getLength(), str + "\n", null );
                        debugOutputWindow.setCaretPosition( consoleDoc.getLength() );
                    } catch (BadLocationException ex) {
                        Logger.getLogger( MainForm.class.getName() ).log( Level.SEVERE, null, ex );
                    }
                }

            });
            
        }
    }

    private void setStatusText( String txt ) {
        statusLabel.setText( txt );
    }

    private MemoryDumpModel model ;

    class MemoryDumpModel implements TableModel {
        private Vector<TableModelListener> listeners = new Vector<TableModelListener>();

        private int[] cache = new int[ getColumnCount() ];

        public MemoryDumpModel () {
            model = this;
        }

        public int getRowCount () {
            return 1;
        }

        public int getColumnCount () {
            return 11;
        }

        public String getColumnName ( int columnIndex ) {
            columnIndex -= 5;
            if( columnIndex < 0 )
                return Integer.toString( columnIndex );
            else
                return "+" + columnIndex;
        }

        public Class<?> getColumnClass ( int columnIndex ) {
            return Integer.class;
        }

        public boolean isCellEditable ( int rowIndex, int columnIndex ) {
            return false;
        }

        public synchronized Object getValueAt ( int rowIndex, int columnIndex ) {
            if( rowIndex > 0 )
                return null;
            if( dbg_vm == null )
                return null;
            if( dbg_vm.isRunning() && dbg_vm.isPerofrming() )
                return cache[columnIndex];
            columnIndex -= 5;
            int value = 0;
            BfMemory memory = dbg_vm.getMemory();
            if( columnIndex > 0 )
                memory.forward( columnIndex );
            else if( columnIndex < 0 )
                memory.backward( -columnIndex );
            value = memory.export();
            if( columnIndex > 0 )
                memory.backward( columnIndex );
            else if( columnIndex < 0 )
                memory.forward( -columnIndex );
            cache[columnIndex + 5] = value;
            return Integer.valueOf( value );
        }

        public void setValueAt ( Object aValue, int rowIndex, int columnIndex ) {
            throw new UnsupportedOperationException( "Not supported." );
        }

        public void addTableModelListener ( TableModelListener l ) {
            listeners.add( l );
        }

        public void removeTableModelListener ( TableModelListener l ) {
            listeners.remove( l );
        }

        public void validateDump() {

                EventQueue.invokeLater( new Runnable() {

                    public void run () {
                        for( TableModelListener l : listeners ) {
                            l.tableChanged( new TableModelEvent( model, 0 ) );
                        }
                    }

                });
        }
    }
}

enum DebugState {

    NOT_LOADED,
    NOT_RUNNING,
    RUNNING_AND_PERFORMING,
    PAUSED
}


