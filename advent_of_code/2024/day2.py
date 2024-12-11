def is_report_safe(report: list) -> bool:
    change_rate = [x - y for x, y in zip(report, report[1:])]
    return all(1 <= abs(x) <= 3 for x in change_rate) and (
        all(x > 0 for x in change_rate) or all(x < 0 for x in change_rate)
    )


def is_report_safe_if_drop_one(report: list) -> bool:
    for i in range(len(report)):
        if is_report_safe(report[:i] + report[i + 1:]):
            return True
    return False


if __name__ == "__main__":
    reports = []
    with open("data/day2") as f:
        while line := f.readline():
            reports.append([int(x) for x in line.strip().split(" ")])

    solution_1 = sum(is_report_safe(report) for report in reports)
    print(f"Part 1: {solution_1}")

    solution_2 = sum(is_report_safe_if_drop_one(report) for report in reports)
    print(f"Part 2: {solution_2}")
