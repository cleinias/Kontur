/*
 *  MenuFactory.scala
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
import de.sciss.common.{ BasicApplication, BasicMenuFactory, BasicWindowHandler }
import de.sciss.gui.{ MenuAction, MenuGroup, MenuItem }
import de.sciss.kontur.{ Main }
import de.sciss.kontur.util.{ PrefsUtil }
import de.sciss.kontur.session.{ Session }
import java.awt.{ FileDialog, Frame }
import java.awt.event.{ ActionEvent, InputEvent, KeyEvent }
import java.io.{ File, FilenameFilter, FileReader, IOException }
import javax.swing.{ Action, KeyStroke }
import javax.xml.parsers.{ SAXParserFactory, SAXParser }
import org.xml.sax.{ Attributes, InputSource, SAXException }
import org.xml.sax.helpers.{ DefaultHandler }

class MenuFactory( app: BasicApplication )
extends BasicMenuFactory( app ) {
  import BasicMenuFactory._

   // ---- actions ----
   private val actionOpen = new ActionOpen( getResourceString( "menuOpen" ),
										    KeyStroke.getKeyStroke( KeyEvent.VK_O,
                                            MENU_SHORTCUT ))
	private val actionNewEmpty = new ActionNewEmpty( getResourceString( "menuNewEmpty" ),
											KeyStroke.getKeyStroke( KeyEvent.VK_N,
                                            MENU_SHORTCUT ))

    def openDocument( f: File ) {
		actionOpen.perform( f )
	}

	def showPreferences() {
		var prefsFrame = app.getComponent( Main.COMP_PREFS ).asInstanceOf[ PrefsFrame ]

		if( prefsFrame == null ) {
			prefsFrame = new PrefsFrame()
		}
		prefsFrame.setVisible( true )
		prefsFrame.toFront()
	}

	protected def getOpenAction() : Action = actionOpen

  	protected def addMenuItems() {
		// --- file menu ---

		val mgFile      = get( "file" ).asInstanceOf[ MenuGroup ]
		val smgFileNew  = new MenuGroup( "new", getResourceString( "menuNew" ))
		smgFileNew.add( new MenuItem( "empty", actionNewEmpty ))
		mgFile.add( smgFileNew, 0 )

  		// --- timeline menu ---
		val mgTimeline  = new MenuGroup( "timeline", getResourceString( "menuTimeline" ))
		mgTimeline.add( new MenuItem( "trimToSelection", getResourceString( "menuTrimToSelection" ),
							  KeyStroke.getKeyStroke( KeyEvent.VK_F5, MENU_SHORTCUT )))

		mgTimeline.add( new MenuItem( "insertSpan", getResourceString( "menuInsertSpan" ),
							  KeyStroke.getKeyStroke( KeyEvent.VK_E, MENU_SHORTCUT + InputEvent.SHIFT_MASK )))
		add( mgTimeline, indexOf( "edit" ) + 1 )
    }

  // ---- internal classes ----
	// action for the New-Empty Document menu item
	protected class ActionNewEmpty( text: String, shortcut: KeyStroke )
	extends MenuAction( text, shortcut )
	{
		def actionPerformed( e: ActionEvent ) {
//			final AudioFileDescr afd = query();
//			if( afd != null ) {
              perform // ( afd )
//            }
		}

		protected def perform: Session = {
//			try {
				val doc = Session.newEmpty // ( afd );
				app.getDocumentHandler().addDocument( this, doc )
				new SessionFrame( doc )
				doc
//			}
//			catch( IOException e1 ) {	// should never happen
//				BasicWindowHandler.showErrorDialog( null, e1, getValue( Action.NAME ).toString() );
//				return null;
//			}
		}
	}

	protected class ActionOpen( text: String, shortcut: KeyStroke )
	extends MenuAction( text, shortcut )
	{
		/*
		 *  Open a Session. If the current Session
		 *  contains unsaved changes, the user is prompted
		 *  to confirm. A file chooser will pop up for
		 *  the user to select the session to open.
		 */
		def actionPerformed( e: ActionEvent ) {
			queryFile().foreach( f => perform( f ))
		}

        private def queryFile() : Option[ File ] = {
			val w = app.getComponent( Main.COMP_MAIN ).asInstanceOf[ AbstractWindow ]
			val frame	= w.getWindow() match {
               case f: Frame => f
               case _ => null
            }
			val prefs = app.getUserPrefs()

			val fDlg = new FileDialog( frame, getResourceString( "fileDlgOpenSession" ), FileDialog.LOAD )
			fDlg.setDirectory( prefs.get( PrefsUtil.KEY_FILEOPENDIR, System.getProperty( "user.home" )))
            val accept = try {
              Some( new Acceptor )
            }
            catch { case _ => None }
            accept.foreach( a => fDlg.setFilenameFilter( a ))
			fDlg.setVisible( true )
            accept.foreach( _.dispose )
			val strDir	= fDlg.getDirectory()
			val strFile	= fDlg.getFile()

			if( strFile == null ) return None;   // means the dialog was cancelled

			// save dir prefs
			prefs.put( PrefsUtil.KEY_FILEOPENDIR, strDir )

			Some( new File( strDir, strFile ))
		}

        private class Acceptor extends DefaultHandler with FilenameFilter {
            val factory = SAXParserFactory.newInstance()
            val parser  = factory.newSAXParser()

            def accept( dir: File, name: String ) : Boolean = {
               val file = new File( dir, name )
               if( !file.isFile || !file.canRead ) return false
               try {
                 var reader = new FileReader( file )
                 try {
                    // note that the parsing is hell slow for some reason.
                    // therefore we do a quick magic cookie check first
                    val cookie = new Array[ Char ]( 5 )
                    reader.read( cookie )
                    if( new String( cookie ) != "<?xml" ) return false
                    // sucky FileReader does not support reset
//                    reader.reset()
                    reader.close()
                    reader = new FileReader( file )
                    val is = new InputSource( reader )
                    parser.reset()
                    parser.parse( is, this )
                    false
                 }
                 catch {
                    case e: SessionFoundException => true
                    case _ => false
                 }
                 finally {
                    try { reader.close() } catch { case e => }
                 }
               }
              catch { case e1: IOException => false }
            }

            @throws( classOf[ SAXException ])
            override def startElement( uri: String, localName: String,
              qName: String, attributes: Attributes ) {

                // eventually we will have a version check here
                // (using attributes) and
                // could then throw more detailed information
                throw (if( qName == Session.XML_START_ELEMENT ) new SessionFoundException
                else new SessionNotFoundException)
            }

            def dispose {
              // nothing actually
            }

            private class SessionFoundException extends SAXException
            private class SessionNotFoundException extends SAXException
        }

        /**
     	 *  Loads a new document file.
		 *  a <code>ProcessingThread</code>
		 *  started which loads the new session.
		 *
		 *  @param  path	the file of the document to be loaded
		 *
		 *  @synchronization	this method must be called in event thread
		 */
		def perform( path: File ) {
//			Session	doc;

//			// check if the document is already open
//			doc = findDocumentForPath( path )
//			if( doc != null ) {
//				doc.getFrame().setVisible( true );
//				doc.getFrame().toFront();
//				return;
//			}

			try {
				val doc = Session.newFrom( path )
				addRecent( path )
				app.getDocumentHandler().addDocument( this, doc )
				new SessionFrame( doc )
			}
			catch { case e1: IOException =>
				BasicWindowHandler.showErrorDialog( null, e1, getValue( Action.NAME ).toString() )
			}
		}
	}
}
