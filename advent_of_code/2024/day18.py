import dataclasses
import heapq
import itertools


@dataclasses.dataclass
class State:
    position: tuple[int, int]
    cost: int
    heuristic: int
    previous_state: "State" = None

    def __lt__(self, other):
        return self.cost + self.heuristic < other.cost + other.heuristic

    def get_positions(self) -> set[tuple[int, int]]:
        return self._get_positions(set())

    def _get_positions(self, positions: set[tuple[int, int]]) -> set[tuple[int, int]]:
        positions.add(self.position)
        if self.previous_state is not None:
            positions.update(self.previous_state._get_positions(positions))
        return positions


class AStar:
    def __init__(self, size: tuple[int, int]):
        self.size = size
        self.positions = []
        with open("data/day18") as f:
            while line := f.readline().strip():
                i, j = line.split(",", maxsplit=1)
                self.positions.append((int(i), int(j)))

    def _initialize_memory_space(
        self, positions: list[tuple[int, int]]
    ) -> dict[tuple[int, int], str]:
        memory_space = {
            (i, j): "."
            for i, j in itertools.product(
                range(self.size[0] + 1), range(self.size[1] + 1)
            )
        }
        for x, y in positions:
            memory_space[(x, y)] = "#"
        return memory_space

    def heuristic(self, p: tuple[int, int]) -> int:
        return abs(p[0] - self.size[0]) + abs(p[1] - self.size[1])

    def run_simulations(self, n_positions: int):
        memory_space = self._initialize_memory_space(self.positions[:n_positions])
        initial_state = self.explore(memory_space=memory_space)
        current_path = initial_state.get_positions()

        while n_positions < len(self.positions):
            n_positions += 1
            memory_space[self.positions[n_positions]] = "#"
            if self.positions[n_positions] not in current_path:
                continue

            try:
                new_state = self.explore(memory_space=memory_space)
                current_path = new_state.get_positions()
            except:
                break
        return initial_state, self.positions[n_positions]

    def explore(
        self,
        memory_space: dict[tuple[int, int], str],
    ) -> State:
        explored = {}
        frontier = [State(position=(0, 0), cost=0, heuristic=1)]
        while len(frontier) != 0:
            current_state = heapq.heappop(frontier)
            if (
                current_state.position not in memory_space
                or memory_space[current_state.position] == "#"
                or (
                    current_state.position in explored
                    and current_state.cost > explored[current_state.position]
                )
            ):
                continue
            explored[current_state.position] = current_state.cost

            if current_state.heuristic == 0:
                return current_state

            directions = [(1, 0), (-1, 0), (0, -1), (0, 1)]
            for dx, dy in directions:
                next_position = (
                    current_state.position[0] + dx,
                    current_state.position[1] + dy,
                )
                state = State(
                    position=next_position,
                    cost=current_state.cost + 1,
                    heuristic=self.heuristic(next_position),
                    previous_state=current_state,
                )

                if (
                    state.position not in explored
                    or explored[state.position] < state.cost
                ):
                    heapq.heappush(frontier, state)

        raise RuntimeError

    def show_map(self, memory_space):
        return "\n".join(
            "".join(memory_space[(i, j)] for j in range(self.size[1]))
            for i in range(self.size[0])
        )


if __name__ == "__main__":
    first_solution, byte_position = AStar(size=(70, 70)).run_simulations(
        n_positions=1024
    )

    print("Solution 1:", first_solution.cost)
    print("Solution 2:", ",".join(map(str, byte_position)))
