package qorrnsmj.smf.game.state

import qorrnsmj.smf.game.state.custom.EmptyState
import qorrnsmj.smf.game.state.custom.ExampleState1
import qorrnsmj.smf.game.state.custom.ExampleState2

enum class States(val instance: State) {
    EMPTY(EmptyState()),
    EXAMPLE1(ExampleState1()),
    EXAMPLE2(ExampleState2())
}
