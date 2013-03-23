/*
 *  Session.scala
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
package session

import util.{Model, BasicSerializerContext, SerializerContext}
import java.awt.EventQueue
import java.io.{File, IOException}
import scala.xml.{Node, XML}
import legacy.ProcessingThread
import scala.util.control.NonFatal
import de.sciss.desktop.impl.UndoManagerImpl
import de.sciss.desktop.UndoManager

object Session {
  def newEmpty = new Session(None)

  val XML_START_ELEMENT = "konturSession"

  def newFrom(path: File): Session = {
    val xml = XML.loadFile(path)
    if (xml.label != XML_START_ELEMENT) throw new IOException("Not a session file")
    val doc = new Session(Some(path))
    val c = new BasicSerializerContext
    try {
      doc.fromXML(c, xml)
    }
    catch {
      case NonFatal(e) => throw new IOException(e)
    }
    doc
  }

  case class DirtyChanged(newDirty: Boolean)
  case class PathChanged(oldPath: Option[File], newPath: Option[File])
}
class Session( private var pathVar: Option[ File ] )
extends /* BasicDocument with */ Model {
  doc =>

  import Session._

  private var pt: Option[ProcessingThread] = None
  private val undo = new UndoManagerImpl {
    def dirty = doc.dirty
    def dirty_=(value: Boolean) { doc.dirty = value}
  }
  private var _dirty = false

  //  private var path: Option[ File ] = None

    val timelines   = new Timelines( this )
    val audioFiles  = new AudioFileSeq( this )
    val diffusions  = new Diffusions( this )

//    def createID : Long = {
//      val res = idCount
//      idCount += 1
//      res
//    }

    // note: the order is crucial
    // in order to resolve dependancies when loading
    def toXML( c: SerializerContext ) = <konturSession>
    {audioFiles.toXML( c )}
    {diffusions.toXML( c )}
    {timelines.toXML( c )}
</konturSession>

  @throws(classOf[IOException])
  def fromXML(c: SerializerContext, elem: Node) {
    audioFiles.fromXML(c, elem)
    diffusions.fromXML(c, elem)
    timelines .fromXML(c, elem)
  }

  def save(f: File) {
    val c = new BasicSerializerContext
    XML.save(f.getAbsolutePath, toXML(c), "UTF-8", xmlDecl = true, null)
  }

  /**
   * Starts a <code>ProcessingThread</code>. Only one thread
   * can exist at a time. To ensure that no other thread is running,
   * call <code>checkProcess()</code>.
   *
   * __synchronization__: must be called in the event thread
   *
   * @param	process	the thread to launch
   * @throws	IllegalMonitorStateException	if called from outside the event thread
   * @throws	IllegalStateException			if another process is still running
   * @see	#checkProcess()
   */
  def start(process: ProcessingThread) {
    if (!EventQueue.isDispatchThread) throw new IllegalMonitorStateException()
    if (pt.isDefined) throw new IllegalStateException("Process already running")

    pt = Some(process)
    process.addListener(new ProcessingThread.Listener() {
      def processStarted(e: ProcessingThread.Event) {
        /* empty */
      }

      def processStopped(e: ProcessingThread.Event) {
        pt = None
      }
    })
    process.start()
  }

  def path = pathVar
  def path_=(newPath: Option[File]) {
    if (newPath != pathVar) {
      val change = PathChanged(pathVar, newPath)
      pathVar = newPath
      dispatch(change)
    }
  }

  def name: Option[String] = pathVar.map(p => {
    val n = p.getName
    val i = n.lastIndexOf('.')
    if (i == -1) n else n.substring(0, i)
  })

  def getName = name getOrElse null

  override def toString = "Session(" + name.getOrElse("<Untitled>") + ")"

  def displayName =
    name getOrElse getResourceString("frameUntitled")

  protected def getResourceString(key: String): String = key // XXX TODO getApplication().getResourceString( key )

  def dirty: Boolean = _dirty

  def dirty_=(value: Boolean) {
    if (_dirty != value) {
      _dirty = value
      dispatch(DirtyChanged(value))
    }
  }

  def undoManager: UndoManager = undo

  def dispose() {
    // nada
  }
}