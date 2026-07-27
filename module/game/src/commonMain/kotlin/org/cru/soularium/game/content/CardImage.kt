package org.cru.soularium.game.content

data class CardImage(val id: Int) {
    init {
        require(id in 1..50) { "Card id must be 1..50, was $id" }
    }
}
