namespace examples.folding

import <fold text='...'>std.io
import std.strings</fold>

<fold text='/*...*/'>/* A comment
   over two lines */</fold>
struct Point <fold text='{...}'>{
    X int
    Y int
}</fold>

func main() <fold text='{...}'>{
    var p = Point{X: 1, Y: 2}
    if p.X > 0 <fold text='{...}'>{
        println(p.X)
    }</fold>
}</fold>

func short() { }
