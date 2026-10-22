namespace examples.shapes

import std.io

const Limit = 10

interface Shape {
    func Area() float
    Named
}

struct Point {
    X float
    Y float

    func Area() float {
        return 0.0
    }

    static func Origin() Point {
        return Point{X: 0.0, Y: 0.0}
    }
}

enum Color {
    Red
    Rgb(r, g, b int)

    func Name() string {
        return "color"
    }
}

func main() {
    println(Limit)
}
