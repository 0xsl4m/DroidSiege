package com.droidsiege.core

object ChallengeRegistry {
    private val registeredCategories = mutableListOf<CategoryMeta>()
    private val registeredChallenges = mutableListOf<Challenge>()

    val categories: List<CategoryMeta>
        get() = registeredCategories.toList()

    val challenges: List<Challenge>
        get() = registeredChallenges.toList()

    fun registerAll(vararg contributors: CategoryContributor) {
        if (registeredChallenges.isNotEmpty()) return
        contributors.forEach { contributor -> register(contributor.category, contributor.challenges) }
    }

    fun register(
        category: CategoryMeta,
        contributions: List<Challenge>,
    ) {
        registeredCategories += category
        registeredChallenges += contributions
    }

    fun categoryById(id: String): CategoryMeta? = registeredCategories.firstOrNull { it.id == id }

    fun challengesByCategory(categoryId: String): List<Challenge> =
        registeredChallenges
            .filter { it.id.category == categoryId }
            .sortedBy { it.id.level.ordinal }

    fun challengesInCategories(categoryIds: Collection<String>): List<Challenge> =
        categoryIds.flatMap { challengesByCategory(it) }.sortedBy { it.id.level.ordinal }

    fun challengeByKey(key: String): Challenge? = registeredChallenges.firstOrNull { it.id.key == key }
}
