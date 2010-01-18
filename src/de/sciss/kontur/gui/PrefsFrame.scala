/*
 *  PrefsFrame.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2010 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 */

package de.sciss.kontur.gui

import de.sciss.app.{ AbstractApplication, AbstractWindow }
import de.sciss.gui.{ ComboBoxEditorBorder, PathField, PrefCheckBox, PrefParamField,
                     StringItem, TreeExpanderButton }
import de.sciss.common.{ BasicPathField }
import de.sciss.util.{ Param, ParamSpace }
import de.sciss.kontur.{ Main }
import de.sciss.kontur.util.{ PrefsUtil }
import PrefsUtil._
import java.awt.{ BorderLayout, Color, SystemColor }
import java.awt.event.{ ActionEvent }
import java.util.prefs.{ Preferences }
import javax.swing.{ AbstractAction, AbstractButton, ButtonGroup, GroupLayout,
                    JButton, JComboBox, JComponent, JLabel, JPanel, JScrollPane,
                    JTable, JToggleButton, JToolBar, ScrollPaneConstants,
                    WindowConstants }

class PrefsFrame extends AppWindow( AbstractWindow.SUPPORT ) {

    // ---- constructor ----
    {
      setTitle( "Preferences" ) // getResourceString( "framePrefs" )
      setResizable( false )
      makeUnifiedLook
      
//	  val app = AbstractApplication.getApplication()

      val cp = getContentPane
      val tb = new JToolBar()
      val bg = new ButtonGroup()
      tb.setFloatable( false )
      val layout = new BorderLayout()
      cp.setLayout( layout )

      def activateTab( tab: AbstractAction ) {
        val panel = tab.getValue( "de.sciss.tabpanel" ).asInstanceOf[ JComponent ]
        val old = layout.getLayoutComponent( BorderLayout.CENTER )
        if( old != null ) cp.remove( old )
        cp.add( panel, BorderLayout.CENTER )
        pack()
      }

      def newTab( key: String, panel: JComponent ) : AbstractButton = {
        val action = new AbstractAction( getResourceString( key )) {
            def actionPerformed( e: ActionEvent ) : Unit = activateTab( this )
        }
        val ggTab = new JToggleButton( action )
        ggTab.putClientProperty( "JButton.buttonType", "toolbar" )
        action.putValue( "de.sciss.tabpanel", panel )
        tb.add( ggTab )
        bg.add( ggTab )
        // action
        ggTab
      }

      val tabGeneral = newTab( "prefsGeneral", generalPanel )

		// ---------- audio pane ----------

      newTab( "prefsAudio", audioPanel )
      
      cp.add( tb, BorderLayout.NORTH )
//     cp.add( panel, BorderLayout.CENTER )
//      activateTab( tabGeneral )
      tabGeneral.doClick()
      
     // ---------- listeners ----------

      addListener( new AbstractWindow.Adapter() {
			override def windowClosing( e: AbstractWindow.Event ) {
				setVisible( false )
				dispose()
			}
      })

	   setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE )
	   init()
	   app.addComponent( Main.COMP_PREFS, this )
    }

    private def createAudioBoxGUI( prefs: Preferences ) : JComponent = {
        val table = new JTable()
		val ggScroll = new JScrollPane( table, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
		        	                   		   ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER )
        ggScroll
    }

    private def createPanel : Tuple2[ JPanel, GroupLayout ] = {
        val panel   = new JPanel()
        panel.setBackground( SystemColor.control ) // new Color( 216, 216, 216 )
        val layout  = new GroupLayout( panel )
        layout.setAutoCreateGaps( true )
        layout.setAutoCreateContainerGaps( true )
        panel.setLayout( layout )
        (panel, layout)
    }

    private def generalPanel : JComponent = {

        val (panel, layout) = createPanel

        val ggOne   = new JLabel( "One" )
        val ggTwo   = new javax.swing.JTextField( "Two" )
        val ggThree = new JLabel( "Three" )
        val ggFour  = new javax.swing.JTextField( "Four" )

        layout.setHorizontalGroup( layout.createSequentialGroup()
          .addGroup( layout.createParallelGroup()
            .addComponent( ggOne )
            .addComponent( ggThree )
          )
          .addGroup( layout.createParallelGroup()
            .addComponent( ggTwo )
            .addComponent( ggFour )
          )
        )

        layout.setVerticalGroup( layout.createSequentialGroup()
          .addGroup( layout.createParallelGroup( GroupLayout.Alignment.BASELINE )
            .addComponent( ggOne )
            .addComponent( ggTwo )
          )
          .addGroup( layout.createParallelGroup( GroupLayout.Alignment.BASELINE )
            .addComponent( ggThree )
            .addComponent( ggFour )
          )
        )
        panel
    }

    private def audioPanel : JComponent = {
		val prefs   = app.getUserPrefs().node( NODE_AUDIO );
//		val abPrefs	= prefs.node( NODE_AUDIOBOXES );

        val (panel, layout) = createPanel
val bg = panel.getBackground

        val resApp  = getResourceString( "prefsSuperColliderApp" )
        val lbApp   = new JLabel( resApp )
        val ggApp   = new BasicPathField( PathField.TYPE_INPUTFILE, resApp )
        ggApp.setPreferences( prefs, KEY_SUPERCOLLIDERAPP )
ggApp.setBackground( bg )

        val lbBoot  = new JLabel( getResourceString( "prefsAutoBoot" ))
        val ggBoot  = new PrefCheckBox( " " ) // space to have proper baseline!
        ggBoot.setPreferences( prefs, KEY_AUTOBOOT )

        val lbInterfaces = new JLabel( getResourceString( "labelAudioIFs" ))
        val ggInterfaces = createAudioBoxGUI( prefs.node( NODE_AUDIOBOXES ))

		val lbRate = new JLabel( getResourceString( "prefsAudioRate" ))
		val ggRateParam  = new PrefParamField()
		ggRateParam.addSpace( ParamSpace.spcFreqHertz )
		val ggRate = new JComboBox()
        val RATE_ITEMS = List(
          new StringItem( new Param( 0, ParamSpace.FREQ | ParamSpace.HERTZ ).toString, "System Default" ),
          new StringItem( new Param( 44100, ParamSpace.FREQ | ParamSpace.HERTZ ).toString, "44.1 kHz" ),
          new StringItem( new Param( 48000, ParamSpace.FREQ | ParamSpace.HERTZ ).toString, "48 kHz" ),
          new StringItem( new Param( 88200, ParamSpace.FREQ | ParamSpace.HERTZ ).toString, "88.2 kHz" ),
          new StringItem( new Param( 96000, ParamSpace.FREQ | ParamSpace.HERTZ ).toString, "96 kHz" )
        )
		for( item <- RATE_ITEMS ) ggRate.addItem( item )
		ggRateParam.setBorder( new ComboBoxEditorBorder() )
		ggRate.setEditor( ggRateParam )
		ggRate.setEditable( true )
		ggRateParam.setPreferences( prefs, KEY_AUDIORATE ) // important to be _afer_ setEditor because otherwise prefs get overwritten!
ggRateParam.setBackground( bg )

        val lbAdvanced  = new JLabel( getResourceString( "prefsAdvanced" ))
		val ggAdvanced  = new TreeExpanderButton()

        layout.setHorizontalGroup( layout.createSequentialGroup()
          .addGroup( layout.createParallelGroup( GroupLayout.Alignment.TRAILING )
            .addComponent( lbApp )
            .addComponent( lbBoot )
            .addComponent( lbInterfaces )
            .addComponent( lbRate )
            .addGroup( layout.createSequentialGroup()
              .addComponent( lbAdvanced )
              .addComponent( ggAdvanced )
            )
          )
          .addGroup( layout.createParallelGroup( GroupLayout.Alignment.LEADING )
            .addComponent( ggApp )
            .addComponent( ggBoot )
            .addComponent( ggInterfaces )
            .addComponent( ggRate )
          )
        )

        layout.setVerticalGroup( layout.createSequentialGroup()
          .addGroup( layout.createParallelGroup( GroupLayout.Alignment.BASELINE )
            .addComponent( lbApp )
            .addComponent( ggApp )
          )
          .addGroup( layout.createParallelGroup( GroupLayout.Alignment.BASELINE )
            .addComponent( lbBoot )
            .addComponent( ggBoot )
          )
          .addGroup( layout.createParallelGroup( GroupLayout.Alignment.LEADING )
            .addComponent( lbInterfaces )
            .addComponent( ggInterfaces )
          )
          .addGroup( layout.createParallelGroup( GroupLayout.Alignment.BASELINE )
            .addComponent( lbRate )
            .addComponent( ggRate )
          )
          .addGroup( layout.createParallelGroup( GroupLayout.Alignment.BASELINE )
            .addComponent( lbAdvanced )
            .addComponent( ggAdvanced )
          )
        )

//    layout.linkSize(SwingConstants.HORIZONTAL, findButton, cancelButton)


/*
		collAudioAdvanced = new ArrayList();

		row++;
		key2	= "prefsSuperColliderOSC";
		lb		= new JLabel( getResourceString( key2 ), TRAILING );
		lb.setVisible( false );
		collAudioAdvanced.add( lb );
		tab.gridAdd( lb, 0, row );
		key		= KEY_SCPROTOCOL;
		key2	= "prefsOSCProtocol";
		lb		= new JLabel( getResourceString( key2 ), TRAILING );
		b		= Box.createHorizontalBox();
		b.add( Box.createHorizontalStrut( 4 ));
		b.add( lb );
		ggChoice = new PrefComboBox();
		ggChoice.addItem( new StringItem( OSCChannel.TCP, "TCP" ));
		ggChoice.addItem( new StringItem( OSCChannel.UDP, "UDP" ));
		ggChoice.setPreferences( prefs, key );
		b.add( ggChoice );

		key		= KEY_SCPORT;
		key2	= "prefsOSCPort";
		lb		= new JLabel( getResourceString( key2 ), TRAILING );
		b.add( Box.createHorizontalStrut( 16 ));
		b.add( lb );
		ggParam  = new PrefParamField();
		ggParam.addSpace( spcIntegerFromZero );
		ggParam.setPreferences( prefs, key );
		b.add( ggParam );
		b.setVisible( false );
		collAudioAdvanced.add( b );
		tab.gridAdd( b, 1, row, -1, 1 );

		row++;
		key		= KEY_SCBLOCKSIZE;
		key2	= "prefsSCBlockSize";
		lb		= new JLabel( getResourceString( key2 ), TRAILING );
		lb.setVisible( false );
		collAudioAdvanced.add( lb );
		tab.gridAdd( lb, 0, row );
		ggParam  = new PrefParamField();
		ggParam.addSpace( spcIntegerFromOne );
		ggParam.setPreferences( prefs, key );
		ggParam.setVisible( false );
		collAudioAdvanced.add( ggParam );
		tab.gridAdd( ggParam, 1, row, -1, 1 );

		row++;
		key		= KEY_AUDIOBUSSES;
		key2	= "prefsAudioBusses";
		lb		= new JLabel( getResourceString( key2 ), TRAILING );
		lb.setVisible( false );
		collAudioAdvanced.add( lb );
		tab.gridAdd( lb, 0, row );
		ggParam  = new PrefParamField();
		ggParam.addSpace( spcIntegerFromOne );
		ggParam.setPreferences( prefs, key );
		ggParam.setVisible( false );
		collAudioAdvanced.add( ggParam );
		tab.gridAdd( ggParam, 1, row, -1, 1 );

		row++;
		key		= KEY_SCMEMSIZE;
		key2	= "prefsSCMemSize";
		lb		= new JLabel( getResourceString( key2 ), TRAILING );
		lb.setVisible( false );
		collAudioAdvanced.add( lb );
		tab.gridAdd( lb, 0, row );
		ggParam  = new PrefParamField();
		ggParam.addSpace( spcIntegerFromOne );
		ggParam.setPreferences( prefs, key );
		ggParam.setVisible( false );
		collAudioAdvanced.add( ggParam );
		tab.gridAdd( ggParam, 1, row, -1, 1 );

		row++;
		key		= KEY_SCRENDEZVOUS;
		key2	= "prefsSCRendezvous";
		lb		= new JLabel( getResourceString( key2 ), TRAILING );
		lb.setVisible( false );
		collAudioAdvanced.add( lb );
		tab.gridAdd( lb, 0, row );
		ggCheckBox  = new PrefCheckBox();
		ggCheckBox.setPreferences( prefs, key );
		ggCheckBox.setVisible( false );
		collAudioAdvanced.add( ggCheckBox );
		tab.gridAdd( ggCheckBox, 1, row, -1, 1 );
*/
/*
final SpringPanel tabAudio = tab;
		ggTreeAudio.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e )
			{
				final int		width	= getWindow().getWidth();
				final int		height	= getWindow().getHeight();
				final boolean	visible	= ggTreeAudio.isExpanded();
				int				delta	= 0;
				int				d2;

				for( int i = 0; i < collAudioAdvanced.size(); ) {
					d2 = 0;
					for( int j = i + 2; i < j; i++ ) {
						d2 = Math.max( d2, ((JComponent) collAudioAdvanced.get( i )).getPreferredSize().height );
					}
					delta = delta + d2 + 2;
				}

				for( int i = 0; i < collAudioAdvanced.size(); i++ ) {
					((JComponent) collAudioAdvanced.get( i )).setVisible( visible );
				}
				tabAudio.makeCompactGrid();
				getWindow().setSize( width, height + (visible ? delta : -delta ));
			}
		});
*/
      panel
    }
}
