package org.cru.soularium.domain.content

data class Question(
    val number: Int,
    val requiredImageCount: Int,
    val promptKey: String,
    val selectionKey: String,
    val finalizingKey: String,
    val discussionKey: String,
) {
    init {
        require(number in 1..5) { "Question number must be 1..5, was $number" }
        require(requiredImageCount in 1..3) { "Required image count must be 1..3, was $requiredImageCount" }
    }
}
