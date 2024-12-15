import dataclasses
import re
from typing import NamedTuple

import numpy as np
from sympy import symbols, Eq, solve


class Position(NamedTuple):
    x: int
    y: int


@dataclasses.dataclass
class PrizeGame:
    button_a: Position
    button_b: Position
    prize_position: Position
    cost_a: int = 3
    cost_b: int = 1
    cap: float = 100

    def is_valid(self, c_a: int, c_b: int) -> bool:
        dx = c_a * self.button_a.x + self.button_b.x * c_b
        dy = c_a * self.button_a.y + self.button_b.y * c_b
        return dx == self.prize_position.x and dy == self.prize_position.y

    def get_cost(self, c_a: int, c_b: int) -> int:
        return (
            self.cost_a * c_a + self.cost_b * c_b
            if c_a <= self.cap and c_b <= self.cap
            else 0
        )


def parse_input() -> list[PrizeGame]:
    prize_games = []
    with open("data/day13") as f:
        line = "placeholder"
        while line != "":
            prize_games.append(
                PrizeGame(
                    button_a=get_x_y_coordinates(f.readline().strip()),
                    button_b=get_x_y_coordinates(f.readline().strip()),
                    prize_position=get_x_y_coordinates(f.readline().strip()),
                )
            )
            line = f.readline()

    return prize_games


def get_x_y_coordinates(s: str) -> Position:
    x, y = re.match(r".*: X[+=](\d+), Y[+=](\d+)", s).groups()
    return Position(x=int(x), y=int(y))


def solve_game(game: PrizeGame) -> int:
    c_a, c_b = symbols("c_a c_b", integer=True)

    eq1 = Eq(game.button_a.y * c_a + game.button_b.y * c_b, game.prize_position.y)
    eq2 = Eq(game.button_a.x * c_a + game.button_b.x * c_b, game.prize_position.x)

    solution = solve((eq1, eq2), (c_a, c_b))
    if c_a in solution and c_b in solution:
        return game.get_cost(solution[c_a], solution[c_b])
    return 0


def solve_game_2(game: PrizeGame) -> int:
    """
    Solve the game with Lagrange Multipliers defined as

    $$
    p_x = ba_x * c_a + bb_x * c_b \\
    p_y = ba_y * c_a + bb_y * c_b \\
    f(c_a, c_b) = c_a * 3 + c_b \\
    $$
    Constraints:
    $$
    g_1(c_a, c_b) = ba_x * c_a + bb_x * c_b - p_x = 0 \\
    g_2(c_a, c_b) = ba_y * c_a + bb_y * c_b - p_y = 0 \\
    $$

    Lagrangian:
    $$
    \mathcal{L}(c_a, c_b, \lambda_1, \lambda_2) =
    c_a * 3 + c_b + \lambda_1 * g_1(c_a, c_b) + \lambda_2 * g_2(c_a, c_b)
    $$

    Partial derivatives:
    $$
    \frac{\partial \mathcal{L}}{\partial c_a} = \lambda_1 * ba_x + \lambda_2 * ba_y = -3 \\
    \frac{\partial \mathcal{L}}{\partial c_b} = \lambda_1 * bb_x + \lambda_2 * bb_y = -1 \\
    \frac{\partial \mathcal{L}}{\partial \lambda_1} = ba_x * c_a + bb_x * c_b = p_x \\
    \frac{\partial \mathcal{L}}{\partial \lambda_2} = ba_y * c_a + bb_y * c_b = p_y
    $$

    Then, solve this system of equations.

    """
    x = np.array(
        [
            [0.0, 0.0, game.button_a.x, game.button_a.y],
            [0.0, 0.0, game.button_b.x, game.button_b.y],
            [game.button_a.x, game.button_b.x, 0.0, 0.0],
            [game.button_a.y, game.button_b.y, 0.0, 0.0],
        ]
    )
    y = np.array(
        [
            -game.cost_a,
            -game.cost_b,
            game.prize_position.x,
            game.prize_position.y,
        ]
    )
    c_a, c_b, *_ = np.linalg.solve(x, y)
    if all(np.allclose(c, c.round(), rtol=0, atol=1.0e-4) for c in [c_a, c_b]):
        c_a, c_b = int(c_a), int(c_b)
        return game.get_cost(c_a, c_b)
    return 0


if __name__ == "__main__":
    prize_games = parse_input()

    print("Solution 1:", sum(solve_game(game) for game in prize_games))
    for game in prize_games:
        game.prize_position = Position(
            x=game.prize_position.x + int(1e13),
            y=game.prize_position.y + int(1e13),
        )
        game.cap = float("Inf")
    print("Solution 2:", sum(solve_game(game) for game in prize_games))
