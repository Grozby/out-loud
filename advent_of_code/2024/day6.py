import itertools
from collections import defaultdict

import numpy as np
from tqdm import tqdm


def parse_input() -> tuple[np.ndarray, np.ndarray]:
    _map = []
    guard_position = None
    with open("data/day6") as f:
        while line := f.readline().strip():
            if "^" in line:
                guard_position = np.array([len(_map), line.index("^")])
                line = line.replace("^", ".")
            _map.append(list(line))

    assert guard_position is not None
    return np.array(_map), guard_position


def simulate(_map: np.ndarray, guard_position: np.ndarray) -> tuple[dict, bool]:
    directions = itertools.cycle(map(np.array, [(-1, 0), (0, 1), (1, 0), (0, -1)]))

    def is_inside_map(g) -> bool:
        return 0 <= g[0] < _map.shape[0] and 0 <= g[1] < _map.shape[1]

    direction = next(directions)
    explored_positions = defaultdict(set)
    explored_positions[tuple(guard_position)].add(tuple(direction))

    while is_inside_map(guard_position + direction):
        while (
            is_inside_map((new_position := guard_position + direction))
            and _map[new_position[0], new_position[1]] == "#"
        ):
            direction = next(directions)
        guard_position += direction

        if tuple(direction) in explored_positions[tuple(guard_position)]:
            return explored_positions, True
        explored_positions[tuple(guard_position)].add(tuple(direction))

    return explored_positions, False


def check_for_loops(
    _map: np.ndarray,
    guard_position: np.ndarray,
    positions_for_obstacles: dict,
) -> int:
    loop_count = 0
    for i, j in tqdm(positions_for_obstacles.keys()):
        previous_map_value = _map[i, j]
        if previous_map_value == "#":
            continue

        _map[i, j] = "#"
        _, is_loop = simulate(_map=_map, guard_position=guard_position.copy())
        loop_count += is_loop
        _map[i, j] = previous_map_value
    return loop_count


if __name__ == "__main__":
    _map, guard_position = parse_input()
    explored_positions, _ = simulate(_map=_map, guard_position=guard_position.copy())
    loop_count = check_for_loops(
        _map=_map,
        guard_position=guard_position,
        positions_for_obstacles=explored_positions,
    )
    print("Solution 1:", len(explored_positions))
    print("Solution 2:", loop_count)
