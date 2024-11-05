package qorrnsmj.smf.state

import qorrnsmj.smf.state.custom.EmptyState
import qorrnsmj.smf.state.custom.ExampleState1
import qorrnsmj.smf.state.custom.ExampleState2

enum class States(val instance: State) {
    EMPTY(EmptyState()),
    EXAMPLE1(ExampleState1()),
    EXAMPLE2(ExampleState2())
}
