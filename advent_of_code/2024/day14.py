import dataclasses
import math
import re
from collections import defaultdict
from functools import reduce
from math import gcd
from typing import NamedTuple

import numpy as np
from scipy.ndimage import label
from tqdm import tqdm


class Position(NamedTuple):
    x: int
    y: int


class Velocity(NamedTuple):
    dx: int
    dy: int


class MapDim(NamedTuple):
    width: int
    height: int


@dataclasses.dataclass
class Robot:
    position: Position
    velocity: Velocity


def parse_input() -> list[Robot]:
    robots = []
    with open("data/day14") as f:
        while line := f.readline().strip():
            robots.append(get_robot(line))

    return robots


def get_robot(s: str) -> Robot:
    x, y, dx, dy = re.match(r"p=(-?\d+),(-?\d+) v=(-?\d+),(-?\d+)", s).groups()
    return Robot(
        position=Position(x=int(x), y=int(y)),
        velocity=Velocity(dx=int(dx), dy=int(dy)),
    )


def move_robot(robot: Robot, seconds: int, _map: MapDim):
    new_position = Position(
        x=(robot.position.x + seconds * robot.velocity.dx) % _map.width,
        y=(robot.position.y + seconds * robot.velocity.dy) % _map.height,
    )
    robot.position = new_position


def solution_1(robots: list[Robot], seconds: int, _map: MapDim):
    quadrants = defaultdict(int)
    for robot in robots:
        move_robot(robot, seconds=seconds, _map=_map)
        if robot.position.x == (_map.width // 2) or robot.position.y == (
            _map.height // 2
        ):
            continue
        quadrants[
            (robot.position.x < (_map.width // 2)),
            (robot.position.y < (_map.height // 2)),
        ] += 1

    return math.prod(quadrants.values())


def solution_2(robots: list[Robot], _map: MapDim):
    robot_cycles = []

    for robot in robots:
        robot_cycle = lcm(
            _map.width // gcd(_map.width, robot.velocity.dx),
            _map.height // gcd(_map.height, robot.velocity.dy),
        )
        robot_cycles.append(robot_cycle)

    cycle = reduce(lcm, robot_cycles)

    n_clusters = []
    for _ in tqdm(range(cycle)):
        n_clusters.append(find_clusters(robots, _map))
        for robot in robots:
            move_robot(robot, seconds=1, _map=_map)

    step_with_closest_points, _ = min(enumerate(n_clusters), key=lambda x: x[1])
    show_map(step_with_closest_points)
    return step_with_closest_points


def find_clusters(
    robots: list[Robot],
    _map: MapDim,
) -> int:
    temp = np.zeros((_map.width, _map.height), dtype=int)
    for robot in robots:
        temp[robot.position.x][robot.position.y] = 1
    _, n_clusters = label(temp, structure=np.ones((3, 3), dtype=int))
    return n_clusters


def show_map(seconds: int):
    robots = parse_input()
    for robot in robots:
        move_robot(robot, seconds, _map=_map)
    output = [["."] * _map[0] for _ in range(_map[1])]
    for robot in robots:
        output[robot.position.y][robot.position.x] = "#"

    print("\n".join("".join(o) for o in output))


def lcm(a: int, b: int) -> int:
    return abs(a * b) // gcd(a, b)


if __name__ == "__main__":
    _map = MapDim(width=101, height=103)
    print("Solution 1:", solution_1(robots=parse_input(), seconds=100, _map=_map))
    print("Solution 2:", solution_2(robots=parse_input(), _map=_map))
