def is_equation_valid(test_value: int, current: int, remaining: list[int]):
    if len(remaining) == 0:
        if test_value == current:
            return True
        return False

    return is_equation_valid(
        test_value=test_value,
        current=current + remaining[0],
        remaining=remaining[1:],
    ) or is_equation_valid(
        test_value=test_value,
        current=current * remaining[0],
        remaining=remaining[1:],
    )


def is_equation_valid_v2(test_value: int, current: int, remaining: list[int]):
    if len(remaining) == 0:
        if test_value == current:
            return True
        return False

    return (
        is_equation_valid_v2(
            test_value=test_value,
            current=current + remaining[0],
            remaining=remaining[1:],
        )
        or is_equation_valid_v2(
            test_value=test_value,
            current=current * remaining[0],
            remaining=remaining[1:],
        )
        or is_equation_valid_v2(
            test_value=test_value,
            current=current * (10 ** len(str(remaining[0]))) + remaining[0],
            remaining=remaining[1:],
        )
    )


def solution_1(operations: list[tuple[int, list[int]]]) -> int:
    return sum(
        test_value
        for (test_value, equation_values) in operations
        if is_equation_valid(
            test_value=test_value,
            current=equation_values[0],
            remaining=equation_values[1:],
        )
    )


def solution_2(operations: list[tuple[int, list[int]]]) -> int:
    return sum(
        test_value
        for (test_value, equation_values) in operations
        if is_equation_valid_v2(
            test_value=test_value,
            current=equation_values[0],
            remaining=equation_values[1:],
        )
    )


if __name__ == "__main__":
    operations = []
    with open("data/day7") as f:
        while line := f.readline().strip():
            test_value, equation_values = line.split(": ", maxsplit=1)
            operations.append(
                (int(test_value), list(map(int, equation_values.split(" "))))
            )

    print("Solution 1:", solution_1(operations))
    print("Solution 2:", solution_2(operations))
