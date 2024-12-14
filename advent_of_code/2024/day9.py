import dataclasses


def solution_1(disk_map: list) -> int:
    block_count = 0
    blocks = []
    for i, size in enumerate(disk_map):
        if i % 2 == 0:
            blocks.extend([block_count] * size)
            block_count += 1
        else:
            blocks.extend([-1] * size)

    i, j = 0, len(blocks) - 1
    while i < j:
        if blocks[i] != -1:
            i += 1
        else:
            blocks[j], blocks[i] = blocks[i], blocks[j]
            j -= 1

    return sum(i * b for i, b in enumerate(blocks) if b != -1)


@dataclasses.dataclass
class Block:
    start_index: int
    end_index: int
    id: int = -1

    @property
    def size(self) -> int:
        return self.end_index - self.start_index + 1

    @property
    def score(self) -> int:
        return sum(i * self.id for i in range(self.start_index, self.end_index + 1))


class FreeBlock(Block):
    id: int = -1
    next: "FreeBlock" = None


def solution_2(disk_map: list) -> int:
    blocks, free_block_head = parse_solution_2(disk_map)

    for block in blocks[::-1]:
        if block.start_index < free_block_head.start_index:
            continue

        block_size = block.size
        free_block = get_first_available_free_block(free_block_head, block)
        if free_block:
            new_free_block_to_add = FreeBlock(
                start_index=block.start_index,
                end_index=block.end_index,
            )
            block.start_index = free_block.start_index
            block.end_index = free_block.start_index + block_size - 1

            if block_size != free_block.size:
                free_block.start_index = block.end_index + 1
            else:
                free_block.start_index = free_block.next.start_index
                free_block.end_index = free_block.next.end_index
                free_block.next = free_block.next.next
            insert_new_free_block(free_block_head, new_free_block_to_add)

    return sum(block.score for block in blocks)


def parse_solution_2(disk_map: list) -> tuple[list[Block], FreeBlock]:
    blocks, free_block_head, free_block_pointer = [], None, None
    current_index = 0
    for i, size in enumerate(disk_map):
        if i % 2 == 0:
            blocks.append(
                Block(
                    id=len(blocks),
                    start_index=current_index,
                    end_index=current_index + size - 1,
                )
            )
        else:
            if size == 0:
                continue
            free_block = FreeBlock(
                start_index=current_index,
                end_index=current_index + size - 1,
            )
            if free_block_head is None:
                free_block_head = free_block_pointer = free_block
            else:
                free_block_pointer.next = free_block
                free_block_pointer = free_block
        current_index += size
    return blocks, free_block_head


def get_first_available_free_block(
    free_block_head: FreeBlock,
    block: Block,
) -> FreeBlock | None:
    while (
        free_block_head is not None and free_block_head.start_index < block.start_index
    ):
        if free_block_head.size >= block.size:
            return free_block_head
        free_block_head = free_block_head.next
    return None


def insert_new_free_block(free_block_head: FreeBlock, block: FreeBlock):
    while (
        free_block_head.next is not None
        and free_block_head.next.start_index < block.start_index
    ):
        free_block_head = free_block_head.next

    if free_block_head.next is None:
        free_block_head.next = block
        return


if __name__ == "__main__":
    with open("data/day9") as f:
        disk_map = list(map(int, f.readline().strip()))

    print("Solution 1:", solution_1(disk_map))
    print("Solution 2:", solution_2(disk_map))
