namespace examples.shapes

import std.io

const Limit = 10

interface Shape {
    func Area() float
}

@json("point")
struct Point {
    X float
}

enum Color { Red, Green }

func main() {
    println(Limit)
}
