from functools import cache

direction_pad = {
    (0, 1): "^",
    (0, 2): "A",
    (1, 0): "<",
    (1, 1): "v",
    (1, 2): ">",
}
reverse_direction_pad = {v: k for k, v in direction_pad.items()}
digit_pad = {
    (0, 0): "7",
    (0, 1): "8",
    (0, 2): "9",
    (1, 0): "4",
    (1, 1): "5",
    (1, 2): "6",
    (2, 0): "1",
    (2, 1): "2",
    (2, 2): "3",
    (3, 1): "0",
    (3, 2): "A",
}
reverse_digit_pad = {v: k for k, v in digit_pad.items()}


@cache
def get_sequence(
    new_p: tuple[int, int], old_p: tuple[int, int], mode: str = "direction"
):
    pad = direction_pad if mode == "direction" else digit_pad
    dy, dx = new_p[0] - old_p[0], new_p[1] - old_p[1]

    up, dw = "^" * -dy, "v" * dy
    lf, rh = "<" * -dx, ">" * dx
    if (min(new_p[0], old_p[0]), min(new_p[1], old_p[1])) not in pad:
        return dw + rh + up + lf + "A"
    elif (max(new_p[0], old_p[0]), min(new_p[1], old_p[1])) not in pad:
        return up + rh + dw + lf + "A"
    else:
        return lf + dw + up + rh + "A"


@cache
def get_complexity_sequence(seq: str, iteration: int) -> int:
    if iteration == 0:
        return len(seq)

    complexity = 0
    pos = (0, 2)
    for c in seq:
        new_pos = reverse_direction_pad[c]
        complexity += get_complexity_sequence(
            seq=get_sequence(new_p=new_pos, old_p=pos, mode="direction"),
            iteration=iteration - 1,
        )
        pos = new_pos
    return complexity


def solve_code(code: str, n_robots: int):
    digit_pos = (3, 2)
    complexity = 0
    for c in code:
        new_digit_pos = reverse_digit_pad[c]
        seq = get_sequence(new_p=new_digit_pos, old_p=digit_pos, mode="digit")
        digit_pos = new_digit_pos
        complexity += get_complexity_sequence(seq, iteration=n_robots)

    return complexity * int(code[:-1])


if __name__ == "__main__":
    codes = []
    with open("data/day21") as f:
        while line := f.readline().strip():
            codes.append(line)
    print("Solution 1:", sum(solve_code(c, n_robots=2) for c in codes))
    print("Solution 2:", sum(solve_code(c, n_robots=25) for c in codes))
