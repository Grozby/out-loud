import itertools

if __name__ == "__main__":
    locks, keys = [], []
    with open("data/day25") as f:
        new_line = "\n"
        while new_line == "\n":
            line = f.readline().strip()
            element = [
                sum(c == "#" for c in col)
                for col in zip(*[list(f.readline().strip()) for _ in range(5)])
            ]
            if all(c == "#" for c in line):
                locks.append(element)
            else:
                keys.append(element)
            f.readline()
            new_line = f.readline()
    overlapping = sum(
        all(k + l <= 5 for k, l in zip(key, lock))
        for key, lock in itertools.product(keys, locks)
    )

    print("Solution 1:", overlapping)
