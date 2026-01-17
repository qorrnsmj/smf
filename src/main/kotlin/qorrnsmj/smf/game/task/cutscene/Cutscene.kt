package qorrnsmj.smf.game.task.cutscene

import qorrnsmj.smf.game.task.sequence.Sequence
import qorrnsmj.smf.game.task.Task

abstract class Cutscene : Task() {
    protected val sequences: MutableList<Sequence> = mutableListOf()

    override fun update(delta: Float) {
        if (finished) return

        var allFinished = true
        for (seq in sequences) {
            if (!seq.isFinished()) {
                seq.update(delta)
                allFinished = false
            }
        }

        if (allFinished && sequences.isNotEmpty()) {
            finished = true
        }
    }

    override fun reset() {
        finished = false
        sequences.forEach { it.reset() }
    }

    fun addSequence(sequence: Sequence) {
        sequences.add(sequence)
    }
}
