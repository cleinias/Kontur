/*
 *  TransportPanel.scala
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

import java.awt.{ Color, Component, Dimension, Font, GradientPaint, Graphics, Graphics2D, Insets, LinearGradientPaint, RenderingHints }
import java.awt.event.{ ActionEvent, ActionListener, KeyEvent }
import java.awt.geom.{ RoundRectangle2D }
import java.util.{ Locale }
import javax.swing.{ AbstractAction, AbstractButton, BorderFactory, Box, BoxLayout, ImageIcon,
                    JButton, JComponent, JLabel, JPanel, JToggleButton, KeyStroke, SwingConstants, Timer }
import javax.swing.border.{ Border }
import de.sciss.app.{ DynamicAncestorAdapter, DynamicListening }
import de.sciss.gui.{ TimeFormat }
import de.sciss.kontur.session.{ Timeline, Transport }

// temporary hack to get osc synced video
import de.sciss.scalaosc.{ OSCMessage, OSCTransmitter }

class TransportPanel( tlv: TimelineView )
extends SegmentedButtonPanel with DynamicListening {
   import Transport._

   private val transport= tlv.timeline.transport
   private val clz      = classOf[ TransportPanel ]
   private val butStop  = new JButton( new ImageIcon( clz.getResource( "transp_stop_20.png" )))
   private val butPlay  = new JButton( new ImageIcon( clz.getResource( "transp_play_20.png" )))
   private val lbTime   = new TimeLabel
   private var isPlaying= false

   // temporary hack to get osc synced video
   private lazy val osc  = {
      val t = OSCTransmitter( 'udp )
      t.target = new java.net.InetSocketAddress( "127.0.0.1", 57120 )
      t.connect
      t
   }
   private var oscEngaged  = false
   private val oscID       = 0 // tlv.timeline.id.toInt 

   private val transportListener = (msg: AnyRef) => msg match {
      case Play( pos, rate ) => trnspChanged( true )
      case Stop( pos ) => trnspChanged( false )
   }

   private val timelineViewListener = (msg: AnyRef) => msg match {
      case TimelineCursor.PositionChanged( _, _ ) => if( !isPlaying ) updateTimeLabel( true )
      case Timeline.RateChanged( _, _ ) => updateTimeLabel( true )
   }

   private val playTimer = new Timer( 27, new ActionListener {
      def actionPerformed( e: ActionEvent ) : Unit = updateTimeLabel( false )
   })

   // ---- constructor ----
   {
      val icnBeg  = new ImageIcon( clz.getResource( "transp_beg_20.png" ))
//      val icnStop =
      val icnStopA= new ImageIcon( clz.getResource( "transp_stopa_20.png" ))
//      val icnPlay =
      val icnPlayA= new ImageIcon( clz.getResource( "transp_playa_20.png" ))
      val icnEnd  = new ImageIcon( clz.getResource( "transp_end_20.png" ))

      val butBeg  = new JButton( icnBeg )
      val butEnd  = new JButton( icnEnd )
      butStop.setDisabledSelectedIcon( icnStopA )
      butPlay.setDisabledSelectedIcon( icnPlayA )

//      butStop.setAction( new ActionStop )
//      butPlay.setAction( new ActionPlay )
      butStop.addActionListener( new ActionStop )
      butPlay.addActionListener( new ActionPlay )

      setLayout( new BoxLayout( this, BoxLayout.X_AXIS ))
      setButtons( List( butBeg, butStop, butPlay, butEnd ))

      val imap	= getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW )
      val amap = getActionMap()
      imap.put( KeyStroke.getKeyStroke( KeyEvent.VK_SPACE, 0 ), "playstop" )
      amap.put( "playstop", new AbstractAction {
         def actionPerformed( e: ActionEvent ) {
            if( isPlaying ) {
               butStop.doClick() // ( 200 )
            } else {
               butPlay.doClick() // ( 200 )
            }
         }
      })

      add( lbTime, 0 )
      add( Box.createHorizontalStrut( 8 ), 1 )

      // --- osc ---
      val ggOSC = new JToggleButton( "OSC" )
      ggOSC.setFocusable( false )
      ggOSC.putClientProperty( "JComponent.sizeVariant", "mini" )
      ggOSC.putClientProperty( "JButton.buttonType", "square" )
      ggOSC.addActionListener( new ActionListener {
         def actionPerformed( e: ActionEvent ) {
            oscEngaged = ggOSC.isSelected
         }
      })
      add( Box.createHorizontalStrut( 8 ), 1 )
      add( ggOSC )

      new DynamicAncestorAdapter( this ).addTo( this )
   }

   private def trnspChanged( newIsPlaying: Boolean ) {
//      if( newIsPlaying != isPlaying ) {
         isPlaying = newIsPlaying
         butStop.setEnabled( isPlaying )
         butStop.setSelected( !isPlaying )
         butPlay.setEnabled( !isPlaying )
         butPlay.setSelected( isPlaying )
//      }
      updateTimeLabel( false )
      if( isPlaying ) {
         playTimer.restart()
      } else {
         playTimer.stop()
      }

      if( oscEngaged ) osc.send( OSCMessage( "/kontur", "transport", oscID, if( isPlaying ) "play" else "stop" ))
   }

   private def updateTimeLabel( sendOSC: Boolean ) {
      val pos  = if( isPlaying ) transport.map( _.currentPos ) getOrElse 0L else tlv.cursor.position
      val secs = pos / tlv.timeline.rate
      lbTime.setSeconds( secs )
      if( sendOSC && oscEngaged ) osc.send( OSCMessage( "/kontur", "transport", oscID, "pos", secs ))
   }

  def startListening {
     transport.foreach( t => {
        t.addListener( transportListener )
        trnspChanged( t.isPlaying )
     })
     tlv.addListener( timelineViewListener )
  }

  def stopListening {
     playTimer.stop()
     tlv.removeListener( timelineViewListener )
     transport.foreach( t => {
        t.removeListener( transportListener )
     })
//     isPlaying = false
  }

   private class ActionPlay extends AbstractAction {
      def actionPerformed( e: ActionEvent ) {
        transport.foreach( _.play( tlv.cursor.position, 1.0 ))
      }
   }

   private class ActionStop extends AbstractAction {
      def actionPerformed( e: ActionEvent ) {
        transport.foreach( _.stop )
      }
   }

   private class TimeLabel extends JLabel( "00:00:00.000", SwingConstants.CENTER ) {
      private var recentH = -1
      private var recentW = -1
      private var grad: LinearGradientPaint = null
      private val shape1 = new RoundRectangle2D.Float()
//      private val shape2 = new RoundRectangle2D.Float()
//      private val shape3 = new RoundRectangle2D.Float()
      private val colrBd1 = new Color( 0x00, 0x00, 0x00, 0x40 )
//      private val colrBd2 = new Color( 0x00, 0x00, 0x00, 0x40 )
      private val colrBd3 = new Color( 0xFF, 0xFF, 0xFF, 0xC0 )
      private val frmt = new TimeFormat( 0, null, null, 3, Locale.US )

      // ---- constructor ----
      setFont( new Font( "Lucida Grande", Font.BOLD, 13 ))
      setForeground( Color.black )
      setBorder( BorderFactory.createEmptyBorder( 3, 12, 4, 12 ))
      private val pref = super.getPreferredSize()

      override def getPreferredSize() : Dimension = pref

      override def paintComponent( g: Graphics ) {
         val h = getHeight; val w = getWidth
         if( h != recentH || w != recentW ) {
            recentH = getHeight
            recentW = getWidth
            recalcGrad
            recalcShape
         }
         val g2 = g.asInstanceOf[ Graphics2D ]
         val atOrig = g2.getTransform()
         val clipOrig = g2.getClip()
         g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON )
         g2.setPaint( grad )
//         g2.translate( 0, 1 )
         g2.fill( shape1 )
         g2.translate( 0, -1 )
         g2.setColor( colrBd1 )
         g2.draw( shape1 )
         g2.translate( 0, 0.2f )
         g2.clipRect( 0, 0, w, 3 )
         g2.draw( shape1 )
         g2.setClip( clipOrig )
         g2.translate( 0, 1 )
         g2.setColor( colrBd3 )
         g2.clipRect( 0, h - 5, w, 5 )
         g2.draw( shape1 )
         g2.setTransform( atOrig )
         g2.setClip( clipOrig )
         super.paintComponent( g )
      }

      private def recalcShape {
         shape1.setRoundRect( 0.2f, 1.2f, recentW - 1.4f, recentH - 2f, 8f, 8f )
//         shape1.setRoundRect( 0.5f, 0.5f, recentW - 2f, recentH - 2.5f, 8f, 8f )
//         shape3.setRoundRect( 0.2f, 1.2f, recentW - 1.4f, recentH - 2.4f, 8f, 8f )
      }

      def setSeconds( secs: Double ) {
         setText( frmt.formatTime( secs ))
      }

      private def recalcGrad {
         val m6 = (recentH - 6).toFloat
         val mid = (m6 / 2).toInt
//         grad = new LinearGradientPaint( 0, 2, 0, recentH - 6,
//            Array( 0f, mid / m6, (mid + 1) / m6, (m6 - 2) / m6, 1f ),
//            Array( new Color( 0xEC, 0xF2, 0xE0 ),
//                   new Color( 0xE5, 0xEC, 0xD4 ),
//                   new Color( 0xDE, 0xE5, 0xC6 ),
//                   new Color( 0xF6, 0xFC, 0xDF ),
//                   new Color( 0xF2, 0xF6, 0xDF )))
         grad = new LinearGradientPaint( 0, 2, 0, recentH - 6,
            Array( 0f, mid / m6, (mid + 1) / m6, 1f ),
            Array( new Color( 0xEC, 0xF2, 0xE0 ),
                   new Color( 0xE5, 0xEC, 0xD4 ),
                   new Color( 0xDE, 0xE5, 0xC6 ),
                   new Color( 0xF2, 0xF6, 0xDF )))
      }
   }
}