/*
 *  Main.scala
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

import io.EisenkrautClient
import javax.swing.UIManager
import gui.{MenuFactory, GlobalActions, MainFrame, SuperColliderFrame}
import session.Session
import util.{Flag, PrefsUtil}
import sc.SuperColliderClient
import swing.Swing
import legacy.ProcessingThread
import de.sciss.desktop.impl.{SwingApplicationImpl, DocumentHandlerImpl, WindowHandlerImpl, ApplicationImpl}
import de.sciss.desktop.{DocumentHandler, Application, Menu, Preferences, SwingApplication, WindowHandler}

/**
 *  The <code>Main</code> class contains the java VM
 *  startup static <code>main</code> method which
 *  creates a new instance of <code>Main</code>. This instance
 *  will initialize localized strings (ResourceBundle),
 *  Preferences, the <code>transport</code>, the <code>menuFactory</code>
 *  object (a prototype of the applications menu and its
 *  actions).
 *  <p>
 *  Common components are created and registered:
 *  <code>SuperColliderFrame</code>, <code>TransportPalette</code>,
 *  <code>ObserverPalette</code>, and <code>DocumentFrame</code>.
 *  <p>
 *  The <code>Main</code> class extends the <code>Application</code>
 *  class from the <code>de.sciss.app</code> package.
 *
 *	@todo		OSC /main/quit doesn't work repeatedly
 *				; seems to be a problem of menuFactory.closeAll!
 */

object Kontur extends SwingApplicationImpl("Kontur") {
  type Document = Session

  /*
   *  The MacOS file creator string.
   */
  private val CREATOR = "Ttm "

  /**
   * Value for add/getComponent(): the preferences frame
   *
   * @see	#getComponent( Object )
   */
  final val COMP_PREFS = "Prefs"
  /**
   * Value for add/getComponent(): the observer palette
   *
   * @see	#getComponent( Object )
   */
  final val COMP_OBSERVER = "Observer"
  /**
   * Value for add/getComponent(): the main log frame
   *
   * @see	#getComponent( Object )
   */
  final val COMP_MAIN = "Main"
  /**
   * Value for add/getComponent(): the online help display frame
   *
   * @see	#getComponent( Object )
   */
  final val COMP_HELP = "Help"

  final val COMP_CTRLROOM = "ControlRoom"

  lazy val eisenkraut = new EisenkrautClient()(this)

  private val quitAfterSaveListener = new ProcessingThread.Listener {
    def processStarted(e: ProcessingThread.Event) {
      /* empty */
    }

    // if the saving was successfull, we will call closeAll again
    def processStopped(e: ProcessingThread.Event) {
      if (e.isDone) quit()
    }
  }

	/**
	 *	The arguments may contain the following options:
	 *	<UL>
	 *	<LI>-laf &lt;screenName&gt; &lt;className&gt; : set the default look-and-feel</LI>
	 *	</UL>
	 *
	 *	All other arguments not starting with a hyphen are considered to be paths to documents
	 *	that will be opened after launch.
	 */
	protected def init() {
		val prefs   = userPrefs
    var lafName = prefs.get[String](PrefsUtil.KEY_LOOKANDFEEL)(Preferences.Type.string).orNull  // XXX TODO: scalac bug
    var openDoc = scala.collection.immutable.Queue[String]()
    var i = 0
    while (i < args.length) {
      if (args(i).startsWith("-")) {
        if (args(i).equals("-laf")) {
          if ((i + 2) < args.length) {
            UIManager.installLookAndFeel(args(i + 1), args(i + 2))
            if (lafName == null) lafName = args(i + 2)
            i += 2
          } else {
            Console.err.println("Option -laf requires two additional arguments (screen-name and class-name).")
            sys.exit(1)
          }
        } else {
          Console.err.println("Unknown option " + args(i))
          sys.exit(1)
        }
      } else {
        openDoc = openDoc.enqueue(args(i))
      }
      i += 1
    }

//    init()

		// ---- component views ----

    val mainFrame = new MainFrame()
    val scFrame   = new SuperColliderFrame()
    scFrame.visible = true
  }

  lazy protected val menuFactory = (new MenuFactory).root

//	protected def createMenuFactory() : BasicMenuFactory = new MenuFactory( this )

	private var shouldForceQuit = false

  def quit() {
    val confirmed = Flag.False()
    val ptOpt     = GlobalActions.closeAll(shouldForceQuit, confirmed)

    ptOpt match {
      case Some(pt) =>
        pt.addListener(quitAfterSaveListener)
        pt.getClientArg("doc").asInstanceOf[Session].start(pt)
      case _ =>
        if (confirmed()) {
          SuperColliderClient.instance.quit()
          sys.exit(0)
        }
    }
  }

  def forceQuit() {
    shouldForceQuit = true
    quit()
  }

//  def menuBarRoot: Menu.Root = ...

  //	def getMacOSCreator : String = Kontur.CREATOR
//	def getVersion: Double = Kontur.version
}