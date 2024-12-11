from collections import Counter

if __name__ == "__main__":
    location_ids_left, location_ids_right = [], []
    with open("data/day1") as f:
        while line := f.readline():
            left_id, *_, right_id = line.rstrip().split(" ")
            location_ids_left.append(int(left_id))
            location_ids_right.append(int(right_id))

    location_ids_left.sort()
    location_ids_right.sort()

    solution_1 = sum(abs(x - y) for x, y in zip(location_ids_left, location_ids_right))
    print(f"Part 1: {solution_1}")


    location_ids_right_unique = Counter(location_ids_right)
    solution_2 = sum(location_ids_right_unique.get(x, 0) * x for x in location_ids_left)
    print(f"Part 2: {solution_2}")
