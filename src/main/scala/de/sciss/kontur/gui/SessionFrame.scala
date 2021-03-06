/*
 *  SessionFrame.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2013 Hanns Holger Rutz. All rights reserved.
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
 */

package de.sciss.kontur
package gui

import java.awt.{ FileDialog, Frame }
import java.awt.event.{ ActionEvent, KeyEvent }
import java.io.{ File, IOException }
import javax.swing.{ AbstractAction, Action, JComponent, JOptionPane, KeyStroke, WindowConstants }
import de.sciss.app.AbstractWindow
import de.sciss.common.{ BasicMenuFactory, BasicWindowHandler, ShowWindowAction }
import de.sciss.gui.MenuAction
import session.Session
import de.sciss.util.Flag
import util.Model

trait SessionFrame {
   frame: AppWindow =>

   private var writeProtected	= false
   private var wpHaveWarned	= false

   private val actionShowWindow= new ShowWindowAction( this )
   protected val actionClose  = new ActionClose()
   private val actionSave     = new ActionSave()
   private val actionSaveAs	= new ActionSaveAs( false )

   private val winListener = new AbstractWindow.Adapter() {
     override def windowClosing(e: AbstractWindow.Event): Unit =
       frame.windowClosing()

        override def windowActivated( e: AbstractWindow.Event ): Unit = {
            // need to check 'disposed' to avoid runtime exception in doc handler if document was just closed
            if( !disposed ) {
                app.getDocumentHandler.setActiveDocument( frame, doc )
                app.getWindowHandler.asInstanceOf[ BasicWindowHandler ].setMenuBarBorrower( frame )
            }
        }
    }

   private val docListener: Model.Listener = {
      case Session.DirtyChanged( _ ) => updateTitle()
      case Session.PathChanged( _, _ ) => updateTitle()
   }

   private var disposed   = false;  // compiler problem, we cannot name it disposed
   
   // ---- constructor ----
   {
      // ---- menus and actions ----
      val mr = app.getMenuBarRoot
      mr.putMimic( "file.close",  this, actionClose )
      mr.putMimic( "file.save",   this, actionSave )
      mr.putMimic( "file.saveAs", this, actionSaveAs )

      mr.putMimic( "edit.undo", this, doc.getUndoManager.getUndoAction )
      mr.putMimic( "edit.redo", this, doc.getUndoManager.getRedoAction )
      
      updateTitle()
      doc.addListener( docListener )

      app.getMenuFactory.addToWindowMenu( actionShowWindow )	// MUST BE BEFORE INIT()!!
      setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE )
      addListener( winListener )
   }

   def doc: Session

   protected def elementName: Option[ String ]

   protected def windowClosing() : Unit

   protected def invokeDispose(): Unit = {
      disposed = true	// important to avoid "too late window messages" to be processed; fucking swing doesn't kill them despite listener being removed
      removeListener( winListener )
      doc.removeListener( docListener )
      app.getMenuFactory.removeFromWindowMenu( actionShowWindow )
      actionShowWindow.dispose()
      dispose()
   }

	/**
	 *  Recreates the main frame's title bar
	 *  after a sessions name changed (clear/load/save as session)
	 */
   protected def updateTitle(): Unit = {
      writeProtected	= doc.path.map( !_.canWrite ) getOrElse false

      val name = doc.displayName
      setTitle( (if( !internalFrames ) app.getName else "") +
                (if( doc.isDirty() ) " - \u2022" else " - ") + name + (elementName.map( e => " - " + e ) getOrElse "") )

      actionShowWindow.putValue( Action.NAME, name )
		actionSave.setEnabled( !writeProtected && doc.isDirty() )
		setDirty( doc.isDirty() )
        setWindowFile( doc.path getOrElse null )

//		final AudioFileInfoPalette infoBox = (AudioFileInfoPalette) app.getComponent( Main.COMP_AUDIOINFO )
//		if( infoBox != null ) infoBox.updateDocumentName( doc )

		if( writeProtected && !wpHaveWarned && doc.isDirty() ) {
			val op = new JOptionPane( getResourceString( "warnWriteProtected" ), JOptionPane.WARNING_MESSAGE )
			BasicWindowHandler.showDialog( op, getWindow, getResourceString( "msgDlgWarn" ))
			wpHaveWarned = true
		}
	}

