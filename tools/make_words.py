"""Builds app/src/main/assets/words.json.

usage: python -I make_words.py <enable1.txt> <out words.json>

"words" is the curated list puzzles are drawn from. "alsoAccepted" holds every
dictionary word that is an anagram of a curated word, so Unscramble accepts any
real answer (e.g. "slate" for "stale"), not only the one it had in mind.
"""
import json
import sys
from collections import defaultdict

CURATED = """
book word read page note poem tale verb noun text line font quiz clue game play hint mind idea fact myth epic plot hero calm
story novel write spell speak learn teach study think quote rhyme verse prose essay title index paper print vowel comma colon
slang idiom voice sound pitch tempo music piano dance paint brush light shade color green amber coral olive lemon mango peach
grape apple berry melon bread toast honey sugar cream water river ocean beach shore coast storm cloud frost sunny earth stone
metal glass steel brick house table chair couch shelf clock watch phone radio train plane truck wheel motor robot laser pixel
mouse tiger zebra horse sheep eagle robin whale shark otter camel panda koala lemur smile laugh happy brave quiet quick sharp
smart fresh clean grand giant small young early night today month dream sleep heart brain comet
letter puzzle riddle answer search hidden wonder clever bright silver golden purple orange yellow violet indigo marker pencil
eraser folder binder school lesson reader writer author editor poetry comedy sonnet legend garden flower forest meadow valley
canyon desert island harbor bridge castle palace temple market window mirror candle pillow basket bottle button pocket jacket
ribbon cotton velvet rabbit monkey parrot turtle falcon walrus donkey spider butter cheese pepper ginger tomato potato carrot
banana cherry walnut almond coffee dinner planet rocket galaxy meteor breeze winter spring summer autumn friend family travel
voyage anchor guitar violin melody rhythm chorus dancer artist sketch canvas museum camera mobile number circle square phrase
sunset sailor
library grammar chapter meaning synonym antonym content context subject penguin dolphin giraffe leopard panther buffalo
hamster raccoon sparrow ostrich morning evening holiday weekend journey compass lantern thunder rainbow sunrise volcano
glacier prairie kitchen blanket cabinet curtain picture pattern texture crystal diamond emerald pyramid balloon bicycle
captain teacher student college science history physics biology algebra theatre concert trumpet whistle popcorn pancake
cupcake avocado coconut apricot mustard vanilla freedom justice courage harmony balance curious perfect amazing thought
mystery fantasy fiction express explain imagine believe achieve develop explore capture collect connect airport station
highway village country kingdom fortune victory
alphabet sentence language notebook keyboard elephant kangaroo mountain sunshine umbrella treasure champion daughter
birthday calendar triangle question sandwich hospital painting airplane computer bookmark envelope syllable discover carriage
""".split()


def main(dict_path, out_path):
    words = sorted(set(CURATED), key=lambda w: (len(w), w))
    assert all(w.isalpha() and w.islower() and 4 <= len(w) <= 8 for w in words)

    by_letters = defaultdict(set)
    with open(dict_path, encoding="ascii", errors="ignore") as f:
        for line in f:
            w = line.strip().lower()
            if w.isalpha() and w.isascii() and 4 <= len(w) <= 8:
                by_letters["".join(sorted(w))].add(w)

    curated = set(words)
    also = sorted(
        {a for w in words for a in by_letters["".join(sorted(w))]} - curated,
        key=lambda w: (len(w), w),
    )
    missing = [w for w in words if w not in by_letters["".join(sorted(w))]]

    with open(out_path, "w", encoding="utf-8", newline="\n") as f:
        json.dump({"version": 1, "words": words, "alsoAccepted": also}, f, separators=(",", ":"))
        f.write("\n")
    print(f"{len(words)} words, {len(also)} extra accepted anagrams")
    print("curated words not in dictionary:", missing)
    print("sample extras:", also[:25])


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
