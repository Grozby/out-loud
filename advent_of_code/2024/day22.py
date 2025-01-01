from collections import defaultdict


def generate_new_secret(secret: int):
    for _ in range(2000):
        secret = next_secret(secret)
    return secret


def next_secret(secret: int) -> int:
    secret = ((secret * 2**6) ^ secret) % 16777216
    secret = ((secret // 32) ^ secret) % 16777216
    secret = ((secret * 2**11) ^ secret) % 16777216
    return secret


def part_2(secrets: list[int]):
    prices = [[int(str(s)[-1]) for s in secrets]]
    for i in range(2000):
        secrets = [next_secret(s) for s in secrets]
        prices.append([int(str(s)[-1]) for s in secrets])

    change_rates = [
        [y - x for x, y in zip(p1, p2)] for p1, p2 in zip(prices, prices[1:])
    ]
    sequences_prices = defaultdict(dict)
    for c1, c2, c3, c4, price in zip(
        change_rates, change_rates[1:], change_rates[2:], change_rates[3:], prices[4:]
    ):
        for i, (a, s, d, f, p) in enumerate(zip(c1, c2, c3, c4, price)):
            if i not in sequences_prices[(a, s, d, f)]:
                sequences_prices[(a, s, d, f)][i] = p

    return max(sum(v.values()) for v in sequences_prices.values())


if __name__ == "__main__":
    secrets = []
    with open("data/day22") as f:
        while line := f.readline().strip():
            secrets.append(int(line))

    print("Solution 1:", sum(generate_new_secret(s) for s in secrets))
    print("Solution 2:", part_2(secrets))
