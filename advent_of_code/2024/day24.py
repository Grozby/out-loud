class Solution:
    def __init__(self):
        self.values = {}
        self.equations = []
        with open("data/day24") as f:
            while (line := f.readline().strip()) != "":
                var, val = line.split(": ")
                self.values[var] = int(val)
            while line := f.readline().strip():
                self.equations.append(self.parse_equation(line))

    def parse_equation(self, eq: str):
        eq, result = eq.split(" -> ")
        v1, op, v2 = eq.split(" ")
        return v1, v2, result, op

    def operation(self, op: str):
        match op:
            case "AND":
                return lambda x, y: x & y
            case "OR":
                return lambda x, y: x | y
            case "XOR":
                return lambda x, y: x ^ y
            case _:
                raise ValueError

    def part1(self, equations: list = None):
        values = self.values.copy()
        equations = self.equations.copy() if equations is None else equations
        i = 0
        while len(equations) > 0:
            i %= len(equations)
            v1, v2, result, op = equations[i]
            if v1 in values and v2 in values:
                values.update({result: self.operation(op)(values[v1], values[v2])})
                equations.pop(i)
            else:
                i += 1
        z = [str(v) for k, v in sorted(values.items())[::-1] if k.startswith("z")]
        return int("".join(z), 2)

    def part2(self):
        def output_z_not_xor(op, result):
            return result.startswith("z") and op != "XOR" and result != "z45"

        def bad_xor_eq(op, result):
            return op == "XOR" and (
                any(
                    s_op == "OR" and (s_v1 == result or s_v2 == result)
                    for s_v1, s_v2, s_result, s_op in self.equations
                )
                or (
                    not result.startswith(("x", "y", "z"))
                    and not v1.startswith(("x", "y", "z"))
                    and not v2.startswith(("x", "y", "z"))
                )
            )

        def bad_and_eq(op, v1, v2, result):
            return (
                op == "AND"
                and "x00" not in (v1, v2)
                and any(
                    s_op != "OR" and (s_v1 == result or s_v2 == result)
                    for s_v1, s_v2, s_result, s_op in self.equations
                )
            )

        wrong = set()
        for v1, v2, result, op in self.equations:
            if output_z_not_xor(op, result):
                wrong.add(result)
            if bad_xor_eq(op, result):
                wrong.add(result)
            if bad_and_eq(op, v1, v2, result):
                wrong.add(result)

        return ",".join(sorted(wrong))


if __name__ == "__main__":
    solution = Solution()
    solution_1 = solution.part1()
    solution_2 = solution.part2()
    print("Solution 1:", solution_1)
    print("Solution 2:", solution_2)