//   override def dispose() {
//      frame.dispose()
//   }

   protected def documentClosed(): Unit = {
      app.getDocumentHandler.removeDocument( this, doc )	// XXX
      invokeDispose() // dispose()
   }

   def closeDocument( force: Boolean, wasClosed: Flag ): Unit = {
//      doc.getTransport().stop();
      val okToClose = force || {
         val name = getResourceString( "menuClose" )
//         if( !confirmCancel( name )) {
//            wasClosed.set( false );
//            return null;
//         }
         val saved = confirmUnsaved( name, wasClosed )
//         if( pt != null ) {
//            pt.addListener( new ProcessingThread.Listener() {
//               public void processStarted( ProcessingThread.Event e ) { /* ignored */ }
//               public void processStopped( ProcessingThread.Event e )
//               {
//                  if( e.isDone() ) {
//                     documentClosed();
//                  }
//               }
//            });
//            return pt;
//         }
         saved || wasClosed.isSet  // aka confirmed.isSet
      }
//println( "wasClosed : " + wasClosed.isSet )
      if( okToClose ) {
         documentClosed()
      }
//      return null;
   }


   /*
    *  Checks if there are unsaved changes to
    *  the session. If so, displays a confirmation
    *  dialog. Invokes Save/Save As depending
    *  on user selection. IF the doc was not dirty,
    *	or if &quot;Cancel&quot; or
    *	&quot;Don't save&quot; was chosen, the
    *	method returns <code>null</code> and the
    *	<code>confirmed</code> flag reflects whether
    *	the document should be closed. If a saving
    *	process should be started, that process is
    *	returned. Note that the <code>ProcessingThread</code>
    *	in this case has not yet been started, as to
    *	allow interested objects to install a listener
    *	first. So it's their job to call the <code>start</code>
    *	method!
    *
    *  @param  actionName		name of the action that
    *							threatens the session
    *	@param	confirmed		a flag that will be set to <code>true</code> if
    *							the doc is allowed to be closed
    *							(doc was not dirty or user chose &quot;Don't save&quot;),
    *							otherwise <code>false</code> (save process
    *							initiated or user chose &quot;Cancel&quot;).
    *  @return				true is file was saved
    *
    *	@see	de.sciss.eisenkraut.util.ProcessingThread#start
    */
   def confirmUnsaved( actionName: String, confirmed: Flag ) : Boolean = {
      if( !doc.isDirty() ) {
         confirmed.set( true )
         return false
      }

      val options = Array[ AnyRef ]( getResourceString( "buttonSave" ),
                           getResourceString( "buttonCancel" ),
                           getResourceString( "buttonDontSave" ))
      val dont = new Flag( false )

      val name = doc.displayName

      val op = new JOptionPane( name + " :\n" + getResourceString( "optionDlgUnsaved" ),
                            JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null,
                            options, options( 1 ))
      val d = op.createDialog( getWindow, actionName )
      val rp = d.getRootPane
      if( rp != null ) {
         rp.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ).put(
           KeyStroke.getKeyStroke( KeyEvent.VK_D, BasicMenuFactory.MENU_SHORTCUT ), "dont" )
         rp.getActionMap.put( "dont", new AbstractAction {
            def actionPerformed( e: ActionEvent ): Unit = {
               dont.set(  true )
               d.dispose()
            }
         })
      }
      BasicWindowHandler.showDialog( d )
      val choice = if( dont.isSet ) {
         2
      } else {
         val value = op.getValue
         if( (value == null) || (value == options( 1 ))) {
            1
         } else if( value == options( 0 )) {
            0
         } else if( value == options( 2 )) {
            2
         } else {
            -1	// throws assertion error in switch block
         }
      }

      choice match {
      case JOptionPane.CLOSED_OPTION | 1 => {
         confirmed.set( false )
         false
      }
      case 2 => {	// don't save
         confirmed.set( true )
         false
      }
      case 0 => {
         confirmed.set( false )
         val path = if( doc.path.isEmpty || writeProtected ) {
            actionSaveAs.query( actionSave.getValue( Action.NAME ).toString )
         } else {
            doc.path
         }
         path.map( p => {
            actionSave.perform( actionSave.getValue( Action.NAME ).toString, p, asCopy = false, openAfterSave = false )
         }) getOrElse false
      }
      case _ => {
         assert( assertion = false )
         false
      }}
   }

   protected class ActionClose extends MenuAction {
      def actionPerformed( e: ActionEvent ): Unit = perform()

      def perform(): Unit =
         closeDocument( force = false, new Flag( false ))
   }

   // action for the Save-Session menu item
   private class ActionSave
   extends MenuAction {
      /**
       *  Saves a Session. If the file
       *  wasn't saved before, a file chooser
       *  is shown before.
       */
      def actionPerformed( e: ActionEvent ): Unit = {
         val name = getValue( Action.NAME ).toString
         (doc.path orElse actionSaveAs.query( name )).foreach( f =>
            perform( name, f, asCopy = false, openAfterSave = false ))
      }

      protected[gui] def perform( name: String, file: File, asCopy: Boolean, openAfterSave: Boolean ) : Boolean = {
         try {
            doc.save( file )
            wpHaveWarned = false

            if( !asCopy ) {
               app.getMenuFactory.addRecent( file )
               doc.path = Some( file )
               doc.getUndoManager.discardAllEdits()
            }
            if( openAfterSave ) {
               app.getMenuFactory.openDocument( file )
            }
            true
         }
         catch { case e1: IOException =>
            BasicWindowHandler.showErrorDialog( getWindow, e1, name )
            false
         }
      }
   }

   // action for the Save-Session-As menu item
   private class ActionSaveAs( asCopy: Boolean )
   extends MenuAction {
      private val openAfterSave = new Flag( false )

      /*
       *  Query a file name from the user and save the Session
       */
      def actionPerformed( e: ActionEvent ): Unit = {
         val name = getValue( Action.NAME ).toString
         query( name ).foreach( f => {
            actionSave.perform( name, f, asCopy, openAfterSave.isSet )
         })
      }

		/**
		 *  Open a file chooser so the user
		 *  can select a new output file and format for the session.
		 *
		 *  @return the AudioFileDescr representing the chosen file path
		 *			and format or <code>null</code>
		 *			if the dialog was cancelled.
		 *
		 *	@todo	should warn user if saveMarkers is true and format does not support it
		 */
		protected[gui] def query( name: String ) : Option[ File ] = {
          val dlg = new FileDialog( null.asInstanceOf[ Frame ], name, FileDialog.SAVE )
          BasicWindowHandler.showDialog( dlg )
          val dirName   = dlg.getDirectory
          val fileName  = dlg.getFile
          if( dirName != null && fileName != null ) {
            Some( new File( dirName, fileName ))
          } else {
            None
          }
		}
	}
}