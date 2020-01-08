package com.oplw.common.base

import android.animation.Animator
import android.view.animation.Animation

/**
 *
 *   @author opLW
 *   @date  2019/7/3
 */
abstract class BaseAnimatorListener: Animator.AnimatorListener {
    override fun onAnimationRepeat(animation: Animator?) {

    }

    override fun onAnimationCancel(animation: Animator?) {

    }

    override fun onAnimationStart(animation: Animator?) {

    }
}

abstract class BaseAnimationListener: Animation.AnimationListener {
    override fun onAnimationRepeat(animation: Animation?) {

    }

    override fun onAnimationStart(animation: Animation?) {

    }
}