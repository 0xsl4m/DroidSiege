package com.droidsiege.challenges.common

import com.droidsiege.core.Challenge
import com.droidsiege.core.ChallengeId
import com.droidsiege.core.Difficulty
import com.droidsiege.core.LearnContent

/**
 * Base for the phase-2 challenge tiers: carries the fixed boilerplate so each tier only
 * declares its identity, content and screen.
 */
abstract class TieredChallenge(
    category: String,
    slug: String,
    level: Difficulty,
    override val title: String,
    override val brief: String,
    override val owaspRefs: List<String>,
    override val hints: List<String>,
    override val flag: String,
    override val learn: LearnContent,
) : Challenge {
    override val id = ChallengeId(category = category, slug = slug, level = level)

    override fun validateFlag(input: String): Boolean = input.trim() == flag
}
