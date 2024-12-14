import itertools


def solution(topological_map: list[list[int]]) -> tuple[int, int]:
    score, rating = 0, 0
    for i, j in itertools.product(
        range(len(topological_map)), range(len(topological_map[0]))
    ):
        if topological_map[i][j] != 0:
            continue
        score += compute_score(topological_map, position=(i, j), explored=set())
        rating += compute_rating(topological_map, position=(i, j))
    return score, rating


def compute_score(
    topological_map: list[list[int]],
    explored: set,
    position: tuple[int, int],
) -> int:
    explored.add(position)
    if topological_map[position[0]][position[1]] == 9:
        return 1

    return sum(
        [
            compute_score(topological_map, position=p, explored=explored)
            for p in [
                (position[0] - 1, position[1]),
                (position[0] + 1, position[1]),
                (position[0], position[1] - 1),
                (position[0], position[1] + 1),
            ]
            if is_inside(topological_map, p)
            and can_move(topological_map, start_pos=position, end_pos=p)
            and p not in explored
        ]
    )


def compute_rating(
    topological_map: list[list[int]],
    position: tuple[int, int],
) -> int:
    if topological_map[position[0]][position[1]] == 9:
        return 1
    possible_positions = [
        p
        for p in [
            (position[0] - 1, position[1]),
            (position[0] + 1, position[1]),
            (position[0], position[1] - 1),
            (position[0], position[1] + 1),
        ]
        if is_inside(topological_map, p)
        and can_move(topological_map, start_pos=position, end_pos=p)
    ]
    asd = sum(compute_rating(topological_map, position=p) for p in possible_positions)
    return asd


def is_inside(_map: list[list[int]], pos: tuple[int, int]) -> bool:
    return 0 <= pos[0] < len(_map) and 0 <= pos[1] < len(_map[0])


def can_move(
    _map: list[list[int]],
    start_pos: tuple[int, int],
    end_pos: tuple[int, int],
) -> bool:
    start_val = _map[start_pos[0]][start_pos[1]]
    end_val = _map[end_pos[0]][end_pos[1]]
    return (start_val + 1) == end_val


if __name__ == "__main__":
    topological_map = []
    with open("data/day10") as f:
        while line := f.readline().strip():
            topological_map.append(list(map(int, line)))

    score, rating = solution(topological_map)
    print("Solution 1:", score)
    print("Solution 2:", rating)
