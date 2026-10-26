package greeter

type Greeter interface {
	Greet(name string) string
}

func Welcome(g Greeter, name string) string {
	return "Hello, " + g.Greet(name) + "!"
}
