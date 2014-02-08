/*
 *  Kontur.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2014 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU General Public License v2+
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
import legacy.ProcessingThread
import de.sciss.desktop.impl.SwingApplicationImpl
import de.sciss.desktop.Preferences
import de.sciss.sonogram
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import java.io.File

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

  //  /*
  //   *  The MacOS file creator string.
  //   */
  //  private val CREATOR = "Ttm "

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

  final val COMP_SONO = "SonogramManager"

  lazy val eisenkraut = new EisenkrautClient()(this)

  private val quitAfterSaveListener = new ProcessingThread.Listener {
    def processStarted(e: ProcessingThread.Event): Unit = ()

    // if the saving was successfull, we will call closeAll again
    def processStopped(e: ProcessingThread.Event): Unit = if (e.isDone) quit()
  }

	/** The arguments may contain the following options:
	  *	<UL>
	  *	<LI>-laf &lt;screenName&gt; &lt;className&gt; : set the default look-and-feel</LI>
	  *	</UL>
	  *
	  *	All other arguments not starting with a hyphen are considered to be paths to documents
	  *	that will be opened after launch.
	  */
	override protected def init(): Unit = {
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
    val sono                = sonogram.OverviewManager.Config()
    sono.executionContext   = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())
    val sonoDir             = new File(new File(sys.props("user.home"), name), "cache")
    sonoDir.mkdirs()
    sono.caching            = Some(sonogram.OverviewManager.Caching(folder = sonoDir, sizeLimit = 10L << 10 << 10 << 10))
    addComponent(COMP_SONO, sonogram.OverviewManager(sono))

    /* val mainFrame = */ new MainFrame()
    val scFrame   = new SuperColliderFrame()
    scFrame.visible = true
  }

  protected def menuFactory = MenuFactory.root

//	protected def createMenuFactory() : BasicMenuFactory = new MenuFactory( this )

	private var shouldForceQuit = false

  override def quit(): Unit = {
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

  def forceQuit(): Unit = {
    shouldForceQuit = true
    quit()
  }

//  def menuBarRoot: Menu.Root = ...

  //	def getMacOSCreator : String = Kontur.CREATOR
//	def getVersion: Double = Kontur.version
}