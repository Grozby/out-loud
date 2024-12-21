import dataclasses
import heapq
from functools import reduce


@dataclasses.dataclass
class State:
    position: tuple[int, int]
    direction: tuple[int, int]
    cost: int
    heuristic: int
    previous_state: "State" = None
    lead_to_goal: bool = False

    def __lt__(self, other):
        return self.cost + self.heuristic < other.cost + other.heuristic

    def __eq__(self, other):
        if not isinstance(other, State):
            return NotImplemented
        return self.position == other.position and self.direction == other.direction

    def __hash__(self):
        return hash(self.position + self.direction)

    def get_positions(self) -> set[tuple[int, int]]:
        return self._get_positions(set())

    def _get_positions(self, positions: set[tuple[int, int]]) -> set[tuple[int, int]]:
        positions.add(self.position)
        if self.previous_state is not None:
            positions.update(self.previous_state._get_positions(positions))
        return positions


class AStar:
    def __init__(self):
        with open("data/day16") as f:
            i = 0
            self.maze = {}
            while line := f.readline().strip():
                if "S" in line:
                    self.start = (i, line.index("S"))
                if "E" in line:
                    self.end = (i, line.index("E"))
                self.maze |= {
                    (i, j): "." if c != "#" else "#" for j, c in enumerate(line)
                }
                i += 1

    def heuristic(self, p: tuple[int, int]) -> int:
        dx = abs(p[0] - self.end[0])
        dy = abs(p[1] - self.end[1])
        d_turns = dx > 0 + dy > 0
        return dx + dy + d_turns * 1000

    def explore(self) -> list[State]:
        solutions = []
        explored = {}
        frontier = [State(position=self.start, direction=(0, 1), cost=0, heuristic=-1)]
        while len(frontier) != 0:
            current_state = heapq.heappop(frontier)
            if (
                current_state.position not in self.maze
                or self.maze[current_state.position] == "#"
                or (
                    current_state in explored
                    and current_state.cost > explored[current_state]
                )
            ):
                continue
            explored[current_state] = current_state.cost

            if current_state.heuristic == 0:
                solutions.append(current_state)
                continue

            directions = (
                [(1, 0), (-1, 0)]
                if current_state.direction[0] == 0
                else [(0, -1), (0, 1)]
            )
            for direction in directions:
                state = State(
                    position=current_state.position,
                    direction=direction,
                    cost=current_state.cost + 1000,
                    heuristic=current_state.heuristic,
                    previous_state=current_state,
                )
                heapq.heappush(frontier, state)

            next_position = (
                current_state.position[0] + current_state.direction[0],
                current_state.position[1] + current_state.direction[1],
            )
            if next_position in self.maze and self.maze[next_position] == ".":
                state = State(
                    position=next_position,
                    direction=current_state.direction,
                    cost=current_state.cost + 1,
                    heuristic=self.heuristic(next_position),
                    previous_state=current_state,
                )
                heapq.heappush(frontier, state)

        return solutions


if __name__ == "__main__":
    solutions = AStar().explore()
    solution_part_1 = min(solutions, key=lambda s: s.cost).cost
    solution_part_2 = len(
        reduce(
            lambda a, b: a | b,
            [s.get_positions() for s in solutions if s.cost == solution_part_1],
        )
    )
    print("Solution 1:", solution_part_1)
    print("Solution 2:", solution_part_2)
