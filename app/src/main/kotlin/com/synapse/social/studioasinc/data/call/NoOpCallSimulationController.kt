package com.synapse.social.studioasinc.data.call

/**
 * The production binding for [CallSimulationController]: it does nothing, and says so by name.
 *
 * The seam exists because Hilt cannot express "this dependency is only ever satisfied in a debug
 * build" — a ViewModel constructor needs a binding in every variant, so a release build gets this
 * instead of the simulator. The debug source set binds the timer-driven repository as the controller
 * when it is the installed repository, so the only build in which `simulate()` can change anything is
 * one where that repository is compiled in at all.
 *
 * The call UI additionally gates every debug affordance behind `BuildConfig.DEBUG`, so a release build
 * has neither the entry point nor the behaviour.
 */
object NoOpCallSimulationController : CallSimulationController {
    override fun simulate(action: CallSimulationAction) = Unit
}
