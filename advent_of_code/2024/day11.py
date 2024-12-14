import math
from collections import defaultdict


def solution(stones: list[int], n_blinks: int) -> int:
    stones_map = defaultdict(int)
    for s in stones:
        stones_map[s] += 1

    for _ in range(n_blinks):
        new_stones_map = defaultdict(int)
        for stone, count in stones_map.items():
            if stone == 0:
                new_stones_map[1] += count
            elif (digits := count_digits(stone)) % 2 == 0:
                new_stones_map[stone // (10 ** (digits / 2))] += count
                new_stones_map[stone % (10 ** (digits / 2))] += count
            else:
                new_stones_map[stone * 2024] += count
        stones_map = new_stones_map
    return sum(stones_map.values())


def count_digits(n):
    if n == 0:
        return 1
    return int(math.log10(abs(n))) + 1


if __name__ == "__main__":
    with open("data/day11") as f:
        stones = list(map(int, f.readline().strip().split(" ")))

    print("Solution 1:", solution(stones, n_blinks=25))
    print("Solution 2:", solution(stones, n_blinks=75))
