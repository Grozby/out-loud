from functools import cache

from tqdm import tqdm


def explore(towel_to_compose: str, towel_patterns: list[str]) -> int:
    @cache
    def dfs(t: str):
        if len(t) == 0:
            return 1

        total = 0
        for tp in towel_patterns:
            if t.startswith(tp):
                total += dfs(t[len(tp) :])
        return total

    return dfs(towel_to_compose)


if __name__ == "__main__":
    with open("data/day19") as f:
        towel_patterns = f.readline().strip().split(", ")
        f.readline()
        towel_to_compose = []
        while line := f.readline().strip():
            towel_to_compose.append(line)

    cost_per_towel_to_compose = [
        explore(towel_to_compose=towel, towel_patterns=towel_patterns)
        for towel in tqdm(towel_to_compose)
    ]
    print("Solution 1:", len([c for c in cost_per_towel_to_compose if c != 0]))
    print("Solution 2:", sum(c for c in cost_per_towel_to_compose))
