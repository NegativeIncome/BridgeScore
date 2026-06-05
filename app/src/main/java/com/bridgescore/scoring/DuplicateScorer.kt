package com.bridgescore.scoring

import com.bridgescore.data.model.Doubled
import com.bridgescore.data.model.Suit
import com.bridgescore.data.model.Vulnerability

/**
 * Computes the duplicate bridge score from the declarer's perspective.
 * Returns a positive number if the contract is made (NS side positive if NS is declarer,
 * or positive for declaring side). Caller adjusts sign for EW.
 *
 * tricksMade = total tricks won by declarer (0-13)
 * level = contract level (1-7)
 * Returns score from declaring side's perspective (positive = made, negative = down).
 */
fun computeScore(
    level: Int,
    suit: Suit,
    doubled: Doubled,
    tricksMade: Int,
    vulnerable: Boolean
): Int {
    val tricksNeeded = level + 6
    val delta = tricksMade - tricksNeeded  // positive = overtricks, negative = undertricks

    return if (delta >= 0) {
        madeScore(level, suit, doubled, delta, vulnerable)
    } else {
        penaltyScore(delta, doubled, vulnerable)  // negative
    }
}

private fun trickValue(suit: Suit): Int = when (suit) {
    Suit.CLUBS, Suit.DIAMONDS -> 20
    Suit.HEARTS, Suit.SPADES -> 30
    Suit.NOTRUMP -> 30  // subsequent tricks; first is handled separately
}

private fun baseTrickScore(level: Int, suit: Suit): Int {
    val perTrick = trickValue(suit)
    return if (suit == Suit.NOTRUMP) 40 + (level - 1) * 30 else level * perTrick
}

private fun madeScore(
    level: Int,
    suit: Suit,
    doubled: Doubled,
    overtricks: Int,
    vulnerable: Boolean
): Int {
    val baseScore = baseTrickScore(level, suit)
    val contractScore = when (doubled) {
        Doubled.NONE      -> baseScore
        Doubled.DOUBLED   -> baseScore * 2
        Doubled.REDOUBLED -> baseScore * 4
    }

    val isGame = contractScore >= 100
    val gameBonus = when {
        level == 7 -> if (vulnerable) 1500 else 1000    // grand slam bonus (includes game)
        level == 6 -> if (vulnerable) 750 else 500       // small slam bonus (includes game)
        isGame     -> if (vulnerable) 500 else 300
        else       -> 50                                  // part score
    }

    val insult = when (doubled) {
        Doubled.NONE      -> 0
        Doubled.DOUBLED   -> 50
        Doubled.REDOUBLED -> 100
    }

    val overtrickValue = when (doubled) {
        Doubled.NONE      -> overtricks * trickValue(suit).let { if (suit == Suit.NOTRUMP && overtricks == 0) 0 else it }
        Doubled.DOUBLED   -> overtricks * if (vulnerable) 200 else 100
        Doubled.REDOUBLED -> overtricks * if (vulnerable) 400 else 200
    }
    val overtrickScore = when (doubled) {
        Doubled.NONE -> overtricks * if (suit == Suit.NOTRUMP) 30 else trickValue(suit)
        Doubled.DOUBLED -> overtricks * if (vulnerable) 200 else 100
        Doubled.REDOUBLED -> overtricks * if (vulnerable) 400 else 200
    }

    return contractScore + gameBonus + insult + overtrickScore
}

private fun penaltyScore(
    delta: Int,   // negative (e.g. -1 = down one)
    doubled: Doubled,
    vulnerable: Boolean
): Int {
    val down = -delta  // positive count of undertricks
    val penalty = when (doubled) {
        Doubled.NONE -> down * if (vulnerable) 100 else 50

        Doubled.DOUBLED -> if (vulnerable) {
            when {
                down == 1 -> 200
                else      -> 200 + (down - 1) * 300
            }
        } else {
            when {
                down == 1 -> 100
                down <= 3 -> 100 + (down - 1) * 200
                else      -> 100 + 2 * 200 + (down - 3) * 300
            }
        }

        Doubled.REDOUBLED -> if (vulnerable) {
            when {
                down == 1 -> 400
                else      -> 400 + (down - 1) * 600
            }
        } else {
            when {
                down == 1 -> 200
                down <= 3 -> 200 + (down - 1) * 400
                else      -> 200 + 2 * 400 + (down - 3) * 600
            }
        }
    }
    return -penalty
}
