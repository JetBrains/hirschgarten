class A2 {
    fun usesB() {
        val b: B? = null
        val c1 = b.getC()
        b?.getC()?.fooBar()
        c2: C? = null
    }
}
