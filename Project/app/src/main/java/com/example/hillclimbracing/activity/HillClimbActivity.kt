package com.example.hillclimbracing.activity

import kr.ac.tukorea.ge.spgp2026.a2dg.activity.BaseGameActivity
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import com.example.hillclimbracing.scene.TitleScene

class HillClimbActivity : BaseGameActivity() {
    override val drawsDebugGrid: Boolean = true
    override val drawsDebugInfo: Boolean = true
    override val drawsFpsGraph: Boolean = false

    override fun createRootScene(gctx: GameContext): Scene {
        return TitleScene(gctx)
    }

}