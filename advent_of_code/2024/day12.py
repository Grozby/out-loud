import itertools


def solution(garden_plot: list[list[str]]) -> tuple[int, int]:
    explored = {}
    price_solution_1, price_solution_2 = 0, 0
    for i, j in itertools.product(range(len(garden_plot)), range(len(garden_plot[0]))):
        if (i, j) in explored:
            continue
        new_explored = fence_price(
            _map=garden_plot,
            pos=(i, j),
            explored={},
            plot_type=garden_plot[i][j],
        )
        price_solution_1 += len(new_explored) * sum(map(len, new_explored.values()))
        explored |= new_explored
        price_solution_2 += len(new_explored) * fence_sides(new_explored)

    return price_solution_1, price_solution_2


def fence_price(
    _map: list[list[str]],
    pos: tuple[int, int],
    explored: dict[tuple, set],
    plot_type: str,
) -> dict[tuple, set]:
    possible_positions = []
    explored[pos] = set()
    for dx, dy in all_directions():
        p = (pos[0] + dx, pos[1] + dy)
        if is_inside(_map, p) and _map[p[0]][p[1]] == plot_type:
            possible_positions.append(p)
        else:
            explored[pos].add((dx, dy))

    for p in possible_positions:
        if p not in explored:
            fence_price(_map, pos=p, explored=explored, plot_type=plot_type)

    return explored


def fence_sides(explored: dict[tuple, set]) -> int:
    sides = 0
    while explored:
        pos, directions = explored.popitem()

        for direction in directions:
            opposite_directions = (
                [(0, -1), (0, +1)] if direction[0] != 0 else [(-1, 0), (1, 0)]
            )
            for dx, dy in opposite_directions:
                p = (pos[0], pos[1])
                while (
                    p := (p[0] + dx, p[1] + dy)
                ) in explored and direction in explored[p]:
                    explored[p].remove(direction)
                    if len(explored[p]) == 0:
                        explored.pop(p)
            sides += 1

    return sides


def is_inside(_map: list[list[str]], pos: tuple[int, int]) -> bool:
    return 0 <= pos[0] < len(_map) and 0 <= pos[1] < len(_map[0])


def all_directions() -> list[tuple[int, int]]:
    return [
        (-1, 0),
        (+1, 0),
        (0, -1),
        (0, +1),
    ]


if __name__ == "__main__":
    garden_plot = []
    with open("data/day12") as f:
        while line := f.readline().strip():
            garden_plot.append(list(line))

    solution_1, solution_2 = solution(garden_plot)
    print("Solution 1:", solution_1)
    print("Solution 2:", solution_2)
