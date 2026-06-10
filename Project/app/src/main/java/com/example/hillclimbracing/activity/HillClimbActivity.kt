package com.example.hillclimbracing.activity

import com.example.hillclimbracing.scene.TitleScene
import kr.ac.tukorea.ge.spgp2026.a2dg.activity.BaseGameActivity
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class HillClimbActivity : BaseGameActivity() {
    override val drawsDebugGrid: Boolean = false
    override val drawsDebugInfo: Boolean = false
    override val drawsFpsGraph: Boolean = false

    override fun createRootScene(gctx: GameContext): Scene {
        return TitleScene(gctx)
    }
}
