package akka.persistence

import akka.persistence.SnapshotProtocol.SaveSnapshot

trait SnapshotterExt extends Snapshotter {
  // We need this because snapshots in actor should have a queue offset, not an internal akka persistence's one
  protected def saveSnapshot(persistenceId: String, snapshotSequenceNr: Long, snapshot: Any): Unit =
    snapshotStore ! SaveSnapshot(SnapshotMetadata(persistenceId, snapshotSequenceNr), snapshot)
}
