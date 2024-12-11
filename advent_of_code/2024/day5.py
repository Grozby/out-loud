from collections import defaultdict


class Page:
    def __init__(self, page: int):
        self.page = page

    def __lt__(self, other):
        return other.page in page_ordering_rules[self.page]


def select_ordered_pages(page_updates: list[list[Page]]) -> list[list[Page]]:
    selected_pages = []
    for page in page_updates:
        if page == sorted(page):
            selected_pages.append(page)
    return selected_pages


def select_unordered_pages(page_updates: list[list[Page]]) -> list[list[Page]]:
    selected_pages = []
    for page in page_updates:
        if page != sorted(page):
            selected_pages.append(page)
    return selected_pages


if __name__ == "__main__":
    page_ordering_rules = defaultdict(set)
    page_updates = []
    with open("data/day5") as f:
        while (line := f.readline().strip()) != "":
            page1, page2 = line.split("|")
            page_ordering_rules[int(page1)].add(int(page2))

        while (line := f.readline().strip()) != "":
            page_updates.append([Page(int(x)) for x in line.split(",")])

    solution_1 = sum(
        page[len(page) // 2].page for page in select_ordered_pages(page_updates)
    )
    print(f"Solution 1: {solution_1}")

    solution_2 = sum(
        sorted(page)[len(page) // 2].page
        for page in select_unordered_pages(page_updates)
    )
    print(f"Solution 2: {solution_2}")
