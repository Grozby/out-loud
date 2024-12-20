import copy
import itertools

from typing_extensions import Self


class SimulateRobotMovement:
    DIRECTIONS = {
        ">": (0, 1),
        "<": (0, -1),
        "^": (-1, 0),
        "v": (1, 0),
    }

    def __init__(self):
        warehouse_map, robot_position, robot_movements = self.parse_input()
        self._warehouse_size = (len(warehouse_map), len(warehouse_map[0]))
        self.warehouse = self.prepare_warehouse_map(warehouse_map)
        self.robot_position = robot_position
        self.robot_movements = robot_movements

    def parse_input(self) -> tuple:
        robot_position = None
        warehouse_map, robot_movements = [], []
        with open("data/day15") as f:
            while (line := f.readline().strip()) != "":
                if "@" in line:
                    robot_position = (len(warehouse_map), line.index("@"))
                    line = line.replace("@", ".")
                warehouse_map.append(line)

            while line := f.readline().strip():
                robot_movements.extend(list(line))

        return (
            warehouse_map,
            robot_position,
            robot_movements,
        )

    def prepare_warehouse_map(self, warehouse_map: list[str]):
        return {
            (i, j): warehouse_map[i][j]
            for i, j in itertools.product(
                range(len(warehouse_map)), range(len(warehouse_map[0]))
            )
        }

    def is_wall(self, p: tuple[int, int]) -> bool:
        return self.warehouse[p] == "#"

    def is_box(self, p: tuple[int, int]) -> bool:
        return self.warehouse[p] == "O"

    def is_not_colliding(self, p: tuple[int, int]) -> bool:
        return (not self.is_wall(p)) and (not self.is_box(p))

    def check_box_collision(self, p: tuple[int, int], d: tuple[int, int]) -> bool:
        start_pos = p
        while self.is_box(p):
            p = p[0] + d[0], p[1] + d[1]

        if self.is_not_colliding(p):
            while start_pos[0] != p[0] or start_pos[1] != p[1]:
                t = (p[0] - d[0], p[1] - d[1])
                self.warehouse[p], self.warehouse[t] = (
                    self.warehouse[t],
                    self.warehouse[p],
                )
                p = t
            return True

        return False

    def run_simulation(self) -> Self:
        for movement in self.robot_movements:
            direction = self.DIRECTIONS[movement]
            new_position = (
                self.robot_position[0] + direction[0],
                self.robot_position[1] + direction[1],
            )
            if self.is_wall(new_position):
                continue
            elif self.is_box(new_position):
                did_move_boxes = self.check_box_collision(new_position, direction)
                if did_move_boxes:
                    self.robot_position = new_position
            else:
                self.robot_position = new_position
        self.show_map()
        return self

    def show_map(self):
        w_copy = copy.deepcopy(self.warehouse)
        w_copy[(self.robot_position[0], self.robot_position[1])] = "@"

        print(
            "\n".join(
                "".join(w_copy[(i, j)] for j in range(self._warehouse_size[1]))
                for i in range(self._warehouse_size[0])
            )
        )

    def score(self) -> int:
        return sum(
            i * 100 + j for (i, j), v in self.warehouse.items() if self.is_box((i, j))
        )


class SimulateRobotMovementPart2(SimulateRobotMovement):
    def __init__(self):
        super().__init__()
        self._warehouse_size = (
            self._warehouse_size[0],
            self._warehouse_size[1] * 2 - 1,
        )
        self.robot_position = self.robot_position[0], self.robot_position[1] * 2

    def prepare_warehouse_map(self, warehouse_map: list[str]) -> dict[tuple, str]:
        new_warehouse_map = {}
        for i, w in enumerate(warehouse_map):
            w = w.replace("#", "##").replace("O", "[]").replace(".", "..")
            for j, val in enumerate(w):
                new_warehouse_map[(i, j)] = val
        return new_warehouse_map

    def is_box(self, p: tuple[int, int]) -> bool:
        return self.is_left_box(p) or self.is_right_box(p)

    def is_left_box(self, p: tuple[int, int]) -> bool:
        return self.warehouse[p] == "["

    def is_right_box(self, p: tuple[int, int]) -> bool:
        return self.warehouse[p] == "]"

    def check_box_collision(self, p: tuple[int, int], d: tuple[int, int]) -> bool:
        if d[0] == 0:
            return super().check_box_collision(p, d)

        def _check_clear_path(pl: tuple[int, int], pr: tuple[int, int]) -> bool:
            next_pl, next_pr = (pl[0] + d[0], pl[1]), (pr[0] + d[0], pr[1])
            if self.is_wall(next_pl) or self.is_wall(next_pr):
                return False
            if self.is_left_box(next_pl) and self.is_right_box(next_pr):
                return _check_clear_path(pl=next_pl, pr=next_pr)

            left_side = (
                _check_clear_path(pl=(next_pl[0], next_pl[1] - 1), pr=next_pl)
                if self.is_right_box(next_pl)
                else self.is_not_colliding(next_pl)
            )
            right_side = (
                _check_clear_path(pl=next_pr, pr=(next_pr[0], next_pr[1] + 1))
                if self.is_left_box(next_pr)
                else self.is_not_colliding(next_pr)
            )
            return left_side and right_side

        def swap(pl: tuple[int, int], pr: tuple[int, int]):
            next_pl, next_pr = (pl[0] + d[0], pl[1]), (pr[0] + d[0], pr[1])
            if self.is_left_box(next_pl) and self.is_right_box(next_pr):
                swap(pl=next_pl, pr=next_pr)
            else:
                if self.is_right_box(next_pl):
                    swap(pl=(next_pl[0], next_pl[1] - 1), pr=next_pl)
                if self.is_left_box(next_pr):
                    swap(pl=next_pr, pr=(next_pr[0], next_pr[1] + 1))

            self.warehouse[next_pl], self.warehouse[pl] = (
                self.warehouse[pl],
                self.warehouse[next_pl],
            )
            self.warehouse[next_pr], self.warehouse[pr] = (
                self.warehouse[pr],
                self.warehouse[next_pr],
            )

        pl, pr = (p, (p[0], p[1] + 1)) if self.is_left_box(p) else ((p[0], p[1] - 1), p)
        can_move = _check_clear_path(pl=pl, pr=pr)
        if can_move:
            swap(pl=pl, pr=pr)

        return can_move

    def score(self) -> int:
        return sum(
            i * 100 + j
            for (i, j), v in self.warehouse.items()
            if self.is_left_box((i, j))
        )


if __name__ == "__main__":
    print("Solution 1:", SimulateRobotMovement().run_simulation().score())
    print("Solution 2:", SimulateRobotMovementPart2().run_simulation().score())
