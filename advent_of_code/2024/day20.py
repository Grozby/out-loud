class Solution:
    def __init__(self):
        self.racetrack = {}
        i = 0
        with open("data/day20") as f:
            while line := f.readline().strip():
                if "S" in line:
                    self.start = (i, line.index("S"))
                self.racetrack |= {(i, j): c for j, c in enumerate(line)}
                i += 1

    def is_goal(self, p: tuple[int, int]) -> int:
        return self.racetrack[p] == "E"

    def base_solution(self):
        solution = [
            self.start,
            next(np for np in self.adj_pos(self.start) if self.racetrack[np] != "#"),
        ]
        while not self.is_goal((p := solution[-1])):
            solution.append(
                next(
                    np
                    for np in self.adj_pos(p)
                    if self.racetrack[np] != "#" and np != solution[-2]
                )
            )
        return solution

    def adj_pos(self, position: tuple[int, int]):
        directions = [(1, 0), (-1, 0), (0, -1), (0, 1)]
        for dx, dy in directions:
            next_position = (position[0] + dx, position[1] + dy)
            yield next_position

    def explore_with_cheat(
        self,
        base_solution: list[tuple[int, int]],
        cheat_steps: int,
    ) -> int:
        count = 0
        for i, p1 in enumerate(base_solution):
            for j, p2 in enumerate(base_solution[i + 1:], start=i + 1):
                distance = self.distance(p1, p2)
                if j - i > cheat_steps >= distance and j - i - distance >= 100:
                    count += 1
        return count

    def distance(self, p1: tuple[int, int], p2: tuple[int, int]) -> int:
        return abs(p1[0] - p2[0]) + abs(p1[1] - p2[1])


if __name__ == "__main__":
    sol = Solution()
    base_solution = sol.base_solution()

    print("Solution 1:", sol.explore_with_cheat(base_solution, cheat_steps=2))
    print("Solution 2:", sol.explore_with_cheat(base_solution, cheat_steps=20))
