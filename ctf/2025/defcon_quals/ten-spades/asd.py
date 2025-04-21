from pwn import *


def prepare_cards_string(card_bytes: bytes) -> str:
    assert len(card_bytes) >= 52, "param_2 must be at least 52 bytes"

    output = []
    DAT = "\x73\x68\x63\x64\x41\x32\x33\x34\x35\x36\x37\x38\x39\x58\x4a\x51\x4b"
    DAT_00106120 = DAT  # Placeholder: lookup table
    DAT_00106124 = DAT[4:]

    for i in range(52):
        b = card_bytes[i]
        if b < 0x34:
            first_char = DAT_00106120[b // 13]  # b / 0xD
            second_char = DAT_00106124[(b + ((b // 13) * -13))]  # b + (b//13)*-13
            chunk = f"{first_char}{second_char}"
        else:
            chunk = ""  # matches lVar2 = 0

        output.append(chunk + " ")  # Always append a space

    return "".join(output)


def get_card_bytes():
    return bytearray(list(range(0x34)))


def shuffle_cards(seed):
    # Initialize deck: 0x00-0x1F (0-31), then printable symbols (32-51)
    deck = list(range(0x34))  # 0x00 to 0x1F
    # Fisher-Yates shuffle

    for i in range(0x33, 0, -1):
        r = update_LCG_seed(seed)
        j = (r & 0xFFFFFFFF) % (i + 1)
        deck[i], deck[j] = deck[j], deck[i]

    return deck


def get_seed(param_2):
    """
    Initialize a LCG-style seed array.
    Returns a list [multiplier, offset, modulus, state]
    """
    param_1 = [0] * 4
    param_1[0] = param_2 ^ 0x77777777  # Multiplier
    param_1[1] = 0x7E5  # Offset
    param_1[2] = 0x7FFFFFFF  # Modulus (2^31 - 1)
    param_1[3] = random.getrandbits(32) ^ 0x77777777  # Initial random state

    return param_1


def update_LCG_seed(seed):
    multiplier = seed[0] & 0xFFFFFFFF
    offset = seed[1] & 0xFFFFFFFF
    modulus = seed[2] & 0xFFFFFFFF
    old_value = seed[3] & 0xFFFFFFFF

    result = (old_value * multiplier + offset) % modulus
    result &= 0xFFFFFFFF
    seed[3] = result
    return result


def parse_cards_string(card_string: str) -> list[int]:
    """
    Reverses the prepare_cards_string function: parses a card string back into the list of card byte values.
    """
    DAT = "\x73\x68\x63\x64\x41\x32\x33\x34\x35\x36\x37\x38\x39\x58\x4a\x51\x4b"
    DAT_00106120 = DAT  # suits
    DAT_00106124 = DAT[4:]  # ranks

    card_bytes = []

    chunks = card_string.strip().split()
    assert len(chunks) == 52, f"Expected 52 cards, got {len(chunks)}."

    for chunk in chunks:
        if not chunk:  # Empty chunk means value >= 0x34 (skipped in original)
            card_bytes.append(0xFF)  # Placeholder for "invalid" or "skipped" value
            continue

        first_char = chunk[0]
        second_char = chunk[1]

        try:
            suit_index = DAT_00106120.index(first_char)
            rank_index = DAT_00106124.index(second_char)

            b = suit_index * 13 + rank_index
            card_bytes.append(b)

        except ValueError:
            raise ValueError(
                f"Invalid character pair: '{chunk}' not found in lookup tables."
            )

    return card_bytes


# if __name__ == "__main__":
#     try:
#         server = remote(host='tenspades-vyl6gsuoz7nky.shellweplayaga.me', port=1337)
#
#         server.recvuntil(b'Ticket please: ')
#         server.sendline(b'ticket{LuckyMocha679n25:iA79U3B4GvPDCMpJCuqTNtbu4FxCN0u4PDIQBZMAw7dwsFy1}')
#
#         string_to_send = prepare_cards_string(bytes(list(range(0x34))))
#         seed_states = []
#         for i in tqdm(range(2**10)):
#             text = server.recvuntil(b'show me your cards ')
#             seed_state = int(text.decode().lstrip().splitlines()[2].strip().replace('seed: ', ''), 16)
#             seed_states.append(seed_state)
#             server.sendline(string_to_send)
#
#
#         find_seed_multiplier(seed_states)
#     finally:
#         print(seed_states)


def test(seed_multiplier):
    seed_offset = 0x7E5
    seed_modulus = 0x7FFFFFFF
    seed_state = 0x13A81D49
    seed = [seed_multiplier, seed_offset, seed_modulus, seed_state]

    print(prepare_cards_string(shuffle_cards(seed)))
    return (
        prepare_cards_string(shuffle_cards(seed))
        == "sA s2 s3 s4 s5 s6 s7 s8 s9 sX sJ sQ sK hA h2 h3 h4 h5 h6 h7 h8 h9 hX hJ hQ hK cA c2 c3 c4 c5 c6 c7 c8 c9 cX cJ cQ cK dA d2 d3 d4 d5 d6 d7 d8 d9 dX dJ dQ dK"
    )


test(0x003F360C)

print(
    parse_cards_string(
        "c3 hJ c8 dJ hX c6 d4 hQ d6 sA s4 d2 dK h8 cJ cK cA s3 sJ d7 h3 s2 sK dQ c5 h9 sQ d3 hK dA c4 s7 dX s6 h7 d5 sX c2 h5 d8 c9 h4 cX s9 h6 hA d9 s8 c7 cQ h2 s5 "
    )
)


# seed_multiplier = 1337 & 0xffffffff
# seed_offset = 0x7E5
# seed_modulus = 0x7FFFFFFF
# seed_state = 0x6bab70f0
# seed = [seed_multiplier, seed_offset, seed_modulus, seed_state]
#
# # card_bytes = get_card_bytes()
# # print(prepare_cards_string(bytes(list(range(0x34)))))
# # print(prepare_cards_string(card_bytes))
# #
# print(prepare_cards_string(shuffle_cards(seed)))

# Expected:

# sA s2 s3 s4 s5 s6 s7 s8 s9 sX sJ sQ sK hA h2 h3 h4 h5 h6 h7 h8 h9 hX hJ hQ hK cA c2 c3 c4 c5 c6 c7 c8 c9 cX cJ cQ cK dA d2 d3 d4 d5 d6 d7 d8 d9 dX dJ dQ dK


# seed: 13a81d49
# c8 dJ s4 dK dA cQ s6 dX d4 s8 h4 d9 sA d2 h5 d6 d8 hJ s7 d3 sK h9 c7 sJ hA hX s3 c9 h2 hK cA c2 d7 c3 h6 s5 cJ hQ s2 cX cK dQ c4 h7 sQ sX c6 c5 d5 h3 s9 h8
# seed: 6bab70f0
# cA h4 sA d4 c8 hA dK c5 s6 dQ hJ dA h7 d5 s2 dJ c2 h9 d6 d9 h2 h8 sK d2 h5 hQ d8 cK s4 sQ h6 c7 cX dX cQ d3 c4 c9 sX s8 sJ c6 s5 d7 hX s9 cJ s3 h3 c3 s7 hK
