/*
 *  Track.scala
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

package de.sciss.kontur.session

import java.io.{ IOException }
import scala.xml.{ Node }
import de.sciss.kontur.edit.{ Editor }

//trait Track[ T <: Stake ] extends SessionElement {
//  def trail: Trail[ T ]
//}

//object Track {
//  private type T
//  type Tr = Track[ T <: Stake[ T ]]
//}

// import Track.Tr

trait Track[ T <: Stake[ T ]] extends SessionElement {
  def trail: Trail[ T ]
  def editor: Option[ TrackEditor[ T ]]
}

trait TrackEditor[ T <: Stake[ T ]] extends Editor {
  
}

class Tracks( val id: Long, doc: Session, tl: BasicTimeline )
extends BasicSessionElementSeq[ Track[ _ <: Stake [ _ ]]]( doc, "Tracks" ) {

    def toXML = <tracks id={id.toString}>
  {innerToXML}
</tracks>

  def fromXML( parent: Node ) {
     val innerXML = SessionElement.getSingleXML( parent, "tracks" )
     innerFromXML( innerXML )
  }

  protected def elementsFromXML( node: Node ) : Seq[ Track[ _ <: Stake[ _ ]]] =
    node.child.filter( _.label != "#PCDATA" ).map( ch => ch.label match {
    case AudioTrack.XML_NODE => AudioTrack.fromXML( ch, doc, tl )
    case lb => throw new IOException( "Unknown track type '" + lb + "'" )
  })
}