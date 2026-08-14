import requests

from proto.example_pb2 import Greeter

def main():
    greeter = Greeter(message="Hello world", count=1)
    print(f"{greeter.message} (x{greeter.count})")
    print("requests import ok:", requests.codes.ok == 200)

