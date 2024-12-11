import re


def parse_multiplication(x: str) -> int:
    x1, x2 = x[4:-1].split(",")
    return int(x1) * int(x2)


def select_multiplication(operations: list) -> list:
    new_multiplications = []
    keep_multiplication = True

    for mul in operations:
        match mul:
            case "do()":
                keep_multiplication = True
            case "don't()":
                keep_multiplication = False
            case _:
                if keep_multiplication:
                    new_multiplications.append(mul)
    return new_multiplications


if __name__ == "__main__":
    program_memory = ""
    with open("data/day3") as f:
        while line := f.readline():
            program_memory += line.strip()

    pattern_1 = re.compile(r"(mul\(\d+,\d+\))")
    solution_1 = sum(parse_multiplication(x) for x in pattern_1.findall(program_memory))
    print(f"Part 1: {solution_1}")

    pattern_2 = re.compile(r"(mul\(\d+,\d+\)|do\(\)|don\'t\(\))")
    solution_2 = sum(
        parse_multiplication(x)
        for x in select_multiplication(pattern_2.findall(program_memory))
    )
    print(f"Part 2: {solution_2}")
