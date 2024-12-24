class ThreeBitComputer:
    def __init__(self):
        with open("data/day17") as f:
            self.register_a = int(f.readline().strip().split(": ")[1])
            self.register_b = int(f.readline().strip().split(": ")[1])
            self.register_c = int(f.readline().strip().split(": ")[1])
            f.readline()

            self.program = list(map(int, f.readline().strip()[9:].split(",")))
        self.instruction_pointer = 0
        self.outputs = []

    def run(self) -> list[int]:
        while self.instruction_pointer < len(self.program):
            opcode, operand = self.program[
                self.instruction_pointer : self.instruction_pointer + 2
            ]

            match opcode:
                case 0:
                    self.register_a //= 2 ** self.combo_operand(operand)
                case 1:
                    self.register_b ^= operand
                case 2:
                    self.register_b = self.combo_operand(operand) % 8
                case 3:
                    if self.register_a == 0:
                        self.instruction_pointer += 2
                    else:
                        self.instruction_pointer = operand
                case 4:
                    self.register_b ^= self.register_c
                case 5:
                    self.outputs.append(self.combo_operand(operand) % 8)
                case 6:
                    self.register_b = self.register_a // (
                        2 ** self.combo_operand(operand)
                    )
                case 7:
                    self.register_c = self.register_a // (
                        2 ** self.combo_operand(operand)
                    )

            if opcode != 3:
                self.instruction_pointer += 2
        return self.outputs

    def combo_operand(self, operand: int) -> int:
        if operand <= 3:
            return operand
        if operand == 4:
            return self.register_a
        if operand == 5:
            return self.register_b
        if operand == 6:
            return self.register_c
        raise ValueError

    def reset(self):
        self.outputs = []
        self.register_a = self.register_b = self.register_c = 0
        self.instruction_pointer = 0

    def _register_value(self, combination: list[int], offset: int = 1) -> int:
        return sum(s * 2 ** (3 * (j + offset)) for j, s in enumerate(combination))

    def find_minimum_register_a(self):
        solutions = []
        combinations = [[]]
        while len(combinations) > 0:
            combination = combinations.pop()
            for i in range(8):
                tbc.reset()
                tbc.register_a = i + self._register_value(combination, offset=1)
                sol = tbc.run()
                if sol == tbc.program:
                    solutions.append([i] + combination)
                    continue
                if sol == tbc.program[-len(combination) - 1 :]:
                    combinations.append([i] + combination)

        return min(self._register_value(sol, offset=0) for sol in solutions)


if __name__ == "__main__":
    tbc = ThreeBitComputer()

    print("Solution 1:", ",".join(map(str, tbc.run())))
    print("Solution 2:", tbc.find_minimum_register_a())
