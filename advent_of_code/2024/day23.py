from collections import defaultdict


def find_parties(links: list):
    graph = defaultdict(set)
    for l1, l2 in links:
        graph[l1].add(l2)
        graph[l2].add(l1)

    parties = set()
    frontier = [{k} for k in graph]
    while frontier:
        party = frontier.pop()
        if (party_tuple := tuple(sorted(party))) in parties:
            continue
        parties.add(party_tuple)
        connected_computers = set.intersection(*[graph[p] for p in party])
        for c in connected_computers:
            frontier.append(party | {c})
    return parties


if __name__ == "__main__":
    links = []
    with open("data/day23") as f:
        while line := f.readline().strip():
            links.append(sorted(line.split("-")))
    parties = find_parties(links)
    solution_1 = len(
        {p for p in parties if any(x.startswith("t") for x in p) and len(p) == 3}
    )
    solution_2 = ",".join(sorted(max(parties, key=lambda p: len(p))))
    print("Solution 1:", solution_1)
    print("Solution 2:", solution_2)
