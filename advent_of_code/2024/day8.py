import itertools
from collections import defaultdict


def parse_input() -> tuple[dict, tuple[int, int]]:
    antennas_positions = defaultdict(list)
    with open("data/day8") as f:
        row = 0
        while line := f.readline().strip():
            for col, c in enumerate(line):
                if c == ".":
                    continue
                antennas_positions[c].append((row, col))
            row += 1
    map_dimension = (row, row)
    return antennas_positions, map_dimension


def is_inside_map(pos: tuple[int, int], map_dimension: tuple[int, int]) -> bool:
    return 0 <= pos[0] < map_dimension[0] and 0 <= pos[1] < map_dimension[1]


def solution_1(
    antennas_positions: dict,
    map_dimension: tuple[int, int],
) -> set[tuple[int, int]]:
    antinodes_positions = set()

    for antenna, positions in antennas_positions.items():
        for pos1, pos2 in itertools.permutations(positions, 2):
            difference_x = pos1[0] - pos2[0]
            difference_y = pos1[1] - pos2[1]

            antinode_pos = (pos1[0] + difference_x, pos1[1] + difference_y)
            if is_inside_map(antinode_pos, map_dimension):
                antinodes_positions.add(antinode_pos)

    return antinodes_positions


def solution_2(
    antennas_positions: dict,
    map_dimension: tuple[int, int],
) -> set[tuple[int, int]]:
    antinodes_positions = set()

    for antenna, positions in antennas_positions.items():
        for pos1, pos2 in itertools.permutations(positions, 2):
            difference_x = pos1[0] - pos2[0]
            difference_y = pos1[1] - pos2[1]

            for dx, dy in [
                (difference_x, difference_y),
                (-difference_x, -difference_y),
            ]:
                antinode_pos = (pos1[0] + dx, pos1[1] + dy)
                while is_inside_map(antinode_pos, map_dimension):
                    antinodes_positions.add(antinode_pos)
                    antinode_pos = (antinode_pos[0] + dx, antinode_pos[1] + dy)

    return antinodes_positions


if __name__ == "__main__":
    antennas_positions, map_dimension = parse_input()
    unique_antennas_positions = set(
        itertools.chain.from_iterable(antennas_positions.values())
    )

    antinodes_positions = solution_1(antennas_positions, map_dimension)
    antinodes_positions_unbounded = solution_2(antennas_positions, map_dimension)
    print("Solution 1:", len(antinodes_positions))
    print("Solution 2:", len(antinodes_positions_unbounded))
