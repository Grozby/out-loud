import re
from typing import Iterable


def count_pattern(pattern: re.Pattern, word_search: list[list[str]]) -> int:
    return (
        count_pattern_horizontally(pattern=pattern, word_search=word_search)
        + count_pattern_vertically(pattern=pattern, word_search=word_search)
        + count_pattern_diagonally(pattern=pattern, word_search=word_search)
    )


def count_pattern_horizontally(
    pattern: re.Pattern,
    word_search: list[list[str]],
) -> int:
    return sum(
        len(pattern.findall("".join(w))) + len(pattern.findall("".join(w[::-1])))
        for w in word_search
    )


def count_pattern_vertically(
    pattern: re.Pattern,
    word_search: list[list[str]],
) -> int:
    return sum(
        len(pattern.findall("".join(w))) + len(pattern.findall("".join(w[::-1])))
        for w in return_vertical_strings(word_search)
    )


def return_vertical_strings(word_search: list[list[str]]) -> Iterable[str]:
    for i in range(len(word_search)):
        yield "".join(word_search[j][i] for j in range(len(word_search[i])))


def count_pattern_diagonally(pattern: re.Pattern, word_search: list[list[str]]) -> int:
    return sum(
        len(pattern.findall("".join(w))) + len(pattern.findall("".join(w[::-1])))
        for w in return_diagonal_strings(word_search)
    )


def return_diagonal_strings(word_search: list[list[str]]) -> Iterable[str]:
    for i in range(len(word_search) + len(word_search[0]) - 1):
        yield "".join(
            word_search[row][col]
            for row in range(len(word_search))
            for col in range(len(word_search[0]))
            if row + col == i
        )
    for i in range(-len(word_search) + 1, len(word_search[0])):
        yield "".join(
            word_search[row][col]
            for row in range(len(word_search))
            for col in range(len(word_search[0]))
            if row - col == i
        )


def count_x_mas_pattern(
    x_mas_patterns: list[list[list[str]]],
    word_search: list[list[str]],
) -> int:
    count = 0
    for i in range(len(word_search) - 2):
        for j in range(len(word_search[0]) - 2):
            sub_word_search = [w[j : j + 3] for w in word_search[i : i + 3]]
            count += sum(
                match_x_mas_pattern(
                    x_mas_pattern=pattern,
                    sub_word_search=sub_word_search,
                )
                for pattern in x_mas_patterns
            )

    return count


def rotate_pattern_clockwise(x_mas_pattern: list[list[str]]) -> list[list[str]]:
    transposed = [list(row) for row in zip(*x_mas_pattern)]
    return [row[::-1] for row in transposed]


def match_x_mas_pattern(
    x_mas_pattern: list[list[str]],
    sub_word_search: list[list[str]],
) -> bool:
    return (
        sub_word_search[0][0] == x_mas_pattern[0][0]
        and sub_word_search[2][0] == x_mas_pattern[2][0]
        and sub_word_search[1][1] == x_mas_pattern[1][1]
        and sub_word_search[0][2] == x_mas_pattern[0][2]
        and sub_word_search[2][2] == x_mas_pattern[2][2]
    )


if __name__ == "__main__":
    word_search = []
    with open("data/day4") as f:
        while line := f.readline():
            word_search.append(list(line.strip()))

    pattern = re.compile(r"XMAS")
    solution_1 = count_pattern(pattern, word_search)
    print(f"Part 1: {solution_1}")

    x_mas_pattern = [
        ["M", ".", "S"],
        [".", "A", "."],
        ["M", ".", "S"],
    ]
    x_mas_patterns = [x_mas_pattern]
    for i in range(3):
        x_mas_patterns.append(rotate_pattern_clockwise(x_mas_patterns[-1]))

    solution_2 = count_x_mas_pattern(
        x_mas_patterns=x_mas_patterns,
        word_search=word_search,
    )
    print(f"Part 2: {solution_2}")
